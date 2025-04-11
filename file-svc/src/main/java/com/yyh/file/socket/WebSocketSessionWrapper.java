package com.yyh.file.socket;

import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

public class WebSocketSessionWrapper {
    private final WebSocketSession session;
    private final Sinks.Many<WebSocketMessage> sink;

    public WebSocketSessionWrapper(WebSocketSession session, Sinks.Many<WebSocketMessage> sink) {
        this.session = session;
        this.sink = sink;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public Sinks.Many<WebSocketMessage> getSink() {
        return sink;
    }
}
