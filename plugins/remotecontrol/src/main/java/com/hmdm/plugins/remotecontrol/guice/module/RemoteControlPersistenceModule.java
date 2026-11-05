/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol.guice.module;

import com.hmdm.plugin.guice.module.PluginPersistenceModule;

import javax.servlet.ServletContext;

public class RemoteControlPersistenceModule extends PluginPersistenceModule {

    public RemoteControlPersistenceModule(ServletContext context) {
        super(context);
    }

    @Override
    protected String getMapperPackageName() {
        return "com.hmdm.plugins.remotecontrol.persistence.mapper";
    }

    @Override
    protected String getDomainObjectsPackageName() {
        return "com.hmdm.plugins.remotecontrol.persistence.domain";
    }
}
