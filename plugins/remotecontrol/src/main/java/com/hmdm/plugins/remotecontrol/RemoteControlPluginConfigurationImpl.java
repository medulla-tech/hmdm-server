/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol;

import com.google.inject.Module;
import com.hmdm.plugin.PluginConfiguration;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.remotecontrol.guice.module.RemoteControlLiquibaseModule;
import com.hmdm.plugins.remotecontrol.guice.module.RemoteControlPersistenceModule;
import com.hmdm.plugins.remotecontrol.guice.module.RemoteControlRestModule;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RemoteControlPluginConfigurationImpl implements PluginConfiguration {

    public static final String PLUGIN_ID = "remotecontrol";

    public RemoteControlPluginConfigurationImpl() {}

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getRootPackage() {
        return "com.hmdm.plugins.remotecontrol";
    }

    @Override
    public List<Module> getPluginModules(ServletContext context) {
        List<Module> modules = new ArrayList<>();
        modules.add(new RemoteControlLiquibaseModule(context));
        modules.add(new RemoteControlPersistenceModule(context));
        modules.add(new RemoteControlRestModule());
        return modules;
    }

    @Override
    public Optional<List<Class<? extends PluginTaskModule>>> getTaskModules(ServletContext context) {
        return Optional.empty();
    }
}
