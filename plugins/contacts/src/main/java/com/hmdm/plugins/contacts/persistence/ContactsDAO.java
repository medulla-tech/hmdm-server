/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.contacts.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.plugins.contacts.persistence.domain.ContactsConfig;
import com.hmdm.plugins.contacts.persistence.mapper.ContactsMapper;
import org.mybatis.guice.transactional.Transactional;

@Singleton
public class ContactsDAO {

    private final ContactsMapper contactsMapper;

    @Inject
    public ContactsDAO(ContactsMapper contactsMapper) {
        this.contactsMapper = contactsMapper;
    }

    public ContactsConfig getConfigForConfiguration(int configurationId) {
        return contactsMapper.findByConfigurationId(configurationId);
    }

    @Transactional
    public void saveConfig(ContactsConfig config) {
        contactsMapper.saveConfig(config);
    }
}
