/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.devicelog.persistence.mysql.guice.module;

import com.hmdm.guice.module.AbstractPersistenceModule;

import javax.servlet.ServletContext;

/**
 * <p>A module used for configuring the {@link org.apache.ibatis.session.SqlSessionFactory} to be used by the
 * persistence layer of the <code>Device Log</code> plugin backed by <code>Mysql</code> database.</p>
 *
 * @author isv
 */
public class DeviceLogMysqlPersistenceModule extends AbstractPersistenceModule {

    /**
     * <p>Constructs new <code>DeviceLogMysqlPersistenceModule</code> instance. This implementation does nothing.</p>
     */
    public DeviceLogMysqlPersistenceModule(ServletContext context) {
        super(context);
    }

    /**
     * <p>Gets the name of Java package containing the MyBatis mapper classes to be used by the persistence layer of the
     * application.</p>
     *
     * @return a fully-qualified name of package with MyBatis mapper classes.
     */
    @Override
    protected String getMapperPackageName() {
        return "com.hmdm.plugins.devicelog.persistence.mysql.dao.mapper";
    }

    /**
     * <p>Gets the name of Java package containing the Domain Object classes to be used by the persistence layer of the
     * application.</p>
     *
     * @return a fully-qualified name of package with Domain Object classes.
     */
    @Override
    protected String getDomainObjectsPackageName() {
        return "com.hmdm.plugins.devicelog.persistence.mysql.dao.domain";
    }


}
