/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.netfilter.persistence.domain;

public class NetFilterProfile {

    private int id;
    private int customerId;
    private String name;
    private String filterMode;
    private boolean enabled;
    private long createdAt;
    private int ruleCount;
    private int configCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFilterMode() { return filterMode; }
    public void setFilterMode(String filterMode) { this.filterMode = filterMode; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getRuleCount() { return ruleCount; }
    public void setRuleCount(int ruleCount) { this.ruleCount = ruleCount; }

    public int getConfigCount() { return configCount; }
    public void setConfigCount(int configCount) { this.configCount = configCount; }
}
