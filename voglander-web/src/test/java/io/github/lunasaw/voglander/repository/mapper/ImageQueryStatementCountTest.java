package io.github.lunasaw.voglander.repository.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.manager.manager.ImageCollectionConfigManager;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImageQueryStatementCountTest extends BaseTest {

    @Autowired private SqlSessionFactory sqlSessionFactory;
    @Autowired private ImageAssetMapper assetMapper;
    @Autowired private ImageAssetSourceMapper sourceMapper;
    @Autowired private BizTaskMapper taskMapper;
    @Autowired private ImageCollectionConfigMapper collectionConfigMapper;
    @Autowired private ImageAssetManager assetManager;
    @Autowired private ImageCollectionConfigManager collectionManager;

    private final StatementCounter counter = new StatementCounter();

    @BeforeAll
    void installStatementCounter() {
        sqlSessionFactory.getConfiguration().addInterceptor(counter);
    }

    @Test
    void assetPageQueryCount_shouldStayConstantForOneAndTwentyFourRows() {
        String suffix = Long.toHexString(System.nanoTime());
        seedAssets("owner-one-" + suffix, "asset-one-" + suffix, 1);
        seedAssets("owner-many-" + suffix, "asset-many-" + suffix, 24);

        int one = countAssetPage("owner-one-" + suffix);
        int many = countAssetPage("owner-many-" + suffix);
        int zero = countAssetPage("owner-zero-" + suffix);

        assertEquals(one, many);
        assertTrue(many > 0 && many <= 4, "asset page must use a bounded count/page query pair");
        assertTrue(zero <= many, "empty pages must not add queries");
    }

    @Test
    void collectionPageQueryCount_shouldStayConstantForOneAndTwentyRows() {
        String suffix = Long.toHexString(System.nanoTime());
        seedCollections("owner-one-" + suffix, "collection-one-" + suffix, 1);
        seedCollections("owner-many-" + suffix, "collection-many-" + suffix, 20);

        int one = countCollectionPage("owner-one-" + suffix);
        int many = countCollectionPage("owner-many-" + suffix);
        int zero = countCollectionPage("owner-zero-" + suffix);

        assertEquals(one, many);
        assertTrue(many > 0 && many <= 4, "collection page must use a bounded count/page query pair");
        assertTrue(zero <= many, "empty pages must not add queries");
    }

    private int countAssetPage(String ownerId) {
        ImageAssetQueryDTO query = new ImageAssetQueryDTO();
        query.setOwnerType("USER");
        query.setOwnerId(ownerId);
        counter.begin("ImageAssetMapper.selectEnrichedPageByCondition");
        assetManager.getEnrichedPage(query, 1, 24);
        return counter.end();
    }

    private int countCollectionPage(String ownerId) {
        BizTaskQueryDTO query = new BizTaskQueryDTO();
        query.setOwnerType("USER");
        query.setOwnerId(ownerId);
        counter.begin("ImageCollectionTaskReadMapper.selectPageByCondition");
        collectionManager.getEnrichedPage(query, null, null, BizTaskAccessScopeDTO.global(), 1, 20);
        return counter.end();
    }

    private void seedAssets(String ownerId, String prefix, int count) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        for (int index = 0; index < count; index++) {
            String assetId = "img-" + prefix + "-" + index;
            ImageAssetDO asset = new ImageAssetDO();
            asset.setCreateTime(now.plusSeconds(index));
            asset.setUpdateTime(asset.getCreateTime());
            asset.setAssetId(assetId);
            asset.setAssetName(assetId + ".jpg");
            asset.setStatus("AVAILABLE");
            asset.setStorageProvider("LOCAL");
            asset.setStorageKey("images/" + assetId + ".jpg");
            asset.setContentType("image/jpeg");
            asset.setImageFormat("JPEG");
            asset.setFileSize(1L);
            asset.setWidth(1);
            asset.setHeight(1);
            asset.setChecksumAlgorithm("SHA256");
            asset.setChecksum("checksum-" + assetId);
            asset.setCapturedAt(asset.getCreateTime());
            asset.setIngestedAt(asset.getCreateTime());
            asset.setOwnerType("USER");
            asset.setOwnerId(ownerId);
            asset.setRetentionPolicy("PERMANENT");
            asset.setVersion(0);
            assetMapper.insert(asset);
            ImageAssetSourceDO source = new ImageAssetSourceDO();
            source.setCreateTime(asset.getCreateTime());
            source.setAssetId(assetId);
            source.setSourceType("USER_UPLOAD");
            source.setSourceSystem("Voglander");
            source.setSourceEntityType("USER");
            source.setSourceEntityId(ownerId);
            sourceMapper.insert(source);
        }
    }

    private void seedCollections(String ownerId, String prefix, int count) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        for (int index = 0; index < count; index++) {
            String taskId = "btask-" + prefix + "-" + index;
            BizTaskDO task = new BizTaskDO();
            task.setCreateTime(now.plusSeconds(index));
            task.setUpdateTime(task.getCreateTime());
            task.setTaskId(taskId);
            task.setTaskType("IMAGE_COLLECTION");
            task.setTaskName(taskId);
            task.setTaskMode("ONCE");
            task.setScheduleVersion(1);
            task.setState("RUNNING");
            task.setPriority(0);
            task.setPlannedCount(1);
            task.setSuccessCount(0);
            task.setFailedCount(0);
            task.setMissedCount(0);
            task.setCancelledCount(0);
            task.setProgressCurrent(0L);
            task.setProgressTotal(0L);
            task.setProgressRevision(0L);
            task.setPayload("{}");
            task.setPayloadVersion(1);
            task.setOwnerType("USER");
            task.setOwnerId(ownerId);
            task.setVersion(0);
            taskMapper.insert(task);
            ImageCollectionConfigDO config = new ImageCollectionConfigDO();
            config.setCreateTime(task.getCreateTime());
            config.setUpdateTime(task.getCreateTime());
            config.setTaskId(taskId);
            config.setDeviceId("device-" + prefix);
            config.setChannelId("channel-" + index);
            config.setRetentionPolicy("PERMANENT");
            config.setVersion(0);
            collectionConfigMapper.insert(config);
        }
    }

    @Intercepts({
        @Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                CacheKey.class, BoundSql.class})
    })
    private static final class StatementCounter implements Interceptor {
        private final AtomicInteger count = new AtomicInteger();
        private final ThreadLocal<String> target = new ThreadLocal<String>();

        void begin(String statementFragment) {
            count.set(0);
            target.set(statementFragment);
        }

        int end() {
            target.remove();
            return count.get();
        }

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            String statementFragment = target.get();
            if (statementFragment != null) {
                MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
                if (statement.getId().contains(statementFragment)) {
                    count.incrementAndGet();
                }
            }
            return invocation.proceed();
        }

        @Override
        public Object plugin(Object target) {
            return Plugin.wrap(target, this);
        }

        @Override
        public void setProperties(Properties properties) {
            // No configuration required.
        }
    }
}
