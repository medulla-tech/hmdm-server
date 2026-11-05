/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.photos.persistence.domain;

public class PhotosPhotoRecord {

    private int id;
    private int customerId;
    private String deviceNumber;
    private String serverPath;
    private String originalName;
    private long uploadTs;
    private Double lat;
    private Double lon;
    private String address;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getDeviceNumber() { return deviceNumber; }
    public void setDeviceNumber(String deviceNumber) { this.deviceNumber = deviceNumber; }

    public String getServerPath() { return serverPath; }
    public void setServerPath(String serverPath) { this.serverPath = serverPath; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public long getUploadTs() { return uploadTs; }
    public void setUploadTs(long uploadTs) { this.uploadTs = uploadTs; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
