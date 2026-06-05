package io.github.lunasaw.voglander.common.event;

/**
 * ZLM 节点退出事件（由 VoglanderZlmHookServiceImpl.onServerExited 发布）。
 *
 * @author luna
 */
public class NodeExitedEvent {

    private final String serverId;

    public NodeExitedEvent(String serverId) {
        this.serverId = serverId;
    }

    public String getServerId() {
        return serverId;
    }
}
