/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.deviceexport;

import com.google.inject.Module;
import com.hmdm.plugin.PluginConfiguration;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.deviceexport.guice.module.DeviceExportLiquibaseModule;
import com.hmdm.plugins.deviceexport.guice.module.DeviceExportPersistenceModule;
import com.hmdm.plugins.deviceexport.guice.module.DeviceExportRestModule;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeviceExportPluginConfigurationImpl implements PluginConfiguration {

    public static final String PLUGIN_ID = "deviceexport";

    public DeviceExportPluginConfigurationImpl() {}

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getRootPackage() {
        return "com.hmdm.plugins.deviceexport";
    }

    @Override
    public List<Module> getPluginModules(ServletContext context) {
        List<Module> modules = new ArrayList<>();
        modules.add(new DeviceExportLiquibaseModule(context));
        modules.add(new DeviceExportPersistenceModule(context));
        modules.add(new DeviceExportRestModule());
        return modules;
    }

    @Override
    public Optional<List<Class<? extends PluginTaskModule>>> getTaskModules(ServletContext context) {
        return Optional.empty();
    }
}
