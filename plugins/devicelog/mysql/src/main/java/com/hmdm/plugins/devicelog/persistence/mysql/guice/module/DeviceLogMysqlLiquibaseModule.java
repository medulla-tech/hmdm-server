/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.devicelog.persistence.mysql.guice.module;

import com.hmdm.guice.module.AbstractLiquibaseModule;
import com.hmdm.plugin.guice.module.PluginLiquibaseResourceAccessor;
import liquibase.resource.ResourceAccessor;

import javax.servlet.ServletContext;

/**
 * <p>A module used for initializing and managing the state of the database tables related to <code>Device Log</code>
 * plugin backed by <code>Mysql</code> database.</p>
 *
 * @author isv
 */
public class DeviceLogMysqlLiquibaseModule extends AbstractLiquibaseModule {

    /**
     * <p>Constructs new <code>DeviceLogMysqlLiquibaseModule</code> instance. This implementation does nothing.</p>
     */
    public DeviceLogMysqlLiquibaseModule(ServletContext context) {
        super(context);
    }


    /**
     * <p>Gets the path to the DB change log to be used by this module.</p>
     *
     * <p>Plugins MUST override this method to provide the path to specific Db change log.</p>
     *
     * @return a path to resource with Db change log.
     */
    @Override
    protected String getChangeLogResourcePath() {
        String path = this.getClass().getResource("/liquibase/devicelog.mysql.changelog.xml").getPath();
        if (!path.startsWith("jar:")) {
            path = "jar:" + path;
        }
        return path;
    }

    /**
     * <p>Gets the resource accessor to be used for loading the change log file.</p>
     *
     * @return a resource accessor for change log file.
     */
    @Override
    protected ResourceAccessor getResourceAccessor() {
        return new PluginLiquibaseResourceAccessor();
    }

}
