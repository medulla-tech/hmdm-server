/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.devicelog.persistence.mysql;

import com.google.inject.Module;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.devicelog.persistence.DeviceLogPersistenceConfiguration;
import com.hmdm.plugins.devicelog.persistence.mysql.guice.module.DeviceLogMysqlLiquibaseModule;
import com.hmdm.plugins.devicelog.persistence.mysql.guice.module.DeviceLogMysqlPersistenceModule;
import com.hmdm.plugins.devicelog.persistence.mysql.guice.module.DeviceLogMysqlServiceModule;
import com.hmdm.plugins.devicelog.persistence.mysql.guice.module.DeviceLogMysqlTaskModule;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>A device log persistence configuration backed by the mysql services.</p>
 *
 * @author isv
 */
public class DeviceLogMysqlPersistenceConfiguration implements DeviceLogPersistenceConfiguration {

    /**
     * <p>Constructs new <code>DeviceLogMysqlPersistenceConfiguration</code> instance. This implementation does nothing.</p>
     */
    public DeviceLogMysqlPersistenceConfiguration() {
    }

    /**
     * <p>Gets the list of modules to be used for initializing the persistence layer.</p>
     *
     * @param context a context for plugin usage.
     * @return a list of modules to be used for persistence layer initialization.
     */
    @Override
    public List<Module> getPersistenceModules(ServletContext context) {
        List<Module> modules = new ArrayList<>();

        modules.add(new DeviceLogMysqlLiquibaseModule(context));
        modules.add(new DeviceLogMysqlServiceModule());
        modules.add(new DeviceLogMysqlPersistenceModule(context));

        return modules;
    }

    /**
     * <p>Gets the list of task modules to be initialized upon application startup.</p>
     *
     * @param context a context for plugin usage.
     * @return an optional list of task modules for plugins.
     */
    @Override
    public Optional<List<Class<? extends PluginTaskModule>>> getTaskModules(ServletContext context) {
        List<Class<? extends PluginTaskModule>> modules = new ArrayList<>();

        modules.add(DeviceLogMysqlTaskModule.class);

        return Optional.of(modules);
    }
}
