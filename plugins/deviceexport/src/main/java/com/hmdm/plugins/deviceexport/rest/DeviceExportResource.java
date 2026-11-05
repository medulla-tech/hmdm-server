/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.deviceexport.rest;

import com.hmdm.persistence.ConfigurationDAO;
import com.hmdm.persistence.DeviceDAO;
import com.hmdm.persistence.GroupDAO;
import com.hmdm.persistence.domain.Configuration;
import com.hmdm.persistence.domain.Device;
import com.hmdm.persistence.domain.Group;
import com.hmdm.plugins.deviceexport.persistence.domain.DeviceExportRecord;
import com.hmdm.plugins.deviceexport.persistence.mapper.DeviceExportMapper;
import com.hmdm.plugins.deviceexport.rest.json.DeviceExportRequest;
import com.hmdm.plugins.deviceexport.rest.json.DeviceImportResult;
import com.hmdm.rest.json.LookupItem;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Singleton
@Path("/plugins/deviceexport")
@Produces(MediaType.APPLICATION_JSON)
public class DeviceExportResource {

    private static final Logger logger = LoggerFactory.getLogger(DeviceExportResource.class);

    private static final String PERMISSION = "plugin_deviceexport_access";

    // All available exportable columns in display order (number is always first and always exported)
    private static final List<String> ALL_COLUMNS = Arrays.asList(
            "description", "configuration", "imei", "phone", "serial",
            "androidVersion", "launcherVersion", "groups", "lastUpdate",
            "enrollTime", "publicIp", "custom1", "custom2", "custom3"
    );

    private final DeviceExportMapper exportMapper;
    private final DeviceDAO deviceDAO;
    private final ConfigurationDAO configurationDAO;
    private final GroupDAO groupDAO;

    @Inject
    public DeviceExportResource(DeviceExportMapper exportMapper,
                                DeviceDAO deviceDAO,
                                ConfigurationDAO configurationDAO,
                                GroupDAO groupDAO) {
        this.exportMapper = exportMapper;
        this.deviceDAO = deviceDAO;
        this.configurationDAO = configurationDAO;
        this.groupDAO = groupDAO;
    }

