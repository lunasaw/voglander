package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.springframework.http.MediaType;

/**
 * 测试桥：{@link ResponseBodyEmitter#initialize} 与 {@link ResponseBodyEmitter.Handler} 为包级可见，
 * 框架由同包的 ReturnValueHandler 初始化。测试需捕获 emitter 实际下发内容，故在同包提供此桥接，
 * 将一个记录型 Handler 绑定到 emitter，缓冲的 send 在 initialize 后即刻 flush 到本捕获器。
 *
 * @author luna
 */
public class SseEmitterTestCapture {

    private final List<String> sent = new CopyOnWriteArrayList<>();

    /**
     * 将捕获 Handler 绑定到给定 emitter。
     *
     * @param emitter 待捕获的 SseEmitter
     */
    public void attach(ResponseBodyEmitter emitter) {
        try {
            emitter.initialize(new ResponseBodyEmitter.Handler() {
            @Override
            public void send(Object data, MediaType mediaType) {
                sent.add(String.valueOf(data));
            }

            @Override
            public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) {
                for (ResponseBodyEmitter.DataWithMediaType item : items) {
                    sent.add(String.valueOf(item.getData()));
                }
            }

            @Override
            public void complete() {
            }

            @Override
            public void completeWithError(Throwable failure) {
            }

            @Override
            public void onTimeout(Runnable callback) {
            }

            @Override
            public void onError(Consumer<Throwable> callback) {
            }

            @Override
            public void onCompletion(Runnable callback) {
            }
            });
        } catch (java.io.IOException e) {
            throw new IllegalStateException("attach SSE capture handler failed", e);
        }
    }

    /**
     * @return 至今下发的所有数据片段，以 "|" 连接
     */
    public String dump() {
        return String.join("|", sent);
    }
}
