/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol.rest;

import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Device;
import com.hmdm.plugins.remotecontrol.persistence.domain.RemoteControlSession;
import com.hmdm.plugins.remotecontrol.persistence.domain.RemoteControlSettings;
import com.hmdm.plugins.remotecontrol.persistence.mapper.RemoteControlMapper;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Singleton
@Path("/plugins/remotecontrol")
public class RemoteControlResource {

    private static final Logger logger = LoggerFactory.getLogger(RemoteControlResource.class);

    private final RemoteControlMapper mapper;
    private final UnsecureDAO unsecureDAO;
    private final RemoteControlSignalingService signalingService;

    @Inject
    public RemoteControlResource(RemoteControlMapper mapper,
                                 UnsecureDAO unsecureDAO,
                                 RemoteControlSignalingService signalingService) {
        this.mapper = mapper;
        this.unsecureDAO = unsecureDAO;
        this.signalingService = signalingService;
    }

    @POST
    @Path("/private/sessions/{deviceNumber}/start")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startSession(@PathParam("deviceNumber") String deviceNumber) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                Device device = unsecureDAO.getDeviceByNumber(deviceNumber);
                if (device == null) {
                    return Response.ERROR("Device not found: " + deviceNumber);
                }

                RemoteControlSession existing = mapper.findActiveSessionByDevice(deviceNumber);
                if (existing != null) {
                    // Always stop existing session and start a new one
                    mapper.updateSessionStatus(existing.getId(), RemoteControlSession.STATUS_ENDED, System.currentTimeMillis());
                    signalingService.stopSignaling(existing.getId());
                    logger.info("Stopped existing remote control session {} for device {} to start a new one", existing.getId(), deviceNumber);
                }

                RemoteControlSettings settings = mapper.findSettings(user.getCustomerId());
                if (settings == null) {
                    settings = new RemoteControlSettings();
                    settings.setStunUrl("stun:stun.l.google.com:19302");
                    settings.setTurnUrl("");
                    settings.setTurnUsername("");
                    settings.setTurnPassword("");
                }

                RemoteControlSession session = new RemoteControlSession();
                session.setCustomerId(user.getCustomerId());
                session.setDeviceId(device.getId());
                session.setDeviceNumber(deviceNumber);
                session.setAdminUserId(user.getId());
                session.setStatus(RemoteControlSession.STATUS_PENDING);
                session.setToken(UUID.randomUUID().toString().replace("-", ""));
                session.setStartTs(System.currentTimeMillis());
                mapper.insertSession(session);

                // Use oldNumber for MQTT topic if available (device subscribes to oldNumber)
                String mqttTopic = device.getOldNumber() != null ? device.getOldNumber() : deviceNumber;
                signalingService.startSignaling(session.getId(), deviceNumber, mqttTopic);
                signalingService.sendStartCommand(mqttTopic, session.getId(),
                        settings.getStunUrl(), settings.getTurnUrl(),
                        settings.getTurnUsername(), settings.getTurnPassword());

                return Response.OK(session);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to start remote control session for device {}", deviceNumber, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @POST
    @Path("/private/sessions/{sessionId}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stopSession(@PathParam("sessionId") int sessionId) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                RemoteControlSession session = mapper.findSessionById(sessionId, user.getCustomerId());
                if (session == null) {
                    return Response.ERROR("Session not found");
                }
                if (RemoteControlSession.STATUS_ENDED.equals(session.getStatus())
                        || RemoteControlSession.STATUS_ERROR.equals(session.getStatus())) {
                    return Response.ERROR("Session already ended");
                }
                mapper.updateSessionStatus(sessionId, RemoteControlSession.STATUS_ENDED, System.currentTimeMillis());
                signalingService.stopSignaling(sessionId);
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to stop remote control session {}", sessionId, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @GET
    @Path("/private/sessions/{sessionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSession(@PathParam("sessionId") int sessionId) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                RemoteControlSession session = mapper.findSessionById(sessionId, user.getCustomerId());
                if (session == null) {
                    return Response.ERROR("Session not found");
                }
                return Response.OK(session);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to get remote control session {}", sessionId, e);
            return Response.INTERNAL_ERROR();
        }
    }
}
