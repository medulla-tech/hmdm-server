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

package com.hmdm.plugins.push.persistence.mapper;

import com.hmdm.plugins.push.persistence.domain.PluginPushMessage;
import com.hmdm.plugins.push.persistence.domain.PluginPushSchedule;
import com.hmdm.plugins.push.rest.json.PushMessageFilter;
import com.hmdm.plugins.push.rest.json.PushScheduleFilter;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * <p>An ORM mapper for {@link PluginPushSchedule} domain objects.</p>
 *
 * @author seva
 */
public interface PushScheduleMapper {

    @Insert("INSERT INTO plugin_push_schedule " +
            "(customerId, deviceId, groupId, configurationId, scope, messageType, payload, comment, " +
            "min, minBit, hour, hourBit, day, dayBit, weekday, weekdayBit, month, monthBit) " +
            "VALUES " +
            "(#{customerId}, #{deviceId}, #{groupId}, #{configurationId}, #{scope}, #{messageType}, #{payload}, #{comment}, " +
            "#{min}, #{minBit}, #{hour}, #{hourBit}, #{day}, #{dayBit}, #{weekday}, #{weekdayBit}, #{month}, #{monthBit})"
    )
    @SelectKey( statement = "SELECT currval('plugin_push_schedule_id_seq')",
            keyColumn = "id", keyProperty = "id", before = false, resultType = int.class )
    int insert(PluginPushSchedule msg);

    @Update("UPDATE plugin_push_schedule " +
            "SET deviceId=#{deviceId}, groupId=#{groupId}, configurationId=#{configurationId}, " +
            "scope=#{scope}, messageType=#{messageType}, payload=#{payload}, comment=#{comment}, " +
            "min=#{min}, minBit=#{minBit}, hour=#{hour}, hourBit=#{hourBit}, " +
            "day=#{day}, dayBit=#{dayBit}, weekday=#{weekday}, weekdayBit=#{weekdayBit}, " +
            "month=#{month}, monthBit=#{monthBit} " +
            "WHERE id=#{id} AND customerId=#{customerId}"
    )
    int update(PluginPushSchedule msg);

    @Delete("DELETE FROM plugin_push_schedule WHERE id = #{id} AND customerId = #{customerId}")
    void delete(@Param("id") int id, @Param("customerId") int customerId);

    @Select("SELECT * FROM plugin_push_schedule WHERE " +
            "(minBit & #{minMask}) = #{minMask} AND " +
            "(hourBit & #{hourMask}) = #{hourMask} AND " +
            "(dayBit & #{dayMask}) = #{dayMask} AND " +
            "(weekdayBit & #{weekdayMask}) = #{weekdayMask} AND " +
            "(monthBit & #{monthMask}) = #{monthMask}"
    )
    List<PluginPushSchedule> findMatchingTime(@Param("minMask") long minMask,
                                            @Param("hourMask") int hourMask,
                                            @Param("dayMask") int dayMask,
                                            @Param("weekdayMask") int weekdayMask,
                                            @Param("monthMask") int monthMask);

    List<PluginPushSchedule> findAll(PushScheduleFilter filter);

    long countAll(PushScheduleFilter filter);
}
