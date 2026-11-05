/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol.rest;

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

@Singleton
@Path("/plugins/remotecontrol")
public class RemoteControlSettingsResource {

    private static final Logger logger = LoggerFactory.getLogger(RemoteControlSettingsResource.class);

    private final RemoteControlMapper mapper;

    @Inject
    public RemoteControlSettingsResource(RemoteControlMapper mapper) {
        this.mapper = mapper;
    }

    @GET
    @Path("/private/settings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSettings() {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                RemoteControlSettings settings = mapper.findSettings(user.getCustomerId());
                if (settings == null) {
                    settings = new RemoteControlSettings();
                    settings.setCustomerId(user.getCustomerId());
                    settings.setStunUrl("stun:stun.l.google.com:19302");
                    settings.setTurnUrl("");
                    settings.setTurnUsername("");
                    settings.setTurnPassword("");
                }
                return Response.OK(settings);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to get remote control settings", e);
            return Response.INTERNAL_ERROR();
        }
    }

    @PUT
    @Path("/private/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveSettings(RemoteControlSettings settings) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                settings.setCustomerId(user.getCustomerId());
                mapper.saveSettings(settings);
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to save remote control settings", e);
            return Response.INTERNAL_ERROR();
        }
    }
}
