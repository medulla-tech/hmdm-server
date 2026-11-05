/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.deviceexport.guice.module;

import com.google.inject.servlet.ServletModule;
import com.hmdm.plugin.rest.PluginAccessFilter;
import com.hmdm.plugins.deviceexport.rest.DeviceExportResource;
import com.hmdm.rest.filter.AuthFilter;
import com.hmdm.rest.filter.PrivateIPFilter;
import com.hmdm.security.jwt.JWTFilter;

import java.util.Collections;
import java.util.List;

public class DeviceExportRestModule extends ServletModule {

    private static final List<String> protectedResources = Collections.singletonList(
            "/rest/plugins/deviceexport/private/*"
    );

    public DeviceExportRestModule() {}

    @Override
    protected void configureServlets() {
        this.filter(protectedResources).through(JWTFilter.class);
        this.filter(protectedResources).through(AuthFilter.class);
        this.filter(protectedResources).through(PluginAccessFilter.class);
        this.filter(protectedResources).through(PrivateIPFilter.class);
        this.bind(DeviceExportResource.class);
    }
}
