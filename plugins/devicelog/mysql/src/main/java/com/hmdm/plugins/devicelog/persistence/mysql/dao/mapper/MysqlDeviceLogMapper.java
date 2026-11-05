/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.devicelog.persistence.mysql.dao.mapper;

import com.hmdm.plugins.devicelog.persistence.mysql.dao.domain.MysqlDeviceLogPluginSettings;
import com.hmdm.plugins.devicelog.persistence.mysql.dao.domain.MysqlDeviceLogRecord;
import com.hmdm.plugins.devicelog.persistence.mysql.dao.domain.MysqlDeviceLogRule;
import com.hmdm.plugins.devicelog.rest.json.DeviceLogFilter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>An ORM mapper for {@link MysqlDeviceLogRecord} domain object.</p>
 *
 * @author isv
 */
public interface MysqlDeviceLogMapper {

    List<MysqlDeviceLogRecord> findAllLogRecordsByCustomerId(DeviceLogFilter filter);

    long countAll(DeviceLogFilter filter);

    int insertDeviceLogRecords(@Param("logs") List<MysqlDeviceLogRecord> mysqlLogs);

    // ------------ devicelog plugin settings ------------------------------------------------------------------------------
    MysqlDeviceLogPluginSettings findPluginSettingsByCustomerId(@Param("customerId") Integer customerId);


    @Insert("INSERT INTO plugin_devicelog_settings (customerId, logsPreservePeriod) " +
            "VALUES (#{customerId}, #{logsPreservePeriod})")
    @SelectKey( statement = "SELECT currval('plugin_devicelog_settings_id_seq')",
            keyColumn = "id", keyProperty = "id", before = false, resultType = int.class )
    void insertPluginSettings(MysqlDeviceLogPluginSettings mysqlDeviceLogPluginSettings);

    @Update("UPDATE plugin_devicelog_settings SET logsPreservePeriod = #{logsPreservePeriod} WHERE id = #{id}")
    void updatePluginSettings(MysqlDeviceLogPluginSettings mysqlDeviceLogPluginSettings);


    @Insert("INSERT INTO plugin_devicelog_settings_rules (" +
            "  settingId, " +
            "  name, " +
            "  active, " +
            "  applicationId, " +
            "  severity, " +
            "  filter, " +
            "  groupId, " +
            "  configurationId " +
            ") " +
            "VALUES (" +
            "  #{settingId}, " +
            "  #{name}, " +
            "  #{active}, " +
            "  #{applicationId}, " +
            "  #{severity}, " +
            "  #{filter}, " +
            "  #{groupId}, " +
            "  #{configurationId} " +
            ")")
    @SelectKey( statement = "SELECT currval('plugin_devicelog_settings_rules_id_seq')",
            keyColumn = "id", keyProperty = "id", before = false, resultType = int.class )
    void insertPluginSettingsRule(MysqlDeviceLogRule rule);

    @Update("UPDATE plugin_devicelog_settings_rules SET " +
            " settingId = #{settingId}, " +
            " name = #{name}, " +
            " active = #{active}, " +
            " applicationId = #{applicationId}, " +
            " severity = #{severity}, " +
            " filter = #{filter}, " +
            " groupId = #{groupId}, " +
            " configurationId = #{configurationId} " +
            "WHERE id = #{id}")
    void updatePluginSettingsRule(MysqlDeviceLogRule rule);

    @Delete("DELETE FROM plugin_devicelog_settings_rules WHERE id = #{id}")
    void deletePluginSettingRule(@Param("id") int id);

    @Select("SELECT settings.id, settings.customerId " +
            "FROM plugin_devicelog_settings_rules rules " +
            "INNER JOIN plugin_devicelog_settings settings ON settings.id = rules.settingId " +
            "WHERE rules.id = #{id}")
    MysqlDeviceLogPluginSettings getPluginSettingsByRuleIdForAuthorization(@Param("id") int id);

    @Select("SELECT * FROM plugin_devicelog_settings_rules WHERE id = #{id}")
    MysqlDeviceLogRule getPluginSettingsRule(@Param("id") int id);

    @Delete("DELETE FROM plugin_devicelog_setting_rule_devices WHERE ruleId = #{id}")
    void deletePluginSettingsRuleDevices(@Param("id") int ruleId);

    void insertPluginSettingsRuleDevices(@Param("ruleId") int ruleId, @Param("deviceIds") List<Integer> deviceIds);

    /**
     * <p>Deletes the log records which are older than number of days configured in customer's profile.</p>
     *
     * @return a number of deleted records.
     */
    @Delete("DELETE FROM plugin_devicelog_log " +
            "WHERE createTime < (SELECT UNIX_TIMESTAMP(DATE(NOW() - INTERVAL pds.logsPreservePeriod DAY)) * 1000 " +
            "                    FROM plugin_devicelog_settings pds " +
            "                    WHERE pds.customerId = #{customerId})")
    int purgeLogRecords(@Param("customerId") int customerId);
}
