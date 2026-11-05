/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.deviceexport.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceExportRequest {

    private Integer groupId;
    private Integer configurationId;
    private String filter;
    private List<String> columns;

    public DeviceExportRequest() {}

    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer groupId) { this.groupId = groupId; }

    public Integer getConfigurationId() { return configurationId; }
    public void setConfigurationId(Integer configurationId) { this.configurationId = configurationId; }

    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }
}
