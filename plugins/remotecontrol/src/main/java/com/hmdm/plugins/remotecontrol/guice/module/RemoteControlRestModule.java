/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol.guice.module;

import com.google.inject.servlet.ServletModule;
import com.hmdm.plugin.rest.PluginAccessFilter;
import com.hmdm.plugins.remotecontrol.rest.RemoteControlResource;
import com.hmdm.plugins.remotecontrol.rest.RemoteControlSettingsResource;
import com.hmdm.plugins.remotecontrol.rest.RemoteControlSignalingService;
import com.hmdm.rest.filter.AuthFilter;
import com.hmdm.rest.filter.PrivateIPFilter;
import com.hmdm.rest.filter.PublicIPFilter;
import com.hmdm.security.jwt.JWTFilter;

import java.util.Collections;
import java.util.List;

public class RemoteControlRestModule extends ServletModule {

    private static final List<String> protectedResources = Collections.singletonList(
            "/rest/plugins/remotecontrol/private/*"
    );

    private static final List<String> publicResources = Collections.singletonList(
            "/rest/plugins/remotecontrol/public/*"
    );

    public RemoteControlRestModule() {}

    @Override
    protected void configureServlets() {
        this.filter(protectedResources).through(JWTFilter.class);
        this.filter(protectedResources).through(AuthFilter.class);
        this.filter(protectedResources).through(PluginAccessFilter.class);
        this.filter(protectedResources).through(PrivateIPFilter.class);
        this.filter(publicResources).through(PublicIPFilter.class);
        this.bind(RemoteControlResource.class);
        this.bind(RemoteControlSettingsResource.class);
        this.bind(RemoteControlSignalingService.class).asEagerSingleton();
    }
}
