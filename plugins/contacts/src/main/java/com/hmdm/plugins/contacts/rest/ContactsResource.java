/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.contacts.rest;

import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Device;
import com.hmdm.plugin.service.PluginStatusCache;
import com.hmdm.plugins.contacts.persistence.ContactsDAO;
import com.hmdm.plugins.contacts.persistence.domain.ContactsConfig;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static com.hmdm.plugins.contacts.ContactsPluginConfigurationImpl.PLUGIN_ID;

/**
 * Public endpoint — called by device to get its contacts sync configuration.
 * GET /rest/plugins/contacts/public/{deviceNumber}
 *
 * Private endpoints — called by admin UI.
 * GET  /rest/plugins/contacts/private/config/{configurationId}
 * PUT  /rest/plugins/contacts/private/config
 */
@Singleton
@Path("/plugins/contacts")
public class ContactsResource {

    private static final Logger logger = LoggerFactory.getLogger(ContactsResource.class);

    private final ContactsDAO contactsDAO;
    private final UnsecureDAO unsecureDAO;
    private final PluginStatusCache pluginStatusCache;

    @Inject
    public ContactsResource(ContactsDAO contactsDAO,
                            UnsecureDAO unsecureDAO,
                            PluginStatusCache pluginStatusCache) {
        this.contactsDAO = contactsDAO;
        this.unsecureDAO = unsecureDAO;
        this.pluginStatusCache = pluginStatusCache;
    }

    // -------------------------------------------------------------------------
    // Device → Server: get contacts sync config for this device's configuration
    // -------------------------------------------------------------------------
    @GET
    @Path("/public/{deviceNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigForDevice(@PathParam("deviceNumber") String deviceNumber) {
        try {
            Device device = unsecureDAO.getDeviceByNumber(deviceNumber);
            if (device == null) {
                logger.warn("Contacts config request: device not found: {}", deviceNumber);
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            SecurityContext.init(device.getCustomerId());
            try {
                if (pluginStatusCache.isPluginDisabled(PLUGIN_ID)) {
                    return Response.PLUGIN_DISABLED();
                }
                ContactsConfig config = contactsDAO.getConfigForConfiguration(device.getConfigurationId());
                return Response.OK(config != null ? config : new ContactsConfig());
            } finally {
                SecurityContext.release();
            }
        } catch (Exception e) {
            logger.error("Error retrieving contacts config for device {}", deviceNumber, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // -------------------------------------------------------------------------
    // Admin: read contacts config for a configuration
    // -------------------------------------------------------------------------
    @GET
    @Path("/private/config/{configurationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@PathParam("configurationId") int configurationId) {
        try {
            ContactsConfig config = contactsDAO.getConfigForConfiguration(configurationId);
            return Response.OK(config != null ? config : new ContactsConfig());
        } catch (Exception e) {
            logger.error("Error retrieving contacts config for configurationId {}", configurationId, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // -------------------------------------------------------------------------
    // Admin: save contacts config for a configuration
    // -------------------------------------------------------------------------
    @PUT
    @Path("/private/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveConfig(ContactsConfig config) {
        try {
            contactsDAO.saveConfig(config);
            return Response.OK();
        } catch (Exception e) {
            logger.error("Error saving contacts config for configurationId {}", config.getConfigurationId(), e);
            return Response.INTERNAL_ERROR();
        }
    }
}
