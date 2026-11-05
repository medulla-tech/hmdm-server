/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.devicelog.persistence.mysql.dao.domain;

import com.hmdm.plugins.devicelog.model.DeviceLogRule;

/**
 * <p>A single device log rule stored in <code>Mysql</code> database.</p>
 *
 * @author isv
 */
public class MysqlDeviceLogRule extends DeviceLogRule {

    private static final long serialVersionUID = 1792395795601813159L;

    /**
     * <p>An ID of a rule record.</p>
     */
    private Integer id;

    /**
     * <p>An ID of a setting record.</p>
     */
    private Integer settingId;

    /**
     * <p>Constructs new <code>MysqlDeviceLogRule</code> instance. This implementation does nothing.</p>
     */
    public MysqlDeviceLogRule() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getSettingId() {
        return settingId;
    }

    public void setSettingId(Integer settingId) {
        this.settingId = settingId;
    }

    @Override
    public String toString() {
        return "MysqlDeviceLogRule{" +
                "id=" + id +
                ", settingId=" + settingId +
                ", data=" + super.toString() +
                '}';
    }
}
