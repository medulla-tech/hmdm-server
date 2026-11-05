/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol.persistence.mapper;

import com.hmdm.plugins.remotecontrol.persistence.domain.RemoteControlSession;
import com.hmdm.plugins.remotecontrol.persistence.domain.RemoteControlSettings;
import org.apache.ibatis.annotations.*;

public interface RemoteControlMapper {

    @Select("SELECT * FROM plugin_remotecontrol_settings WHERE customerId = #{customerId}")
    RemoteControlSettings findSettings(@Param("customerId") int customerId);

    @Insert("INSERT INTO plugin_remotecontrol_settings " +
            "(customerId, stunUrl, turnUrl, turnUsername, turnPassword) " +
            "VALUES (#{customerId}, #{stunUrl}, #{turnUrl}, #{turnUsername}, #{turnPassword}) " +
            "ON DUPLICATE KEY UPDATE " +
            "stunUrl = VALUES(stunUrl), turnUrl = VALUES(turnUrl), " +
            "turnUsername = VALUES(turnUsername), turnPassword = VALUES(turnPassword)")
    void saveSettings(RemoteControlSettings settings);

    @Insert("INSERT INTO plugin_remotecontrol_sessions " +
            "(customerId, deviceId, deviceNumber, adminUserId, status, token, startTs) " +
            "VALUES (#{customerId}, #{deviceId}, #{deviceNumber}, #{adminUserId}, #{status}, #{token}, #{startTs})")
    @SelectKey(statement = "SELECT LAST_INSERT_ID()", keyProperty = "id", before = false, resultType = int.class)
    void insertSession(RemoteControlSession session);

    @Select("SELECT * FROM plugin_remotecontrol_sessions WHERE id = #{id} AND token = #{token}")
    RemoteControlSession findSessionByIdAndToken(@Param("id") int id, @Param("token") String token);

    @Select("SELECT * FROM plugin_remotecontrol_sessions WHERE id = #{id} AND customerId = #{customerId}")
    RemoteControlSession findSessionById(@Param("id") int id, @Param("customerId") int customerId);

    @Select("SELECT * FROM plugin_remotecontrol_sessions " +
            "WHERE deviceNumber = #{deviceNumber} AND status IN ('PENDING', 'ACTIVE') " +
            "ORDER BY startTs DESC LIMIT 1")
    RemoteControlSession findActiveSessionByDevice(@Param("deviceNumber") String deviceNumber);

    @Update("UPDATE plugin_remotecontrol_sessions SET status = #{status}, endTs = #{endTs} WHERE id = #{id}")
    void updateSessionStatus(@Param("id") int id, @Param("status") String status, @Param("endTs") Long endTs);
}
