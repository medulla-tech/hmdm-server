/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol.persistence.domain;

public class RemoteControlSession {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ENDED = "ENDED";
    public static final String STATUS_ERROR = "ERROR";

    private int id;
    private int customerId;
    private int deviceId;
    private String deviceNumber;
    private int adminUserId;
    private String status;
    private String token;
    private long startTs;
    private Long endTs;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getDeviceId() { return deviceId; }
    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }

    public String getDeviceNumber() { return deviceNumber; }
    public void setDeviceNumber(String deviceNumber) { this.deviceNumber = deviceNumber; }

    public int getAdminUserId() { return adminUserId; }
    public void setAdminUserId(int adminUserId) { this.adminUserId = adminUserId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public long getStartTs() { return startTs; }
    public void setStartTs(long startTs) { this.startTs = startTs; }

    public Long getEndTs() { return endTs; }
    public void setEndTs(Long endTs) { this.endTs = endTs; }
}
