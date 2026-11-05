/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.devicelog.persistence.mysql.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.persistence.AbstractDAO;
import com.hmdm.persistence.domain.Customer;
import com.hmdm.plugins.devicelog.model.DeviceLogPluginSettings;
import com.hmdm.plugins.devicelog.model.DeviceLogRule;
import com.hmdm.plugins.devicelog.persistence.DeviceLogPluginSettingsDAO;
import com.hmdm.plugins.devicelog.persistence.mysql.dao.domain.MysqlDeviceLogPluginSettings;
import com.hmdm.plugins.devicelog.persistence.mysql.dao.domain.MysqlDeviceLogRule;
import com.hmdm.plugins.devicelog.persistence.mysql.dao.mapper.MysqlDeviceLogMapper;
import com.hmdm.rest.json.LookupItem;
import com.hmdm.security.SecurityException;
import org.mybatis.guice.transactional.Transactional;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>A DAO for device log plugin settings backed by the <code>Mysql</code> database.</p>
 *
 * @author isv
 */
@Singleton
public class MysqlDeviceLogPluginSettingsDAO extends AbstractDAO<MysqlDeviceLogPluginSettings>
        implements DeviceLogPluginSettingsDAO {

    /**
     * <p>An ORM mapper for domain object type.</p>
     */
    private final MysqlDeviceLogMapper mapper;

    /**
     * <p>Constructs new <code>MysqlDeviceLogPluginSettingsDAO</code> instance. This implementation does nothing.</p>
     */
    @Inject
    public MysqlDeviceLogPluginSettingsDAO(MysqlDeviceLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public DeviceLogPluginSettings getPluginSettings() {
        final MysqlDeviceLogPluginSettings settings = getSingleRecord(this.mapper::findPluginSettingsByCustomerId);
        if (settings != null && settings.getRules() != null && settings.getRules().size() == 1 && settings.getRules().get(0).getIdentifier() == null) {
            settings.setRules(new ArrayList<>());
        }
        return settings;
    }

    @Override
    public DeviceLogPluginSettings getDefaultSettings() {
        return new MysqlDeviceLogPluginSettings();
    }

    @Override
    public DeviceLogPluginSettings getDefaultSettings(Customer customer) {
        MysqlDeviceLogPluginSettings settings = new MysqlDeviceLogPluginSettings();
        settings.setCustomerId(customer.getId());
        return settings;
    }

    @Override
    public void insertPluginSettings(DeviceLogPluginSettings settings) {
        MysqlDeviceLogPluginSettings mysqlSettings = (MysqlDeviceLogPluginSettings) settings;
        insertRecord(mysqlSettings, this.mapper::insertPluginSettings);
    }

    @Override
    public void insertPluginSettings(DeviceLogPluginSettings settings, Customer customer) {
        MysqlDeviceLogPluginSettings mysqlSettings = (MysqlDeviceLogPluginSettings) settings;
        mysqlSettings.setCustomerId(customer.getId());
        this.mapper.insertPluginSettings(mysqlSettings);
    }

    @Override
    public void updatePluginSettings(DeviceLogPluginSettings settings) {
        MysqlDeviceLogPluginSettings mysqlSettings = (MysqlDeviceLogPluginSettings) settings;
        updateRecord(
                mysqlSettings,
                this.mapper::updatePluginSettings,
                s -> SecurityException.onCustomerDataAccessViolation(s.getId(), "pluginDeviceLogSettings")
        );
    }

    @Override
    @Transactional
    public void savePluginSettingsRule(@NotNull DeviceLogRule rule) {
        savePluginSettingsRule(getPluginSettings(), rule);
    }

    @Override
    @Transactional
    public void savePluginSettingsRule(@NotNull DeviceLogPluginSettings settings, @NotNull DeviceLogRule rule) {
        MysqlDeviceLogPluginSettings mysqlSettings = (MysqlDeviceLogPluginSettings) settings;
        if (mysqlSettings != null) {
            MysqlDeviceLogRule mysqlRule = (MysqlDeviceLogRule) rule;

            mysqlRule.setSettingId(mysqlSettings.getId());

            if (mysqlRule.getId() == null) {
                this.mapper.insertPluginSettingsRule(mysqlRule);
            } else {
                this.mapper.updatePluginSettingsRule(mysqlRule);
            }


            this.mapper.deletePluginSettingsRuleDevices(mysqlRule.getId());
            if (mysqlRule.getDevices() != null && !mysqlRule.getDevices().isEmpty()) {
                final List<Integer> deviceIds = mysqlRule.getDevices()
                        .stream()
                        .map(LookupItem::getId)
                        .collect(Collectors.toList());
                this.mapper.insertPluginSettingsRuleDevices(mysqlRule.getId(), deviceIds);
            }

        } else {
            throw new IllegalStateException("Device Log Plugin settings record is required to be created prior " +
                    "to saving the rule: " + rule);
        }
    }

    @Transactional
    public DeviceLogRule getPluginSettingsRuleById(int id) {
        return mapper.getPluginSettingsRule(id);
    }

    @Transactional
    public void deletePluginSettingRule(int id) {
        final MysqlDeviceLogPluginSettings settings = getSingleRecord(
                () -> this.mapper.getPluginSettingsByRuleIdForAuthorization(id),
                (s) -> SecurityException.onCustomerDataAccessViolation(id, "pluginDeviceLogRule")
        );

        if (settings != null) {
            this.mapper.deletePluginSettingRule(id);
        }
    }

    @Override
    public Class<? extends DeviceLogPluginSettings> getSettingsClass() {
        return MysqlDeviceLogPluginSettings.class;
    }

    @Override
    public Class<? extends DeviceLogRule> getSettingsRuleClass() {
        return MysqlDeviceLogRule.class;
    }

    @Override
    public DeviceLogPluginSettings getPluginSettings(int customerId) {
        final MysqlDeviceLogPluginSettings settings = this.mapper.findPluginSettingsByCustomerId(customerId);
        if (settings.getRules() != null && settings.getRules().size() == 1 && settings.getRules().get(0).getIdentifier() == null) {
            settings.setRules(new ArrayList<>());
        }

        return settings;
    }
}
