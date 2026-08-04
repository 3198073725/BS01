package com.vidsprout.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        log.debug("WebSocket connected: {}", event.getMessage().getHeaders().get("simpSessionId"));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.debug("WebSocket disconnected: {}", event.getSessionId());
    }

    public void broadcastConfigUpdate() {
        messagingTemplate.convertAndSend("/topic/system-events",
                new SystemEvent("config.updated", System.currentTimeMillis()));
    }

    record SystemEvent(String type, long timestamp) {}
}
