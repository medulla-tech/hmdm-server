/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.devicelog.persistence.mysql.dao.domain;

import com.hmdm.persistence.domain.CustomerData;
import com.hmdm.plugins.devicelog.model.DeviceLogPluginSettings;

import java.util.ArrayList;

/**
 * <p>A device log settings record stored in <code>Mysql</code> database.</p>
 *
 * @author isv
 */
public class MysqlDeviceLogPluginSettings extends DeviceLogPluginSettings implements CustomerData {

    private static final long serialVersionUID = -6329947142252659953L;
    
    /**
     * <p>An ID of a setting record.</p>
     */
    private Integer id;

    /**
     * <p>An ID of a customer account which the record belongs to.</p>
     */
    private int customerId;

    /**
     * <p>Constructs new <code>MysqlDeviceLogPluginSettings</code> instance. This implementation does nothing.</p>
     */
    public MysqlDeviceLogPluginSettings() {
        setRules(new ArrayList<>());
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public int getCustomerId() {
        return customerId;
    }

    @Override
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    /**
     * <p>Gets the unique identifier for this record within underlying persistence layer.</p>
     *
     * @return an identifier for this record.
     */
    @Override
    public String getIdentifier() {
        return getId() == null ? null : getId().toString();
    }

}
