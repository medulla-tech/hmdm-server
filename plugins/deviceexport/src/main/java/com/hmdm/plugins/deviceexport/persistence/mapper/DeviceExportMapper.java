/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.deviceexport.persistence.mapper;

import com.hmdm.plugins.deviceexport.persistence.domain.DeviceExportRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeviceExportMapper {

    List<DeviceExportRecord> getAllDevicesForExport(
            @Param("customerId") int customerId,
            @Param("userId") int userId,
            @Param("groupId") Integer groupId,
            @Param("configurationId") Integer configurationId,
            @Param("filter") String filter);
}
