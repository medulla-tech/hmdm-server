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

package com.hmdm.plugins.deviceinfo.persistence.mapper;

import com.hmdm.plugins.deviceinfo.persistence.domain.DeviceData;
import com.hmdm.plugins.deviceinfo.persistence.domain.DeviceDynamicInfo;
import com.hmdm.plugins.deviceinfo.persistence.domain.DeviceInfoPluginSettings;
import com.hmdm.plugins.deviceinfo.persistence.domain.GpsData;
import com.hmdm.plugins.deviceinfo.persistence.domain.MobileData;
import com.hmdm.plugins.deviceinfo.persistence.domain.WifiData;
import com.hmdm.plugins.deviceinfo.rest.json.DeviceDynamicInfoRecord;
import com.hmdm.plugins.deviceinfo.rest.json.DeviceInfo;
import com.hmdm.plugins.deviceinfo.rest.json.DynamicInfoExportFilter;
import com.hmdm.plugins.deviceinfo.rest.json.DynamicInfoFilter;
import com.hmdm.rest.json.LookupItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.cursor.Cursor;

import java.util.List;

/**
 * <p>An ORM mapper for {@link DeviceInfoPluginSettings} domain objects.</p>
 *
 * @author isv
 */
public interface DeviceInfoMapper {

    @Select("SELECT settings.* " +
            "FROM plugin_deviceinfo_settings settings " +
            "WHERE customerId = #{customerId}")
    DeviceInfoPluginSettings findPluginSettingsByCustomerId(@Param("customerId") Integer customerId);

    @Insert({
            "INSERT INTO plugin_deviceinfo_settings " +
                    "(" +
                    "dataPreservePeriod, intervalMins, sendData, customerId" +
                    ") " +
                    "VALUES (" +
                    "#{dataPreservePeriod}, #{intervalMins}, #{sendData}, #{customerId}" +
                    ") " +
                    "ON DUPLICATE KEY UPDATE " +
                    "dataPreservePeriod = VALUES(dataPreservePeriod), " +
                    "intervalMins = VALUES(intervalMins), " +
                    "sendData = VALUES(sendData)"
    })
    void savePluginSettings(DeviceInfoPluginSettings settings);

    @Insert("INSERT INTO plugin_deviceinfo_deviceparams (deviceId, customerId, ts) VALUES (#{deviceId}, #{customerId}, #{ts})")
    @SelectKey(statement = "SELECT currval('plugin_deviceinfo_deviceparams_id_seq')",
            keyColumn = "id", keyProperty = "id", before = false, resultType = int.class)
    int insertDeviceInfoMain(DeviceDynamicInfo record);

    @Insert("INSERT INTO plugin_deviceinfo_deviceparams_device (" +
            "  recordId," +
            "  batteryLevel," +
            "  batteryCharging," +
            "  ip," +
            "  keyguard," +
            "  ringVolume," +
            "  wifi," +
            "  mobileData," +
            "  gps," +
            "  bluetooth," +
            "  usbStorage," +
            "  memoryTotal," +
            "  memoryAvailable" +
            ") VALUES (" +
            "  #{recordId}," +
            "  #{item.batteryLevel}," +
            "  #{item.batteryCharging}," +
            "  #{item.ip}," +
            "  #{item.keyguard}," +
            "  #{item.ringVolume}," +
            "  #{item.wifi}," +
            "  #{item.mobileData}," +
            "  #{item.gps}," +
            "  #{item.bluetooth}," +
            "  #{item.usbStorage}," +
            "  #{item.memoryTotal}," +
            "  #{item.memoryAvailable}" +
            ")")
    int insertDeviceInfoGroupDevice(@Param("recordId") Integer recordId, @Param("item") DeviceData device);

    @Insert("INSERT INTO plugin_deviceinfo_deviceparams_wifi (" +
            "  recordId," +
            "  rssi," +
            "  ssid," +
            "  security," +
            "  state," +
            "  ip," +
            "  tx," +
            "  rx" +
            ") VALUES (" +
            "  #{recordId}," +
            "  #{item.rssi}," +
            "  #{item.ssid}," +
            "  #{item.security}," +
            "  #{item.state}," +
            "  #{item.ip}," +
            "  #{item.tx}," +
            "  #{item.rx}" +
            ")")
    int insertDeviceInfoGroupWifi(@Param("recordId") Integer recordId, @Param("item") WifiData wifi);

