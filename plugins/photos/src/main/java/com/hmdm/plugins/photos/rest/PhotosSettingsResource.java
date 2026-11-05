/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.photos.rest;

import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.plugin.service.PluginStatusCache;
import com.hmdm.plugins.photos.persistence.domain.PhotosPluginSettings;
import com.hmdm.plugins.photos.persistence.mapper.PhotosMapper;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.hmdm.plugins.photos.PhotosPluginConfigurationImpl.PLUGIN_ID;

@Singleton
@Path("/plugins/photos/private/settings")
public class PhotosSettingsResource {

    private static final Logger logger = LoggerFactory.getLogger(PhotosSettingsResource.class);

    private final PhotosMapper photosMapper;
    private final PluginStatusCache pluginStatusCache;

    @Inject
    public PhotosSettingsResource(PhotosMapper photosMapper,
                                  PluginStatusCache pluginStatusCache) {
        this.photosMapper = photosMapper;
        this.pluginStatusCache = pluginStatusCache;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSettings() {
        try {
            if (pluginStatusCache.isPluginDisabled(PLUGIN_ID)) {
                return Response.PLUGIN_DISABLED();
            }
            if (!SecurityContext.get().hasPermission("plugin_photos_access")) {
                return Response.PERMISSION_DENIED();
            }
            return SecurityContext.get().getCurrentUser().map(user -> {
                PhotosPluginSettings settings = photosMapper.findSettings(user.getCustomerId());
                if (settings == null) {
                    settings = defaultSettings(user.getCustomerId());
                }
                return Response.OK(settings);
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to get photos plugin settings", e);
            return Response.INTERNAL_ERROR();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveSettings(PhotosPluginSettings settings) {
        try {
            if (pluginStatusCache.isPluginDisabled(PLUGIN_ID)) {
                return Response.PLUGIN_DISABLED();
            }
            if (!SecurityContext.get().hasPermission("plugin_photos_access")) {
                return Response.PERMISSION_DENIED();
            }
            return SecurityContext.get().getCurrentUser().map(user -> {
                settings.setCustomerId(user.getCustomerId());
                photosMapper.saveSettings(settings);
                return Response.OK();
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to save photos plugin settings", e);
            return Response.INTERNAL_ERROR();
        }
    }

    private PhotosPluginSettings defaultSettings(int customerId) {
        PhotosPluginSettings s = new PhotosPluginSettings();
        s.setCustomerId(customerId);
        s.setTransferPhotos(false);
        s.setImagePaths("");
        s.setExcludePaths("");
        s.setIncludeStandardPaths(true);
        s.setNonImageExtensions("");
        s.setDirectoryStructure("DEVICE/YEAR/MONTH/");
        s.setFileNameTemplate("img_YEAR_MONTH_DAY_HOUR_MIN_SEC_NAME");
        s.setRemoveOldFiles(30);
        s.setDeleteImagesOnDevices("");
        s.setImageLocation(false);
        s.setDisableLocationWarning("");
        s.setAddLabel(false);
        s.setLabelColor("#ffffff");
        s.setBackgroundColor("");
        s.setTransparency(80);
        s.setLabelText("DATE TIME ADDRESS");
        return s;
    }
}
