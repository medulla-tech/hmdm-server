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

package com.hmdm.persistence.mapper;

import com.hmdm.persistence.domain.UserRoleSettings;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>An ORM Mapper for {@link UserRoleSettings} domain object.</p>
 *
 * @author isv
 */
public interface UserRoleSettingsMapper {

    @Select({"SELECT * " +
            "FROM userrolesettings " +
            "WHERE customerId = #{customerId} AND roleId = #{roleId} " +
            "LIMIT 1"})
    UserRoleSettings getUserRoleSettings(@Param("customerId") int customerId, @Param("roleId") int roleId);

    @Insert({
            "INSERT INTO userrolesettings (" +
                    "columnDisplayedDeviceStatus, " +
                    "columnDisplayedDeviceDate, " +
                    "columnDisplayedDeviceNumber, " +
                    "columnDisplayedDeviceModel, " +
                    "columnDisplayedDevicePermissionsStatus, " +
                    "columnDisplayedDeviceAppInstallStatus, " +
                    "columnDisplayedDeviceFilesStatus, " +
                    "columnDisplayedDeviceConfiguration," +
                    "columnDisplayedDeviceImei," +
                    "columnDisplayedDevicePhone," +
                    "columnDisplayedDeviceDesc," +
                    "columnDisplayedDeviceGroup," +
                    "columnDisplayedLauncherVersion," +
                    "columnDisplayedBatteryLevel," +
                    "columnDisplayedDefaultLauncher," +
                    "columnDisplayedMdmMode," +
                    "columnDisplayedKioskMode," +
                    "columnDisplayedAndroidVersion," +
                    "columnDisplayedEnrollmentDate," +
                    "columnDisplayedSerial," +
                    "columnDisplayedPublicIp," +
                    "columnDisplayedCustom1," +
                    "columnDisplayedCustom2," +
                    "columnDisplayedCustom3," +
                    "roleId," +
                    "customerId" +
                    ") VALUES (" +
                    "#{columnDisplayedDeviceStatus}, " +
                    "#{columnDisplayedDeviceDate}, " +
                    "#{columnDisplayedDeviceNumber}, " +
                    "#{columnDisplayedDeviceModel}, " +
                    "#{columnDisplayedDevicePermissionsStatus}, " +
                    "#{columnDisplayedDeviceAppInstallStatus}, " +
                    "#{columnDisplayedDeviceFilesStatus}, " +
                    "#{columnDisplayedDeviceConfiguration}, " +
                    "#{columnDisplayedDeviceImei}, " +
                    "#{columnDisplayedDevicePhone}," +
                    "#{columnDisplayedDeviceDesc}," +
                    "#{columnDisplayedDeviceGroup}," +
                    "#{columnDisplayedLauncherVersion}," +
                    "#{columnDisplayedBatteryLevel}," +
                    "#{columnDisplayedDefaultLauncher}," +
                    "#{columnDisplayedMdmMode}," +
                    "#{columnDisplayedKioskMode}," +
                    "#{columnDisplayedAndroidVersion}," +
                    "#{columnDisplayedEnrollmentDate}," +
                    "#{columnDisplayedSerial}," +
                    "#{columnDisplayedPublicIp}," +
                    "#{columnDisplayedCustom1}," +
                    "#{columnDisplayedCustom2}," +
                    "#{columnDisplayedCustom3}," +
                    "#{roleId}," +
                    "#{customerId}" +
                    ") ON DUPLICATE KEY UPDATE " +
                    "columnDisplayedDeviceStatus = VALUES(columnDisplayedDeviceStatus), " +
                    "columnDisplayedDeviceDate = VALUES(columnDisplayedDeviceDate), " +
                    "columnDisplayedDeviceNumber = VALUES(columnDisplayedDeviceNumber), " +
                    "columnDisplayedDeviceModel = VALUES(columnDisplayedDeviceModel), " +
                    "columnDisplayedDevicePermissionsStatus = VALUES(columnDisplayedDevicePermissionsStatus), " +
                    "columnDisplayedDeviceAppInstallStatus = VALUES(columnDisplayedDeviceAppInstallStatus), " +
                    "columnDisplayedDeviceFilesStatus = VALUES(columnDisplayedDeviceFilesStatus), " +
                    "columnDisplayedDeviceConfiguration = VALUES(columnDisplayedDeviceConfiguration), " +
                    "columnDisplayedDeviceImei = VALUES(columnDisplayedDeviceImei), " +
                    "columnDisplayedDevicePhone = VALUES(columnDisplayedDevicePhone), " +
                    "columnDisplayedDeviceDesc = VALUES(columnDisplayedDeviceDesc), " +
                    "columnDisplayedDeviceGroup = VALUES(columnDisplayedDeviceGroup), " +
                    "columnDisplayedLauncherVersion = VALUES(columnDisplayedLauncherVersion), " +
                    "columnDisplayedBatteryLevel = VALUES(columnDisplayedBatteryLevel), " +
                    "columnDisplayedDefaultLauncher = VALUES(columnDisplayedDefaultLauncher), " +
                    "columnDisplayedMdmMode = VALUES(columnDisplayedMdmMode), " +
                    "columnDisplayedKioskMode = VALUES(columnDisplayedKioskMode), " +
                    "columnDisplayedAndroidVersion = VALUES(columnDisplayedAndroidVersion), " +
                    "columnDisplayedEnrollmentDate = VALUES(columnDisplayedEnrollmentDate), " +
                    "columnDisplayedSerial = VALUES(columnDisplayedSerial), " +
                    "columnDisplayedPublicIp = VALUES(columnDisplayedPublicIp), " +
                    "columnDisplayedCustom1 = VALUES(columnDisplayedCustom1), " +
                    "columnDisplayedCustom2 = VALUES(columnDisplayedCustom2), " +
                    "columnDisplayedCustom3 = VALUES(columnDisplayedCustom3)"
    })
    void saveUserRoleCommonSettings(UserRoleSettings settings);
}