    @Insert("INSERT INTO plugin_deviceinfo_deviceparams_gps (" +
            "  recordId," +
            "  state," +
            "  lat," +
            "  lon," +
            "  alt," +
            "  speed," +
            "  course" +
            ") VALUES (" +
            "  #{recordId}," +
            "  #{item.state}," +
            "  #{item.lat}," +
            "  #{item.lon}," +
            "  #{item.alt}," +
            "  #{item.speed}," +
            "  #{item.course}" +
            ")")
    int insertDeviceInfoGroupGps(@Param("recordId") Integer recordId, @Param("item") GpsData gps);

    @Insert("INSERT INTO plugin_deviceinfo_deviceparams_mobile (" +
            "  recordId," +
            "  rssi," +
            "  carrier," +
            "  data," +
            "  ip," +
            "  state," +
            "  simState," +
            "  tx," +
            "  rx" +
            ") VALUES (" +
            "  #{recordId}," +
            "  #{item.rssi}," +
            "  #{item.carrier}," +
            "  #{item.data}," +
            "  #{item.ip}," +
            "  #{item.state}," +
            "  #{item.simState}," +
            "  #{item.tx}," +
            "  #{item.rx}" +
            ")")
    int insertDeviceInfoGroupMobile(@Param("recordId") Integer recordId, @Param("item") MobileData mobile);

    @Insert("INSERT INTO plugin_deviceinfo_deviceparams_mobile2 (" +
            "  recordId," +
            "  rssi," +
            "  carrier," +
            "  data," +
            "  ip," +
            "  state," +
            "  simState," +
            "  tx," +
            "  rx" +
            ") VALUES (" +
            "  #{recordId}," +
            "  #{item.rssi}," +
            "  #{item.carrier}," +
            "  #{item.data}," +
            "  #{item.ip}," +
            "  #{item.state}," +
            "  #{item.simState}," +
            "  #{item.tx}," +
            "  #{item.rx}" +
            ")")
    int insertDeviceInfoGroupMobile2(@Param("recordId") Integer recordId, @Param("item") MobileData mobile);

    /**
     * <p>Deletes the device info records which are older than number of days configured in customer's profile.</p>
     *
     * @return a number of deleted records.
     */
    @Delete("DELETE FROM plugin_deviceinfo_deviceparams " +
            "WHERE ts < (SELECT UNIX_TIMESTAMP(DATE(NOW() - INTERVAL pds.dataPreservePeriod DAY)) * 1000 " +
            "            FROM plugin_deviceinfo_settings pds " +
            "            WHERE pds.customerId =  #{customerId})")
    int purgeDeviceInfoRecords(@Param("customerId") int customerId);

    @Select("SELECT " +
            "    d.id AS id, " +
            "    d.number AS deviceNumber, " +
            "    d.description AS description, " +
            "    d.lastUpdate AS latestUpdateTime, " +
            "    d.imei AS imeiRequired, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.imei')) AS imeiActual, " +
            "    d.phone AS phoneNumberRequired, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.phone')) AS phoneNumberActual, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.model')) AS model, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.androidVersion')) AS osVersion, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.batteryLevel')) AS batteryLevel, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.mdmMode')) AS mdmMode, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.kioskMode')) AS kioskMode, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.launcherType')) AS launcherType, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.launcherPackage')) AS launcherPackage, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.defaultLauncher')) AS defaultLauncher, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.phone2')) AS phone2, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.imei2')) AS imei2, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.iccid')) AS iccid, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.imsi')) AS imsi, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.iccid2')) AS iccid2, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.imsi2')) AS imsi2, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.serial')) AS serial, " +
            "    JSON_UNQUOTE(JSON_EXTRACT(d.infojson, '$.cpu')) AS cpu, " +
            "    COALESCE(JSON_EXTRACT(d.infojson, '$.permissions[0]'), FALSE) AS adminPermission, " +
            "    COALESCE(JSON_EXTRACT(d.infojson, '$.permissions[1]'), FALSE) AS overlapPermission, " +
            "    COALESCE(JSON_EXTRACT(d.infojson, '$.permissions[2]'), FALSE) AS historyPermission, " +
            "    COALESCE(JSON_EXTRACT(d.infojson, '$.permissions[3]'), FALSE) AS accessibilityPermission " +
            "FROM devices d " +
            "WHERE d.id = #{id}")
    DeviceInfo getDetailedDeviceInfo(@Param("id") int deviceId);


