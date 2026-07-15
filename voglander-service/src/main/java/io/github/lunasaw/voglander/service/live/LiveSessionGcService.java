package io.github.lunasaw.voglander.service.live;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.github.lunasaw.voglander.common.anno.TechnicalScheduler;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import io.github.lunasaw.zlm.node.service.NodeService;
import lombok.extern.slf4j.Slf4j;

/**
 * 直播会话 GC 调度器。
 * <p>
 * 1. INVITING 超时 → FAILED<br>
 * 2. pending_close 到期且 refCount=0 → 委托 {@link MediaPlayService#closeStream(String)} 真实关流<br>
 * 3. reconcileActiveSessions：以 ZLM 真实流状态为准，对账 DB ACTIVE 会话，清理回调丢失的幽灵会话<br>
 * </p>
 *
 * @author luna
 */
@Slf4j
@Service
@TechnicalScheduler(category = TechnicalScheduler.Category.MAINTENANCE)
public class LiveSessionGcService {

    private static final int    INVITING_TIMEOUT_MIN  = 2;
    private static final String PENDING_CLOSE_PREFIX  = "live:pending_close:";

    /** ZLM 收流 app（直播流统一落在 rtp 应用下） */
    private static final String ZLM_APP               = "rtp";
    /** 宽限期：会话建立后 N ��内跳过对账，避免与首播窗口竞态 */
    private static final int    RECONCILE_GRACE_SEC   = 30;
    /** 对账分布式锁，多节点只让一个实例执行整轮对账，避免重复 closeStream/SSE */
    private static final String RECONCILE_LOCK_KEY    = "live:gc:reconcile:lock";
    /** 对账锁持有时间（秒），需大于整轮对账耗时 */
    private static final int    RECONCILE_LOCK_SEC    = 30;

    @Autowired private MediaSessionManager mediaSessionManager;
    @Autowired private LiveStreamRegistry  liveStreamRegistry;
    @Autowired private StringRedisTemplate stringRedisTemplate;
    @Autowired private MediaPlayService    mediaPlayService;
    @Autowired private NodeService         nodeService;
    @Autowired private RedisLockUtil       redisLockUtil;

    @Scheduled(fixedDelay = 60_000)
    public void gc() {
        // 1. INVITING 超时 → FAILED
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(INVITING_TIMEOUT_MIN);
        int failed = mediaSessionManager.markTimeoutInvitingAsFailed(deadline);
        if (failed > 0) {
            log.info("[GC] INVITING 超时标 FAILED, count={}", failed);
        }

        // 2. drainPendingClose：扫 Redis pending_close:* 键，refCount=0 则委托编排层真实关流
        drainPendingClose();

        // 3. reconcileActiveSessions：以 ZLM 为准对账 ACTIVE 会话，兜底回调丢失
        reconcileActiveSessions();
    }

    void drainPendingClose() {
        try {
            Set<String> keys = stringRedisTemplate.keys(PENDING_CLOSE_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            for (String key : keys) {
                String streamId = key.substring(PENDING_CLOSE_PREFIX.length());
                if (liveStreamRegistry.getRef(streamId) <= 0) {
                    // 真实关流下沉到编排层（closeRtpServer + BYE + 标 CLOSED + SSE + 清 Registry + 删 key）
                    mediaPlayService.closeStream(streamId);
                    log.info("[GC] 委托 closeStream 回收空闲流, streamId={}", streamId);
                } else {
                    // 仍有观看者，取消 pending
                    stringRedisTemplate.delete(key);
                }
            }
        } catch (Exception e) {
            // 与 DelayedCacheEviction.drainDue 一致：Redis 故障时跳过本轮，避免污染调度器线程
            log.warn("[GC] drainPendingClose Redis 故障，跳过本轮", e);
        }
    }

    /**
     * 对账：枚举 DB 中 ACTIVE 会话，逐一向其所在 ZLM 节点核实流是否存活；
     * ZLM 查不到（流已死但回调丢失）→ 委托 {@link MediaPlayService#closeStream(String)} 收尾
     * （清缓存 + 标 CLOSED + SSE）。以流媒体真实状态为权威，纠正本地缓存漂移。
     * <p>
     * 多节点防重：整轮对账加分布式锁，只让一个实例执行（死流幂等收尾即可，无需多实例并发）。
     * </p>
     */
    void reconcileActiveSessions() {
        // 多节点防重：单次非阻塞尝试拿锁（lock 而非 tryLock(...,0)，后者校验 getTimeOut>0 会抛异常）；
        // 拿不到说明别的实例在对账，本轮跳过
        String lockValue = redisLockUtil.generateLockValue();
        if (!Boolean.TRUE.equals(redisLockUtil.lock(RECONCILE_LOCK_KEY, lockValue, RECONCILE_LOCK_SEC))) {
            return;
        }
        try {
            List<MediaSessionDTO> actives = mediaSessionManager.getActiveSessions();
            for (MediaSessionDTO s : actives) {
                String streamId = s.getStreamId();
                String serverId = s.getNodeServerId();
                if (streamId == null || serverId == null) {
                    continue;
                }
                // 宽限：刚建立的会话（createTime 太近）跳过，避免与首播窗口竞态
                if (isWithinGracePeriod(s)) {
                    continue;
                }
                ZlmNode node = nodeService.getAvailableNode(serverId);
                if (node == null) {
                    // 节点都没了，交给 NodeExitedEvent 路径，这里跳过
                    continue;
                }
                if (!streamAliveOnZlm(node, streamId)) {
                    log.info("[GC] 对账发现死流(ZLM 查无), 收尾, streamId={}, serverId={}", streamId, serverId);
                    mediaPlayService.closeStream(streamId);
                }
            }
        } catch (Exception e) {
            log.warn("[GC] reconcileActiveSessions 异常，跳过本轮", e);
        } finally {
            redisLockUtil.unLock(RECONCILE_LOCK_KEY, lockValue);
        }
    }

    /**
     * 会话创建时间在 {@link #RECONCILE_GRACE_SEC} 内则跳过（用 DB 的 createTime，LocalDateTime）。
     */
    private boolean isWithinGracePeriod(MediaSessionDTO s) {
        if (s.getCreateTime() == null) {
            return false;
        }
        return s.getCreateTime().isAfter(LocalDateTime.now().minusSeconds(RECONCILE_GRACE_SEC));
    }

    /**
     * 以 ZLM 为准判活：用 {@code isMediaOnline}（专用判活接口，返回 online 布尔），
     * 而非 {@code getMediaInfo}（无 online 字段且无条件 success，无法判活）。
     * 查询异常（网络抖动）保守返回 true，避免误杀正常流，下轮再对。
     */
    private boolean streamAliveOnZlm(ZlmNode node, String streamId) {
        try {
            MediaReq req = new MediaReq();
            req.setApp(ZLM_APP);
            req.setStream(streamId);
            // vhost 默认 __defaultVhost__，schema 留空由 ZLM 跨协议匹配
            MediaOnlineStatus st = ZlmRestService.isMediaOnline(node.getHost(), node.getSecret(), req);
            return st != null && Boolean.TRUE.equals(st.getOnline());
        } catch (Exception e) {
            log.warn("[GC] 探活异常，本轮保守保留, streamId={}: {}", streamId, e.getMessage());
            return true;
        }
    }
}
