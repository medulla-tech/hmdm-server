/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.photos.rest;

import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Device;
import com.hmdm.plugin.service.PluginStatusCache;
import com.hmdm.plugins.photos.persistence.domain.PhotosPhotoRecord;
import com.hmdm.plugins.photos.persistence.domain.PhotosPluginSettings;
import com.hmdm.plugins.photos.persistence.mapper.PhotosMapper;
import com.hmdm.plugins.photos.rest.json.PhotosListFilter;
import com.hmdm.rest.json.PaginatedData;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.hmdm.plugins.photos.PhotosPluginConfigurationImpl.PLUGIN_ID;

@Singleton
@Path("/plugins/photos")
public class PhotosResource {

    private static final Logger logger = LoggerFactory.getLogger(PhotosResource.class);

    private final PhotosMapper photosMapper;
    private final UnsecureDAO unsecureDAO;
    private final PluginStatusCache pluginStatusCache;
    private final String pluginsFilesDirectory;

    @Inject
    public PhotosResource(PhotosMapper photosMapper,
                          UnsecureDAO unsecureDAO,
                          PluginStatusCache pluginStatusCache,
                          @Named("plugins.files.directory") String pluginsFilesDirectory) {
        this.photosMapper = photosMapper;
        this.unsecureDAO = unsecureDAO;
        this.pluginStatusCache = pluginStatusCache;
        this.pluginsFilesDirectory = pluginsFilesDirectory;
    }

