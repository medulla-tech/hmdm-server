/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.photos;

import com.google.inject.Module;
import com.hmdm.plugin.PluginConfiguration;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.photos.guice.module.PhotosLiquibaseModule;
import com.hmdm.plugins.photos.guice.module.PhotosPersistenceModule;
import com.hmdm.plugins.photos.guice.module.PhotosRestModule;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PhotosPluginConfigurationImpl implements PluginConfiguration {

    public static final String PLUGIN_ID = "photos";

    public PhotosPluginConfigurationImpl() {}

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getRootPackage() {
        return "com.hmdm.plugins.photos";
    }

    @Override
    public List<Module> getPluginModules(ServletContext context) {
        List<Module> modules = new ArrayList<>();
        modules.add(new PhotosLiquibaseModule(context));
        modules.add(new PhotosPersistenceModule(context));
        modules.add(new PhotosRestModule());
        return modules;
    }

    @Override
    public Optional<List<Class<? extends PluginTaskModule>>> getTaskModules(ServletContext context) {
        return Optional.empty();
    }
}
