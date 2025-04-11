package com.yyh.file.config;

import com.yyh.file.socket.WebSocketSessionHolder;
import com.yyh.file.socket.WebSocketSessionWrapper;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public class CustomWebSocketHandler implements WebSocketHandler {
    private final WebSocketSessionHolder sessionHolder;

    public CustomWebSocketHandler(WebSocketSessionHolder sessionHolder) {
        this.sessionHolder = sessionHolder;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        // 创建一个 Sink 用于收集发送消息
        Sinks.Many<WebSocketMessage> sink = Sinks.many().unicast().onBackpressureBuffer();
        // 封装 session 与 sink
        WebSocketSessionWrapper wrapper = new WebSocketSessionWrapper(session, sink);
        sessionHolder.addSession(sessionId, wrapper);
// 首次发送 sessionId 给客户端
        sink.tryEmitNext(session.textMessage("{\"sessionId\": \"" + sessionId + "\"}"));

        // 只调用一次 send() 方法，将 Sink 的 Flux 作为输出通道
        Mono<Void> sendMono = session.send(sink.asFlux());

        // 接收客户端消息（这里简单打印）
        Mono<Void> receiveMono = session.receive()
                .doOnNext(msg -> System.out.println("收到消息: " + msg.getPayloadAsText()))
                .then();

        return Mono.zip(sendMono, receiveMono).then()
                .doFinally(signal -> {
                    sessionHolder.removeSession(sessionId);
                    System.out.println("WebSocket 会话关闭: " + sessionId);
                });
    }
}