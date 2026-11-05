/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.netfilter.persistence.mapper;

import com.hmdm.plugins.netfilter.persistence.domain.NetFilterProfile;
import com.hmdm.plugins.netfilter.persistence.domain.NetFilterProfileRule;
import com.hmdm.plugins.netfilter.persistence.domain.NetFilterRule;
import com.hmdm.plugins.netfilter.persistence.domain.NetFilterSettings;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface NetFilterMapper {

    // -------------------------------------------------------------------------
    // Legacy (customer-level) settings and rules — kept for backward compat
    // -------------------------------------------------------------------------

    @Select("SELECT * FROM plugin_netfilter_settings WHERE customerId = #{customerId}")
    NetFilterSettings findSettings(@Param("customerId") int customerId);

    @Insert("INSERT INTO plugin_netfilter_settings (customerId, enabled, filterMode) " +
            "VALUES (#{customerId}, #{enabled}, #{filterMode}) " +
            "ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), filterMode = VALUES(filterMode)")
    void saveSettings(NetFilterSettings settings);

    @Select("SELECT * FROM plugin_netfilter_rules WHERE customerId = #{customerId} ORDER BY createdAt ASC")
    List<NetFilterRule> findRules(@Param("customerId") int customerId);

    @Insert("INSERT INTO plugin_netfilter_rules (customerId, domain, ruleType, enabled, createdAt) " +
            "VALUES (#{customerId}, #{domain}, #{ruleType}, #{enabled}, #{createdAt})")
    @SelectKey(statement = "SELECT LAST_INSERT_ID()", keyProperty = "id", before = false, resultType = int.class)
    void insertRule(NetFilterRule rule);

    @Update("UPDATE plugin_netfilter_rules SET domain = #{domain}, ruleType = #{ruleType}, enabled = #{enabled} " +
            "WHERE id = #{id} AND customerId = #{customerId}")
    int updateRule(NetFilterRule rule);

    @Delete("DELETE FROM plugin_netfilter_rules WHERE id = #{id} AND customerId = #{customerId}")
    int deleteRule(@Param("id") int id, @Param("customerId") int customerId);

    @Select("SELECT * FROM plugin_netfilter_rules WHERE id = #{id} AND customerId = #{customerId}")
    NetFilterRule findRuleById(@Param("id") int id, @Param("customerId") int customerId);

    // -------------------------------------------------------------------------
    // Profiles
    // -------------------------------------------------------------------------

    @Select("SELECT p.*, " +
            "(SELECT COUNT(*) FROM plugin_netfilter_profile_rule WHERE profileId = p.id AND enabled = TRUE) AS ruleCount, " +
            "(SELECT COUNT(*) FROM plugin_netfilter_profile_config WHERE profileId = p.id) AS configCount " +
            "FROM plugin_netfilter_profile p " +
            "WHERE p.customerId = #{customerId} ORDER BY p.createdAt ASC")
    List<NetFilterProfile> findProfiles(@Param("customerId") int customerId);

    @Select("SELECT * FROM plugin_netfilter_profile WHERE id = #{id} AND customerId = #{customerId}")
    NetFilterProfile findProfileById(@Param("id") int id, @Param("customerId") int customerId);

    @Insert("INSERT INTO plugin_netfilter_profile (customerId, name, filterMode, enabled, createdAt) " +
            "VALUES (#{customerId}, #{name}, #{filterMode}, #{enabled}, #{createdAt})")
    @SelectKey(statement = "SELECT LAST_INSERT_ID()", keyProperty = "id", before = false, resultType = int.class)
    void insertProfile(NetFilterProfile profile);

    @Update("UPDATE plugin_netfilter_profile SET name = #{name}, filterMode = #{filterMode}, enabled = #{enabled} " +
            "WHERE id = #{id} AND customerId = #{customerId}")
    int updateProfile(NetFilterProfile profile);

    @Delete("DELETE FROM plugin_netfilter_profile WHERE id = #{id} AND customerId = #{customerId}")
    int deleteProfile(@Param("id") int id, @Param("customerId") int customerId);

    // -------------------------------------------------------------------------
    // Profile rules
    // -------------------------------------------------------------------------

    @Select("SELECT * FROM plugin_netfilter_profile_rule WHERE profileId = #{profileId} ORDER BY createdAt ASC")
    List<NetFilterProfileRule> findProfileRules(@Param("profileId") int profileId);

    @Insert("INSERT INTO plugin_netfilter_profile_rule (profileId, domain, ruleType, enabled, createdAt) " +
            "VALUES (#{profileId}, #{domain}, #{ruleType}, #{enabled}, #{createdAt})")
    @SelectKey(statement = "SELECT LAST_INSERT_ID()", keyProperty = "id", before = false, resultType = int.class)
    void insertProfileRule(NetFilterProfileRule rule);

    @Update("UPDATE plugin_netfilter_profile_rule SET domain = #{domain}, ruleType = #{ruleType}, enabled = #{enabled} " +
            "WHERE id = #{id} AND profileId = #{profileId}")
    int updateProfileRule(NetFilterProfileRule rule);

    @Delete("DELETE FROM plugin_netfilter_profile_rule WHERE id = #{id} AND profileId = #{profileId}")
    int deleteProfileRule(@Param("id") int id, @Param("profileId") int profileId);

    // -------------------------------------------------------------------------
    // Profile-to-configuration mapping
    // -------------------------------------------------------------------------

    @Select("SELECT configurationId FROM plugin_netfilter_profile_config WHERE profileId = #{profileId}")
    List<Integer> findProfileConfigIds(@Param("profileId") int profileId);

    @Insert("INSERT IGNORE INTO plugin_netfilter_profile_config (profileId, configurationId) " +
            "VALUES (#{profileId}, #{configurationId})")
    void addConfigToProfile(@Param("profileId") int profileId, @Param("configurationId") int configurationId);

    @Delete("DELETE FROM plugin_netfilter_profile_config WHERE profileId = #{profileId} AND configurationId = #{configurationId}")
    int removeConfigFromProfile(@Param("profileId") int profileId, @Param("configurationId") int configurationId);

    // -------------------------------------------------------------------------
    // Device config — merge all enabled profiles attached to device's config
    // -------------------------------------------------------------------------

    @Select("SELECT r.domain, r.ruleType FROM plugin_netfilter_profile_rule r " +
            "JOIN plugin_netfilter_profile p ON p.id = r.profileId " +
            "JOIN plugin_netfilter_profile_config pc ON pc.profileId = p.id " +
            "JOIN devices d ON d.configurationId = pc.configurationId " +
            "WHERE d.number = #{deviceNumber} AND p.enabled = TRUE AND r.enabled = TRUE")
    List<NetFilterProfileRule> findEnabledProfileRulesByDeviceNumber(@Param("deviceNumber") String deviceNumber);

    @Select("SELECT DISTINCT p.filterMode FROM plugin_netfilter_profile p " +
            "JOIN plugin_netfilter_profile_config pc ON pc.profileId = p.id " +
            "JOIN devices d ON d.configurationId = pc.configurationId " +
            "WHERE d.number = #{deviceNumber} AND p.enabled = TRUE")
    List<String> findFilterModesByDeviceNumber(@Param("deviceNumber") String deviceNumber);
}
