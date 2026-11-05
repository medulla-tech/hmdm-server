/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.netfilter.persistence.domain;

public class NetFilterSettings {

    public static final String MODE_BLOCKLIST = "BLOCKLIST";
    public static final String MODE_ALLOWLIST = "ALLOWLIST";

    private int id;
    private int customerId;
    private boolean enabled;
    private String filterMode;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFilterMode() { return filterMode; }
    public void setFilterMode(String filterMode) { this.filterMode = filterMode; }
}
