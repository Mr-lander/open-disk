package com.yyh.file.socket;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionHolder {
    // 存储 sessionId 对应的 Sinks.Many，用于推送消息
    private final Map<String, WebSocketSessionWrapper> sessions = new ConcurrentHashMap<>();

    public void addSession(String sessionId, WebSocketSessionWrapper sessionWrapper) {
        sessions.put(sessionId, sessionWrapper);
    }

    public WebSocketSessionWrapper getSessionWrapper(String sessionId) {
        return sessions.get(sessionId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
