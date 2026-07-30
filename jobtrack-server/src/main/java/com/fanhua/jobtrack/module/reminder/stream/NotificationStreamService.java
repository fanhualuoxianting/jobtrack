package com.fanhua.jobtrack.module.reminder.stream;

import com.fanhua.jobtrack.common.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationStreamService {
    private static final Logger log = LoggerFactory.getLogger(NotificationStreamService.class);
    private static final int MAX_CONNECTIONS_PER_USER = 3;

    private final Map<Long, Map<String, SseEmitter>> connections = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        Map<String, SseEmitter> userConnections = connections.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>());
        if (userConnections.size() >= MAX_CONNECTIONS_PER_USER) {
            throw new ConflictException("SSE_CONNECTION_LIMIT", "单用户实时通知连接数已达上限");
        }
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        userConnections.put(connectionId, emitter);
        Runnable cleanup = () -> remove(userId, connectionId);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("connectionId", connectionId)));
        } catch (IOException e) {
            cleanup.run();
            throw new IllegalStateException("SSE 连接初始化失败", e);
        }
        return emitter;
    }

    public void send(Long userId, Long reminderId, String title, String content) {
        Map<String, SseEmitter> userConnections = connections.get(userId);
        if (userConnections == null) return;
        Map<String, Object> payload = Map.of("id", reminderId, "title", title, "content", content);
        userConnections.forEach((connectionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("notification").id(String.valueOf(reminderId)).data(payload));
            } catch (IOException | IllegalStateException ex) {
                log.warn("SSE 发送失败，移除连接 userId={}, connectionId={}", userId, connectionId);
                remove(userId, connectionId);
            }
        });
    }

    @Scheduled(fixedDelayString = "${jobtrack.notification.heartbeat-ms:15000}")
    public void heartbeat() {
        connections.forEach((userId, userConnections) -> userConnections.forEach((connectionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException ex) {
                remove(userId, connectionId);
            }
        }));
    }

    public int connectionCount(Long userId) {
        Map<String, SseEmitter> userConnections = connections.get(userId);
        return userConnections == null ? 0 : userConnections.size();
    }

    /** 主动关闭指定连接；浏览器断开时仍由 SseEmitter completion callback 触发同一清理逻辑。 */
    public void disconnect(Long userId, SseEmitter emitter) {
        Map<String, SseEmitter> userConnections = connections.get(userId);
        if (userConnections == null) return;
        userConnections.entrySet().removeIf(entry -> entry.getValue() == emitter);
        if (userConnections.isEmpty()) connections.remove(userId, userConnections);
    }

    private void remove(Long userId, String connectionId) {
        Map<String, SseEmitter> userConnections = connections.get(userId);
        if (userConnections == null) return;
        userConnections.remove(connectionId);
        if (userConnections.isEmpty()) connections.remove(userId, userConnections);
    }
}