    @POST
    @Path("/public/upload/{deviceNumber}")
    @Consumes("application/octet-stream")
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadPhoto(@PathParam("deviceNumber") String deviceNumber,
                                @QueryParam("filename") String filename,
                                @QueryParam("ts") Long ts,
                                @QueryParam("lat") Double lat,
                                @QueryParam("lon") Double lon,
                                @QueryParam("address") String address,
                                InputStream fileData) {
        try {
            Device device = unsecureDAO.getDeviceByNumber(deviceNumber);
            if (device == null) {
                logger.error("Photos upload: device not found: {}", deviceNumber);
                return Response.DEVICE_NOT_FOUND_ERROR();
            }

            SecurityContext.init(device.getCustomerId());
            try {
                if (pluginStatusCache.isPluginDisabled(PLUGIN_ID)) {
                    return Response.PLUGIN_DISABLED();
                }

                PhotosPluginSettings settings = photosMapper.findSettings(device.getCustomerId());
                if (settings == null || !settings.isTransferPhotos()) {
                    return Response.OK();
                }

                if (filename == null || filename.trim().isEmpty()) {
                    filename = "photo_" + System.currentTimeMillis();
                }

                long uploadTs = ts != null ? ts : System.currentTimeMillis();
                Date uploadDate = new Date(uploadTs);

                String resolvedDir = resolveTemplate(settings.getDirectoryStructure(), deviceNumber, uploadDate, null);
                String resolvedName = resolveTemplate(settings.getFileNameTemplate(), deviceNumber, uploadDate, sanitizeFilename(filename));

                String relativePath = "photos/" + resolvedDir + resolvedName;
                File targetFile = new File(pluginsFilesDirectory + "/" + relativePath);

                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                }

                // Security: ensure target is within the photos base dir
                File photosBase = new File(pluginsFilesDirectory + "/photos").getCanonicalFile();
                if (!targetFile.getCanonicalFile().getAbsolutePath().startsWith(photosBase.getAbsolutePath())) {
                    logger.error("Photos upload: path traversal attempt for device {}", deviceNumber);
                    return Response.INTERNAL_ERROR();
                }

                try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = fileData.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                }

                PhotosPhotoRecord record = new PhotosPhotoRecord();
                record.setCustomerId(device.getCustomerId());
                record.setDeviceNumber(deviceNumber);
                record.setServerPath(relativePath);
                record.setOriginalName(filename);
                record.setUploadTs(uploadTs);
                record.setLat(lat);
                record.setLon(lon);
                record.setAddress(address);
                photosMapper.insertPhotoRecord(record);

                if (settings.getRemoveOldFiles() > 0) {
                    long cutoff = uploadTs - ((long) settings.getRemoveOldFiles() * 86400_000L);
                    photosMapper.deleteOldPhotoRecords(cutoff, device.getCustomerId());
                }

                return Response.OK();
            } finally {
                SecurityContext.release();
            }
        } catch (Exception e) {
            logger.error("Failed to upload photo for device {}", deviceNumber, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @GET
    @Path("/public/config/{deviceNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceConfig(@PathParam("deviceNumber") String deviceNumber) {
        try {
            Device device = unsecureDAO.getDeviceByNumber(deviceNumber);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            SecurityContext.init(device.getCustomerId());
            try {
                PhotosPluginSettings settings = photosMapper.findSettings(device.getCustomerId());
                if (settings == null) {
                    settings = new PhotosPluginSettings();
                    settings.setTransferPhotos(false);
                }
                return Response.OK(settings);
            } finally {
                SecurityContext.release();
            }
        } catch (Exception e) {
            logger.error("Failed to get photos config for device {}", deviceNumber, e);
            return Response.INTERNAL_ERROR();
        }
    }

    @POST
    @Path("/private/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPhotos(PhotosListFilter filter) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                if (!SecurityContext.get().hasPermission("plugin_photos_access")) {
                    return Response.PERMISSION_DENIED();
                }
                filter.setCustomerId(user.getCustomerId());
                List<PhotosPhotoRecord> items = photosMapper.findPhotos(filter);
                long total = photosMapper.countPhotos(filter);
                return Response.OK(new PaginatedData<>(items, total));
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to list photos", e);
            return Response.INTERNAL_ERROR();
        }
    }

    @GET
    @Path("/private/photo/{id}/file")
    public javax.ws.rs.core.Response servePhoto(@PathParam("id") int id) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                try {
                    PhotosPhotoRecord record = photosMapper.findPhotoById(id, user.getCustomerId());
                    if (record == null) {
                        return javax.ws.rs.core.Response.status(404).build();
                    }

                    File photosBase = new File(pluginsFilesDirectory + "/photos").getCanonicalFile();
                    File targetFile = new File(pluginsFilesDirectory + "/" + record.getServerPath()).getCanonicalFile();

                    if (!targetFile.getAbsolutePath().startsWith(photosBase.getAbsolutePath())) {
                        return javax.ws.rs.core.Response.status(403).build();
                    }

                    if (!targetFile.exists()) {
                        return javax.ws.rs.core.Response.status(404).build();
                    }

                    String contentType = detectContentType(record.getOriginalName());
                    StreamingOutput stream = output -> {
                        try (FileInputStream fis = new FileInputStream(targetFile)) {
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = fis.read(buf)) > 0) {
                                output.write(buf, 0, n);
                            }
                        }
                    };
                    return javax.ws.rs.core.Response.ok(stream, contentType).build();
                } catch (Exception e) {
                    logger.error("Failed to serve photo id={}", id, e);
                    return javax.ws.rs.core.Response.serverError().build();
                }
            }).orElse(javax.ws.rs.core.Response.status(403).build());
        } catch (Exception e) {
            logger.error("Failed to serve photo id={}", id, e);
            return javax.ws.rs.core.Response.serverError().build();
        }
    }

    @DELETE
    @Path("/private/photo/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePhoto(@PathParam("id") int id) {
        try {
            return SecurityContext.get().getCurrentUser().map(user -> {
                try {
                    PhotosPhotoRecord record = photosMapper.findPhotoById(id, user.getCustomerId());
                    if (record == null) {
                        return Response.OBJECT_NOT_FOUND_ERROR();
                    }

                    File photosBase = new File(pluginsFilesDirectory + "/photos").getCanonicalFile();
                    File targetFile = new File(pluginsFilesDirectory + "/" + record.getServerPath()).getCanonicalFile();
                    if (targetFile.getAbsolutePath().startsWith(photosBase.getAbsolutePath()) && targetFile.exists()) {
                        targetFile.delete();
                    }

                    photosMapper.deletePhotoRecord(id, user.getCustomerId());
                    return Response.OK();
                } catch (Exception e) {
                    logger.error("Failed to delete photo id={}", id, e);
                    return Response.INTERNAL_ERROR();
                }
            }).orElse(Response.PERMISSION_DENIED());
        } catch (Exception e) {
            logger.error("Failed to delete photo id={}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    private String resolveTemplate(String template, String deviceNumber, Date date, String originalName) {
        if (template == null) template = "";
        SimpleDateFormat yyyy = new SimpleDateFormat("yyyy");
        SimpleDateFormat MM = new SimpleDateFormat("MM");
        SimpleDateFormat dd = new SimpleDateFormat("dd");
        SimpleDateFormat HH = new SimpleDateFormat("HH");
        SimpleDateFormat mm = new SimpleDateFormat("mm");
        SimpleDateFormat ss = new SimpleDateFormat("ss");
        String result = template
                .replace("DEVICE", sanitizePathSegment(deviceNumber))
                .replace("YEAR", yyyy.format(date))
                .replace("MONTH", MM.format(date))
                .replace("DAY", dd.format(date))
                .replace("HOUR", HH.format(date))
                .replace("MIN", mm.format(date))
                .replace("SEC", ss.format(date));
        if (originalName != null) {
            result = result.replace("NAME", originalName);
        }
        return result;
    }

    private String sanitizePathSegment(String s) {
        if (s == null) return "unknown";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "file";
        String base = new File(name).getName();
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String detectContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".heic")) return "image/heic";
        return "application/octet-stream";
    }
}
