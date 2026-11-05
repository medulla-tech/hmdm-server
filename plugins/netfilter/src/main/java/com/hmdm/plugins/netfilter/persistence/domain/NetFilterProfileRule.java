/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.netfilter.persistence.domain;

public class NetFilterProfileRule {

    private int id;
    private int profileId;
    private String domain;
    private String ruleType;
    private boolean enabled;
    private long createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
