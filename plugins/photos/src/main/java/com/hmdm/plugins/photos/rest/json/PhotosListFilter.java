/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.photos.rest.json;

public class PhotosListFilter {

    private int customerId;
    private String deviceNumber;
    private Long dateFrom;
    private Long dateTo;
    private int pageNum = 0;
    private int pageSize = 50;

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getDeviceNumber() { return deviceNumber; }
    public void setDeviceNumber(String deviceNumber) { this.deviceNumber = deviceNumber; }

    public Long getDateFrom() { return dateFrom; }
    public void setDateFrom(Long dateFrom) { this.dateFrom = dateFrom; }

    public Long getDateTo() { return dateTo; }
    public void setDateTo(Long dateTo) { this.dateTo = dateTo; }

    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
