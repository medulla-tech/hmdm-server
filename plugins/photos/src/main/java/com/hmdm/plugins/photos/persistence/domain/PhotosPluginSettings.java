/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.photos.persistence.domain;

public class PhotosPluginSettings {

    private int id;
    private int customerId;
    private boolean transferPhotos;
    private String imagePaths;
    private String excludePaths;
    private boolean includeStandardPaths;
    private String nonImageExtensions;
    private String directoryStructure;
    private String fileNameTemplate;
    private int removeOldFiles;
    private String deleteImagesOnDevices;
    private boolean imageLocation;
    private String disableLocationWarning;
    private boolean addLabel;
    private String labelColor;
    private String backgroundColor;
    private int transparency;
    private String labelText;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public boolean isTransferPhotos() { return transferPhotos; }
    public void setTransferPhotos(boolean transferPhotos) { this.transferPhotos = transferPhotos; }

    public String getImagePaths() { return imagePaths; }
    public void setImagePaths(String imagePaths) { this.imagePaths = imagePaths; }

    public String getExcludePaths() { return excludePaths; }
    public void setExcludePaths(String excludePaths) { this.excludePaths = excludePaths; }

    public boolean isIncludeStandardPaths() { return includeStandardPaths; }
    public void setIncludeStandardPaths(boolean includeStandardPaths) { this.includeStandardPaths = includeStandardPaths; }

    public String getNonImageExtensions() { return nonImageExtensions; }
    public void setNonImageExtensions(String nonImageExtensions) { this.nonImageExtensions = nonImageExtensions; }

    public String getDirectoryStructure() { return directoryStructure; }
    public void setDirectoryStructure(String directoryStructure) { this.directoryStructure = directoryStructure; }

    public String getFileNameTemplate() { return fileNameTemplate; }
    public void setFileNameTemplate(String fileNameTemplate) { this.fileNameTemplate = fileNameTemplate; }

    public int getRemoveOldFiles() { return removeOldFiles; }
    public void setRemoveOldFiles(int removeOldFiles) { this.removeOldFiles = removeOldFiles; }

    public String getDeleteImagesOnDevices() { return deleteImagesOnDevices; }
    public void setDeleteImagesOnDevices(String deleteImagesOnDevices) { this.deleteImagesOnDevices = deleteImagesOnDevices; }

    public boolean isImageLocation() { return imageLocation; }
    public void setImageLocation(boolean imageLocation) { this.imageLocation = imageLocation; }

    public String getDisableLocationWarning() { return disableLocationWarning; }
    public void setDisableLocationWarning(String disableLocationWarning) { this.disableLocationWarning = disableLocationWarning; }

    public boolean isAddLabel() { return addLabel; }
    public void setAddLabel(boolean addLabel) { this.addLabel = addLabel; }

    public String getLabelColor() { return labelColor; }
    public void setLabelColor(String labelColor) { this.labelColor = labelColor; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    public int getTransparency() { return transparency; }
    public void setTransparency(int transparency) { this.transparency = transparency; }

    public String getLabelText() { return labelText; }
    public void setLabelText(String labelText) { this.labelText = labelText; }
}