    @Select("SELECT " +
            "    groups.id AS id," +
            "    groups.name AS name " +
            "FROM devicegroups " +
            "INNER JOIN groups ON groups.id = devicegroups.groupId " +
            "WHERE devicegroups.deviceId = #{id} " +
            "ORDER BY groups.name ")
    List<LookupItem> getDeviceGroups(@Param("id") int deviceId);

    @Select("SELECT " +
            "    p.ts AS latestUpdateTime," +
            "" +
            "    main.batteryLevel AS deviceBatteryLevel," +
            "    main.batteryCharging AS deviceBatteryCharging," +
            "    main.ip AS deviceIpAddress," +
            "    main.keyguard AS deviceKeyguard," +
            "    main.ringVolume AS deviceRingVolume," +
            "    main.wifi AS deviceWifiEnabled," +
            "    main.mobileData AS deviceMobileDataEnabled," +
            "    main.gps AS deviceGpsEnabled," +
            "    main.bluetooth AS deviceBluetoothEnabled," +
            "    main.usbStorage AS deviceUsbEnabled," +
            "    main.memoryTotal AS deviceMemoryTotal," +
            "    main.memoryAvailable AS deviceMemoryAvailable," +
            "" +
            "    wifi.rssi AS wifiRssi," +
            "    wifi.ssid AS wifiSsid," +
            "    wifi.security AS wifiSecurity," +
            "    wifi.state AS wifiState," +
            "    wifi.ip AS wifiIpAddress," +
            "    wifi.tx AS wifiTx," +
            "    wifi.rx AS wifiRx," +
            "" +
            "    gps.state AS gpsState," +
            "    gps.lat AS gpsLat," +
            "    gps.lon AS gpsLon," +
            "    gps.alt AS gpsAlt," +
            "    gps.speed AS gpsSpeed," +
            "    gps.course AS gpsCourse," +
            "" +
            "    mobile.rssi AS mobile1Rssi," +
            "    mobile.carrier AS mobile1Carrier," +
            "    mobile.data AS mobile1DataEnabled," +
            "    mobile.ip AS mobile1IpAddress," +
            "    mobile.state AS mobile1State," +
            "    mobile.simState AS mobile1SimState," +
            "    mobile.tx AS mobile1Tx," +
            "    mobile.rx AS mobile1Rx," +
            "" +
            "    mobile2.rssi AS mobile2Rssi," +
            "    mobile2.carrier AS mobile2Carrier," +
            "    mobile2.data AS mobile2DataEnabled," +
            "    mobile2.ip AS mobile2IpAddress," +
            "    mobile2.state AS mobile2State," +
            "    mobile2.simState AS mobile2SimState," +
            "    mobile2.tx AS mobile2Tx," +
            "    mobile2.rx AS mobile2Rx" +
            " " +
            "FROM plugin_deviceinfo_deviceparams p " +
            "LEFT JOIN plugin_deviceinfo_deviceparams_device main ON main.recordId = p.id " +
            "LEFT JOIN plugin_deviceinfo_deviceparams_wifi wifi ON wifi.recordId = p.id " +
            "LEFT JOIN plugin_deviceinfo_deviceparams_gps gps ON gps.recordId = p.id " +
            "LEFT JOIN plugin_deviceinfo_deviceparams_mobile mobile ON mobile.recordId = p.id " +
            "LEFT JOIN plugin_deviceinfo_deviceparams_mobile2 mobile2 ON mobile2.recordId = p.id " +
            "WHERE p.deviceId = #{id} " +
            "AND p.ts = (SELECT MAX(p2.ts) FROM plugin_deviceinfo_deviceparams p2 WHERE p2.deviceId = #{id}) " +
            "LIMIT 1")
    DeviceDynamicInfoRecord getLatestDeviceDynamicInfo(@Param("id") int deviceId);

    List<DeviceDynamicInfoRecord> searchDynamicData(DynamicInfoFilter filter);

    Cursor<DeviceDynamicInfoRecord> searchDynamicDataForExport(DynamicInfoExportFilter filter);

    long countAllDynamicData(DynamicInfoFilter filter);
}
