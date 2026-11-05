/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.netfilter;

import com.google.inject.Module;
import com.hmdm.plugin.PluginConfiguration;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.netfilter.guice.module.NetFilterLiquibaseModule;
import com.hmdm.plugins.netfilter.guice.module.NetFilterPersistenceModule;
import com.hmdm.plugins.netfilter.guice.module.NetFilterRestModule;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NetFilterPluginConfigurationImpl implements PluginConfiguration {

    public static final String PLUGIN_ID = "netfilter";

    public NetFilterPluginConfigurationImpl() {}

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getRootPackage() {
        return "com.hmdm.plugins.netfilter";
    }

    @Override
    public List<Module> getPluginModules(ServletContext context) {
        List<Module> modules = new ArrayList<>();
        modules.add(new NetFilterLiquibaseModule(context));
        modules.add(new NetFilterPersistenceModule(context));
        modules.add(new NetFilterRestModule());
        return modules;
    }

    @Override
    public Optional<List<Class<? extends PluginTaskModule>>> getTaskModules(ServletContext context) {
        return Optional.empty();
    }
}
