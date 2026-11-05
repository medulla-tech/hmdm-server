/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.contacts.persistence.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Per-configuration settings for the Contacts plugin.
 * The server stores a VCF file URL and sync options; devices download this config
 * and pull contacts from the VCF URL.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactsConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private int configurationId;
    private String url;
    private String login;
    private String password;
    private String accountType;
    private int syncInterval;
    private boolean wipeContacts;

    public ContactsConfig() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public int getConfigurationId() { return configurationId; }
    public void setConfigurationId(int configurationId) { this.configurationId = configurationId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public int getSyncInterval() { return syncInterval; }
    public void setSyncInterval(int syncInterval) { this.syncInterval = syncInterval; }

    public boolean isWipeContacts() { return wipeContacts; }
    public void setWipeContacts(boolean wipeContacts) { this.wipeContacts = wipeContacts; }
}
