/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.deviceexport.rest.json;

import java.util.ArrayList;
import java.util.List;

public class DeviceImportResult {

    private int created;
    private int updated;
    private int skipped;
    private List<String> errors;

    public DeviceImportResult() {
        this.errors = new ArrayList<>();
    }

    public int getCreated() { return created; }
    public void setCreated(int created) { this.created = created; }

    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public void addError(String error) { this.errors.add(error); }
    public void incrementCreated() { this.created++; }
    public void incrementUpdated() { this.updated++; }
    public void incrementSkipped() { this.skipped++; }
}
