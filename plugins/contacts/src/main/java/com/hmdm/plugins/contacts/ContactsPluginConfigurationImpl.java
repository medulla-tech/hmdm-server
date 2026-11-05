/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.contacts;

import com.google.inject.Module;
import com.hmdm.plugin.PluginConfiguration;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.contacts.guice.module.ContactsLiquibaseModule;
import com.hmdm.plugins.contacts.guice.module.ContactsPersistenceModule;
import com.hmdm.plugins.contacts.guice.module.ContactsRestModule;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContactsPluginConfigurationImpl implements PluginConfiguration {

    public static final String PLUGIN_ID = "contacts";

    public ContactsPluginConfigurationImpl() {}

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getRootPackage() {
        return "com.hmdm.plugins.contacts";
    }

    @Override
    public List<Module> getPluginModules(ServletContext context) {
        List<Module> modules = new ArrayList<>();
        modules.add(new ContactsLiquibaseModule(context));
        modules.add(new ContactsPersistenceModule(context));
        modules.add(new ContactsRestModule());
        return modules;
    }

    @Override
    public Optional<List<Class<? extends PluginTaskModule>>> getTaskModules(ServletContext context) {
        return Optional.empty();
    }
}
