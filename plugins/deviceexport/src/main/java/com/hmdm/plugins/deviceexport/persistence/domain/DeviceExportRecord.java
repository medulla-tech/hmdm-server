/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.deviceexport.persistence.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceExportRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String number;
    private String description;
    private String imei;
    private String phone;
    private Long lastUpdate;
    private Long enrollTime;
    private String publicIp;
    private String configName;
    private String groupNames;
    private String custom1;
    private String custom2;
    private String custom3;
    private String androidVersion;
    private String serial;
    private String launcherVersion;

    public DeviceExportRecord() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(Long lastUpdate) { this.lastUpdate = lastUpdate; }

    public Long getEnrollTime() { return enrollTime; }
    public void setEnrollTime(Long enrollTime) { this.enrollTime = enrollTime; }

    public String getPublicIp() { return publicIp; }
    public void setPublicIp(String publicIp) { this.publicIp = publicIp; }

    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }

    public String getGroupNames() { return groupNames; }
    public void setGroupNames(String groupNames) { this.groupNames = groupNames; }

    public String getCustom1() { return custom1; }
    public void setCustom1(String custom1) { this.custom1 = custom1; }

    public String getCustom2() { return custom2; }
    public void setCustom2(String custom2) { this.custom2 = custom2; }

    public String getCustom3() { return custom3; }
    public void setCustom3(String custom3) { this.custom3 = custom3; }

    public String getAndroidVersion() { return androidVersion; }
    public void setAndroidVersion(String androidVersion) { this.androidVersion = androidVersion; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public String getLauncherVersion() { return launcherVersion; }
    public void setLauncherVersion(String launcherVersion) { this.launcherVersion = launcherVersion; }
}
