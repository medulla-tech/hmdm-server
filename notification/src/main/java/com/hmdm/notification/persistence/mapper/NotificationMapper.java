/*
 *
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hmdm.notification.persistence.mapper;

import com.hmdm.notification.persistence.domain.PushMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;

import java.util.List;

/**
 * <p>An ORM mapper for <code>Notification</code> sub-system domain objects.</p>
 *
 * @author isv
 */
public interface NotificationMapper {

    @Select("SELECT pushmessages.* " +
            "FROM pendingpushes " +
            "INNER JOIN pushmessages ON pushmessages.id = pendingpushes.messageId " +
            "INNER JOIN devices ON devices.id = pushmessages.deviceId " +
            "WHERE (devices.number = #{deviceNumber} OR devices.oldNumber = #{deviceNumber}) " +
            "AND pendingpushes.status = 0 " +
            "ORDER BY pendingpushes.createTime ASC")
    List<PushMessage> getPendingMessagesByNumber(@Param("deviceNumber") String deviceNumber);

    @Select("SELECT pushmessages.* " +
            "FROM pendingpushes " +
            "INNER JOIN pushmessages ON pushmessages.id = pendingpushes.messageId " +
            "WHERE pushmessages.deviceId = #{deviceId} " +
            "AND pendingpushes.status = 0 " +
            "ORDER BY pendingpushes.createTime ASC")
    List<PushMessage> getPendingMessagesById(@Param("deviceId") int deviceId);

    void markMessagesAsDelivered(@Param("messageIds") List<Integer> messageIds);

    @Insert("INSERT INTO pushmessages (messageType, deviceId, payload) " +
            "VALUES (#{messageType}, #{deviceId}, #{payload})")
    @SelectKey( statement = "SELECT currval('pushmessages_id_seq')", keyColumn = "id", keyProperty = "id", before = false, resultType = int.class )
    void insertPushMessage(PushMessage message);

    @Insert("INSERT INTO pendingpushes (messageId, status, createTime) " +
            "VALUES (#{messageId}, 0, UNIX_TIMESTAMP(NOW()) * 1000)")
    void insertPendingPush(int messageId);

    @Select("SELECT status FROM pendingpushes WHERE messageId = #{messageId}")
    Integer getDeliveryStatus(@Param("messageId") int messageId);

    @Delete("DELETE FROM pushmessages " +
            "WHERE EXISTS " +
            "(" +
            " SELECT 1 " +
            " FROM pendingpushes " +
            " WHERE pendingpushes.messageId = pushmessages.id " +
            " AND (" +
            "      pendingpushes.status = 0 AND UNIX_TIMESTAMP(NOW()) * 1000 - pendingpushes.createTime >= #{d1}" +
            "      OR " +
            "      pendingpushes.status = 1 AND NOT pendingpushes.sendTime IS NULL AND UNIX_TIMESTAMP(NOW()) * 1000 - pendingpushes.sendTime >= #{d2}" +
            "     )" +
            ")")
    void purgeMessages(@Param("d1") long nonDeliveredMessagesLifeSpan, @Param("d2") long deliveredMessagesLifeSpan);
}
