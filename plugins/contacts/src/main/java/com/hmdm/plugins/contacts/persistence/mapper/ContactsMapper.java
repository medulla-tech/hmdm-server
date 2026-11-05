/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.contacts.persistence.mapper;

import com.hmdm.plugins.contacts.persistence.domain.ContactsConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ContactsMapper {

    @Select("SELECT * FROM plugin_contacts_config WHERE configurationId = #{configurationId}")
    ContactsConfig findByConfigurationId(@Param("configurationId") int configurationId);

    @Insert("INSERT INTO plugin_contacts_config (configurationId, url, login, password, accountType, syncInterval, wipeContacts) " +
            "VALUES (#{configurationId}, #{url}, #{login}, #{password}, #{accountType}, #{syncInterval}, #{wipeContacts}) " +
            "ON DUPLICATE KEY UPDATE url=VALUES(url), login=VALUES(login), password=VALUES(password), " +
            "accountType=VALUES(accountType), syncInterval=VALUES(syncInterval), wipeContacts=VALUES(wipeContacts)")
    void saveConfig(ContactsConfig config);
}