    @POST
    @Path("/private/export")
    @Produces("text/csv")
    public javax.ws.rs.core.Response exportDevices(DeviceExportRequest request) {
        logger.debug("Device export request: {}", request);
        try {
            if (!SecurityContext.get().hasPermission(PERMISSION)) {
                logger.warn("Access denied to device export for user: {}",
                        SecurityContext.get().getCurrentUser().map(u -> u.getLogin()).orElse("anonymous"));
                return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.FORBIDDEN).build();
            }

            final int userId = SecurityContext.get().getCurrentUser().get().getId();
            final int customerId = SecurityContext.get().getCurrentUser().get().getCustomerId();

            final List<String> columns = resolveColumns(request.getColumns());
            final List<DeviceExportRecord> records = exportMapper.getAllDevicesForExport(
                    customerId, userId,
                    request.getGroupId(),
                    request.getConfigurationId(),
                    request.getFilter() != null && !request.getFilter().trim().isEmpty() ? request.getFilter().trim() : null
            );

            StreamingOutput stream = output -> {
                try (OutputStreamWriter osw = new OutputStreamWriter(output, StandardCharsets.UTF_8);
                     CSVWriter csv = new CSVWriter(osw)) {

                    // Write BOM for Excel UTF-8 compatibility
                    output.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

                    // Header row
                    String[] header = new String[1 + columns.size()];
                    header[0] = "number";
                    for (int i = 0; i < columns.size(); i++) {
                        header[i + 1] = columns.get(i);
                    }
                    csv.writeNext(header);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    // Data rows
                    for (DeviceExportRecord r : records) {
                        String[] row = new String[1 + columns.size()];
                        row[0] = nvl(r.getNumber());
                        for (int i = 0; i < columns.size(); i++) {
                            row[i + 1] = getColumnValue(r, columns.get(i), sdf);
                        }
                        csv.writeNext(row);
                    }
                    csv.flush();
                } catch (Exception e) {
                    logger.error("Error writing device export CSV", e);
                }
            };

            return javax.ws.rs.core.Response.ok(stream)
                    .header("Content-Disposition", "attachment; filename=\"devices.csv\"")
                    .build();

        } catch (Exception e) {
            logger.error("Unexpected error during device export", e);
            return javax.ws.rs.core.Response.serverError().build();
        }
    }

    @POST
    @Path("/private/import")
    @Consumes("text/plain")
    public Response importDevices(String csvContent) {
        logger.debug("Device import request received");
        try {
            if (!SecurityContext.get().hasPermission(PERMISSION)) {
                return Response.PERMISSION_DENIED();
            }

            if (csvContent == null || csvContent.trim().isEmpty()) {
                return Response.ERROR("Empty CSV content");
            }

            // Strip UTF-8 BOM if present
            if (csvContent.startsWith("\uFEFF")) {
                csvContent = csvContent.substring(1);
            }

            DeviceImportResult result = new DeviceImportResult();

            try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
                String[] headers = reader.readNext();
                if (headers == null) {
                    return Response.ERROR("CSV has no header row");
                }

                // Build header → index map (case-insensitive)
                Map<String, Integer> headerIdx = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    headerIdx.put(headers[i].trim().toLowerCase(), i);
                }

                if (!headerIdx.containsKey("number")) {
                    return Response.ERROR("CSV must have a 'number' column");
                }

                int rowNum = 1;
                String[] row;
                while ((row = reader.readNext()) != null) {
                    rowNum++;
                    if (isBlankRow(row)) continue;

                    String number = col(row, headerIdx, "number");
                    if (number == null || number.isEmpty()) {
                        result.addError("Row " + rowNum + ": 'number' is empty — skipped");
                        result.incrementSkipped();
                        continue;
                    }

                    try {
                        Device existing = deviceDAO.getDeviceByNumber(number);
                        if (existing != null) {
                            applyImportFields(existing, row, headerIdx, result, rowNum);
                            deviceDAO.updateDevice(existing);
                            result.incrementUpdated();
                        } else {
                            Device newDevice = new Device();
                            newDevice.setNumber(number);
                            newDevice.setLastUpdate(0L);
                            applyImportFields(newDevice, row, headerIdx, result, rowNum);
                            if (newDevice.getConfigurationId() == null) {
                                result.addError("Row " + rowNum + ": device '" + number
                                        + "' not found and 'configuration' is missing — skipped");
                                result.incrementSkipped();
                                continue;
                            }
                            deviceDAO.insertDevice(newDevice);
                            result.incrementCreated();
                        }
                    } catch (Exception e) {
                        logger.warn("Error processing import row {}: {}", rowNum, e.getMessage());
                        result.addError("Row " + rowNum + ": " + e.getMessage());
                        result.incrementSkipped();
                    }
                }
            } catch (CsvValidationException e) {
                return Response.ERROR("Invalid CSV format: " + e.getMessage());
            }

            return Response.OK(result);

        } catch (IOException e) {
            logger.error("IO error during device import", e);
            return Response.INTERNAL_ERROR();
        } catch (Exception e) {
            logger.error("Unexpected error during device import", e);
            return Response.INTERNAL_ERROR();
        }
    }

    // ---- helpers ----

    private void applyImportFields(Device device, String[] row, Map<String, Integer> headerIdx,
                                   DeviceImportResult result, int rowNum) {
        String description = col(row, headerIdx, "description");
        if (description != null) device.setDescription(description);

        String imei = col(row, headerIdx, "imei");
        if (imei != null) device.setImei(imei);

        String phone = col(row, headerIdx, "phone");
        if (phone != null) device.setPhone(phone);

        String custom1 = col(row, headerIdx, "custom1");
        if (custom1 != null) device.setCustom1(custom1);

        String custom2 = col(row, headerIdx, "custom2");
        if (custom2 != null) device.setCustom2(custom2);

        String custom3 = col(row, headerIdx, "custom3");
        if (custom3 != null) device.setCustom3(custom3);

        String configName = col(row, headerIdx, "configuration");
        if (configName != null && !configName.isEmpty()) {
            Configuration config = configurationDAO.getConfigurationByName(configName);
            if (config != null) {
                device.setConfigurationId(config.getId());
            } else {
                result.addError("Row " + rowNum + ": configuration '" + configName + "' not found — field ignored");
            }
        }

        String groupsCell = col(row, headerIdx, "groups");
        if (groupsCell != null) {
            List<LookupItem> groupList = new ArrayList<>();
            if (!groupsCell.isEmpty()) {
                for (String gName : groupsCell.split(",")) {
                    gName = gName.trim();
                    if (gName.isEmpty()) continue;
                    try {
                        Group group = groupDAO.getGroupByName(gName);
                        if (group != null) {
                            groupList.add(new LookupItem(group.getId(), group.getName()));
                        } else {
                            result.addError("Row " + rowNum + ": group '" + gName + "' not found — skipped");
                        }
                    } catch (Exception e) {
                        result.addError("Row " + rowNum + ": group '" + gName + "' error: " + e.getMessage());
                    }
                }
            }
            device.setGroups(groupList);
        }
    }

    private List<String> resolveColumns(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>(ALL_COLUMNS);
        }
        // Keep only valid column names, in the requested order
        List<String> result = new ArrayList<>();
        for (String col : requested) {
            if (ALL_COLUMNS.contains(col)) {
                result.add(col);
            }
        }
        return result.isEmpty() ? new ArrayList<>(ALL_COLUMNS) : result;
    }

    private String getColumnValue(DeviceExportRecord r, String column, SimpleDateFormat sdf) {
        switch (column) {
            case "description":    return nvl(r.getDescription());
            case "configuration":  return nvl(r.getConfigName());
            case "imei":           return nvl(r.getImei());
            case "phone":          return nvl(r.getPhone());
            case "serial":         return nvl(r.getSerial());
            case "androidVersion": return nvl(r.getAndroidVersion());
            case "launcherVersion":return nvl(r.getLauncherVersion());
            case "groups":         return nvl(r.getGroupNames());
            case "lastUpdate":     return r.getLastUpdate() != null && r.getLastUpdate() > 0
                                          ? sdf.format(new Date(r.getLastUpdate())) : "";
            case "enrollTime":     return r.getEnrollTime() != null && r.getEnrollTime() > 0
                                          ? sdf.format(new Date(r.getEnrollTime())) : "";
            case "publicIp":       return nvl(r.getPublicIp());
            case "custom1":        return nvl(r.getCustom1());
            case "custom2":        return nvl(r.getCustom2());
            case "custom3":        return nvl(r.getCustom3());
            default:               return "";
        }
    }

    private static String col(String[] row, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key.toLowerCase());
        if (i == null || i >= row.length) return null;
        String v = row[i].trim();
        return v;
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) return false;
        }
        return true;
    }
}
