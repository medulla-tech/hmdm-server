/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol.rest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.notification.PushSenderMqtt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RemoteControlSignalingService {

    private static final Logger logger = LoggerFactory.getLogger(RemoteControlSignalingService.class);

    private static volatile RemoteControlSignalingService INSTANCE;

    private final PushSenderMqtt pushSenderMqtt;

    private final ConcurrentHashMap<Integer, Session> webSocketSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> sessionDeviceMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> sessionMqttTopicMap = new ConcurrentHashMap<>();

    @Inject
    public RemoteControlSignalingService(PushSenderMqtt pushSenderMqtt) {
        this.pushSenderMqtt = pushSenderMqtt;
        INSTANCE = this;
    }

    public static RemoteControlSignalingService getInstance() {
        return INSTANCE;
    }

    public void startSignaling(int sessionId, String deviceNumber, String mqttTopic) {
        sessionDeviceMap.put(sessionId, deviceNumber);
        sessionMqttTopicMap.put(sessionId, mqttTopic);
        String topic = "rc/toServer/" + deviceNumber;
        pushSenderMqtt.subscribe(topic, (t, payload) -> {
            Session ws = webSocketSessions.get(sessionId);
            if (ws != null && ws.isOpen()) {
                try {
                    synchronized (ws) {
                        ws.getBasicRemote().sendText(payload);
                    }
                } catch (Exception e) {
                    logger.error("Failed to forward MQTT message to WebSocket for session {}", sessionId, e);
                }
            }
        });
    }

    public void stopSignaling(int sessionId) {
        String deviceNumber = sessionDeviceMap.remove(sessionId);
        String mqttTopic = sessionMqttTopicMap.remove(sessionId);
        if (deviceNumber != null) {
            // Tell the device to stop the RC session
            String stopMessage = "{\"messageType\": \"rcStop\", \"payload\": {}}";
            pushSenderMqtt.publishRaw(mqttTopic != null ? mqttTopic : deviceNumber, stopMessage);
            pushSenderMqtt.unsubscribe("rc/toServer/" + deviceNumber);
        }
        Session ws = webSocketSessions.remove(sessionId);
        if (ws != null && ws.isOpen()) {
            try {
                ws.close();
            } catch (Exception e) {
                logger.error("Error closing WebSocket for session {}", sessionId, e);
            }
        }
    }

    public void registerWebSocket(int sessionId, Session ws) {
        webSocketSessions.put(sessionId, ws);
    }

    public void unregisterWebSocket(int sessionId) {
        webSocketSessions.remove(sessionId);
    }

    public void sendToDevice(int sessionId, String message) {
        String mqttTopic = sessionMqttTopicMap.get(sessionId);
        if (mqttTopic == null) return;
        String wrappedMessage = "{\"messageType\": \"rcSignal\", \"payload\": " + message + "}";
        pushSenderMqtt.publishRaw(mqttTopic, wrappedMessage);
    }

    public void sendStartCommand(String mqttTopic, int sessionId, String stunUrl, String turnUrl, String turnUsername, String turnPassword) {
        String payload = "{\"sessionId\":" + sessionId
                + ",\"stunUrl\":\"" + escapeJson(stunUrl) + "\""
                + ",\"turnUrl\":\"" + escapeJson(turnUrl) + "\""
                + ",\"turnUsername\":\"" + escapeJson(turnUsername) + "\""
                + ",\"turnPassword\":\"" + escapeJson(turnPassword) + "\""
                + "}";
        String message = "{\"messageType\": \"rcStartSession\", \"payload\": " + payload + "}";
        logger.info("Sending rcStartSession to MQTT topic '{}', sessionId={}", mqttTopic, sessionId);
        pushSenderMqtt.publishRaw(mqttTopic, message);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
