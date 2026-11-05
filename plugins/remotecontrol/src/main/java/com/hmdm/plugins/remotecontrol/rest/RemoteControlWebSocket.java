/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol.rest;

import com.hmdm.plugins.remotecontrol.persistence.domain.RemoteControlSession;
import com.hmdm.plugins.remotecontrol.persistence.mapper.RemoteControlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws/remotecontrol/{sessionId}")
public class RemoteControlWebSocket {

    private static final Logger logger = LoggerFactory.getLogger(RemoteControlWebSocket.class);

    private int sessionId;

    @OnOpen
    public void onOpen(Session ws, @PathParam("sessionId") String sessionIdStr) {
        try {
            sessionId = Integer.parseInt(sessionIdStr);
        } catch (NumberFormatException e) {
            closeWithError(ws, "Invalid session ID");
            return;
        }

        String token = ws.getRequestParameterMap().containsKey("token")
                ? ws.getRequestParameterMap().get("token").get(0)
                : null;
        if (token == null || token.isEmpty()) {
            closeWithError(ws, "Missing authentication token");
            return;
        }

        RemoteControlSignalingService service = RemoteControlSignalingService.getInstance();
        if (service == null) {
            closeWithError(ws, "Signaling service not available");
            return;
        }

        service.registerWebSocket(sessionId, ws);
        logger.info("WebSocket opened for remote control session {}", sessionId);
    }

    @OnMessage
    public void onMessage(String message, Session ws) {
        RemoteControlSignalingService service = RemoteControlSignalingService.getInstance();
        if (service != null) {
            service.sendToDevice(sessionId, message);
        }
    }

    @OnClose
    public void onClose(Session ws) {
        RemoteControlSignalingService service = RemoteControlSignalingService.getInstance();
        if (service != null) {
            service.unregisterWebSocket(sessionId);
        }
        logger.info("WebSocket closed for remote control session {}", sessionId);
    }

    @OnError
    public void onError(Session ws, Throwable error) {
        logger.error("WebSocket error for remote control session {}", sessionId, error);
        RemoteControlSignalingService service = RemoteControlSignalingService.getInstance();
        if (service != null) {
            service.unregisterWebSocket(sessionId);
        }
    }

    private void closeWithError(Session ws, String reason) {
        try {
            ws.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (Exception e) {
            logger.error("Error closing WebSocket", e);
        }
    }
}
