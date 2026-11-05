/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.remotecontrol.persistence.domain;

public class RemoteControlSettings {

    private int id;
    private int customerId;
    private String stunUrl;
    private String turnUrl;
    private String turnUsername;
    private String turnPassword;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getStunUrl() { return stunUrl; }
    public void setStunUrl(String stunUrl) { this.stunUrl = stunUrl; }

    public String getTurnUrl() { return turnUrl; }
    public void setTurnUrl(String turnUrl) { this.turnUrl = turnUrl; }

    public String getTurnUsername() { return turnUsername; }
    public void setTurnUsername(String turnUsername) { this.turnUsername = turnUsername; }

    public String getTurnPassword() { return turnPassword; }
    public void setTurnPassword(String turnPassword) { this.turnPassword = turnPassword; }
}
