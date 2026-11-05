/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.photos.persistence.mapper;

import com.hmdm.plugins.photos.persistence.domain.PhotosPhotoRecord;
import com.hmdm.plugins.photos.persistence.domain.PhotosPluginSettings;
import com.hmdm.plugins.photos.rest.json.PhotosListFilter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;

import java.util.List;

public interface PhotosMapper {

    @Select("SELECT * FROM plugin_photos_settings WHERE customerId = #{customerId}")
    PhotosPluginSettings findSettings(@Param("customerId") int customerId);

    @Insert({
        "INSERT INTO plugin_photos_settings " +
        "(customerId, transferPhotos, imagePaths, excludePaths, includeStandardPaths, nonImageExtensions, " +
        "directoryStructure, fileNameTemplate, removeOldFiles, deleteImagesOnDevices, imageLocation, " +
        "disableLocationWarning, addLabel, labelColor, backgroundColor, transparency, labelText) VALUES " +
        "(#{customerId}, #{transferPhotos}, #{imagePaths}, #{excludePaths}, #{includeStandardPaths}, #{nonImageExtensions}, " +
        "#{directoryStructure}, #{fileNameTemplate}, #{removeOldFiles}, #{deleteImagesOnDevices}, #{imageLocation}, " +
        "#{disableLocationWarning}, #{addLabel}, #{labelColor}, #{backgroundColor}, #{transparency}, #{labelText}) " +
        "ON DUPLICATE KEY UPDATE " +
        "transferPhotos=VALUES(transferPhotos), imagePaths=VALUES(imagePaths), excludePaths=VALUES(excludePaths), " +
        "includeStandardPaths=VALUES(includeStandardPaths), nonImageExtensions=VALUES(nonImageExtensions), " +
        "directoryStructure=VALUES(directoryStructure), fileNameTemplate=VALUES(fileNameTemplate), " +
        "removeOldFiles=VALUES(removeOldFiles), deleteImagesOnDevices=VALUES(deleteImagesOnDevices), " +
        "imageLocation=VALUES(imageLocation), disableLocationWarning=VALUES(disableLocationWarning), " +
        "addLabel=VALUES(addLabel), labelColor=VALUES(labelColor), backgroundColor=VALUES(backgroundColor), " +
        "transparency=VALUES(transparency), labelText=VALUES(labelText)"
    })
    void saveSettings(PhotosPluginSettings settings);

    @Insert({
        "INSERT INTO plugin_photos_files (customerId, deviceNumber, serverPath, originalName, uploadTs, lat, lon, address) " +
        "VALUES (#{customerId}, #{deviceNumber}, #{serverPath}, #{originalName}, #{uploadTs}, #{lat}, #{lon}, #{address})"
    })
    @SelectKey(statement = "SELECT currval('plugin_photos_files_id_seq')",
            keyColumn = "id", keyProperty = "id", before = false, resultType = int.class)
    int insertPhotoRecord(PhotosPhotoRecord record);

    @Select("SELECT * FROM plugin_photos_files WHERE id = #{id} AND customerId = #{customerId}")
    PhotosPhotoRecord findPhotoById(@Param("id") int id, @Param("customerId") int customerId);

    @Delete("DELETE FROM plugin_photos_files WHERE id = #{id} AND customerId = #{customerId}")
    void deletePhotoRecord(@Param("id") int id, @Param("customerId") int customerId);

    @Delete("DELETE FROM plugin_photos_files WHERE uploadTs < #{cutoffTs} AND customerId = #{customerId}")
    void deleteOldPhotoRecords(@Param("cutoffTs") long cutoffTs, @Param("customerId") int customerId);

    List<PhotosPhotoRecord> findPhotos(PhotosListFilter filter);

    long countPhotos(PhotosListFilter filter);
}
