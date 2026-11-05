/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.photos.guice.module;

import com.google.inject.servlet.ServletModule;
import com.hmdm.plugin.rest.PluginAccessFilter;
import com.hmdm.plugins.photos.rest.PhotosResource;
import com.hmdm.plugins.photos.rest.PhotosSettingsResource;
import com.hmdm.rest.filter.AuthFilter;
import com.hmdm.rest.filter.PrivateIPFilter;
import com.hmdm.rest.filter.PublicIPFilter;
import com.hmdm.security.jwt.JWTFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PhotosRestModule extends ServletModule {

    private static final List<String> protectedResources = Collections.singletonList(
            "/rest/plugins/photos/private/*"
    );

    private static final List<String> publicResources = Collections.singletonList(
            "/rest/plugins/photos/public/*"
    );

    public PhotosRestModule() {}

    @Override
    protected void configureServlets() {
        this.filter(protectedResources).through(JWTFilter.class);
        this.filter(protectedResources).through(AuthFilter.class);
        this.filter(protectedResources).through(PluginAccessFilter.class);
        this.filter(protectedResources).through(PrivateIPFilter.class);
        this.filter(publicResources).through(PublicIPFilter.class);
        this.bind(PhotosResource.class);
        this.bind(PhotosSettingsResource.class);
    }
}
