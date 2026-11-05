/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.devicelog.persistence.mysql.guice.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.hmdm.plugins.devicelog.persistence.DeviceLogDAO;
import com.hmdm.plugins.devicelog.persistence.DeviceLogPluginSettingsDAO;
import com.hmdm.plugins.devicelog.persistence.mysql.dao.MysqlDeviceLogDAO;
import com.hmdm.plugins.devicelog.persistence.mysql.dao.MysqlDeviceLogPluginSettingsDAO;

/**
 * <p>A module used to bind the service interfaces to specific implementations provided by the <code>Mysql</code>
 * persistence layer for <code>Device Log</code> plugin..</p>
 *
 * @author isv
 */
public class DeviceLogMysqlServiceModule extends AbstractModule {

    /**
     * <p>Constructs new <code>DeviceLogMysqlServiceModule</code> instance. This implementation does nothing.</p>
     */
    public DeviceLogMysqlServiceModule() {
    }

    /**
     * <p>Configures the services exposed by the <code>Mysql</code> persistence layer for <code>Device Log</code>
     * plugin.</p>
     */
    @Override
    protected void configure() {
        bind(DeviceLogPluginSettingsDAO.class).to(MysqlDeviceLogPluginSettingsDAO.class).in(Singleton.class);
        bind(DeviceLogDAO.class).to(MysqlDeviceLogDAO.class).in(Singleton.class);
    }
}
