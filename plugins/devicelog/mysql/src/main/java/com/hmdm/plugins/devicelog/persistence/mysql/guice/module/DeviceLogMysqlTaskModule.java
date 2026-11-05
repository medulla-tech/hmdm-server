/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

package com.hmdm.plugins.devicelog.persistence.mysql.guice.module;

import com.google.inject.Inject;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.devicelog.persistence.mysql.dao.MysqlDeviceLogDAO;
import com.hmdm.util.BackgroundTaskRunnerService;

import java.util.concurrent.TimeUnit;

/**
 * <p>A module used for initializing the tasks to be executed in background.</p>
 *
 * @author isv
 */
public class DeviceLogMysqlTaskModule implements PluginTaskModule {

    /**
     * <p>An interface to persistence layer.</p>
     */
    private final MysqlDeviceLogDAO deviceLogDAO;

    /**
     * <p>A runner for the repeatable tasks.</p>
     */
    private final BackgroundTaskRunnerService taskRunner;

    /**
     * <p>Constructs new <code>DeviceLogMysqlTaskModule</code> instance. This implementation does nothing.</p>
     */
    @Inject
    public DeviceLogMysqlTaskModule(MysqlDeviceLogDAO deviceLogDAO, BackgroundTaskRunnerService taskRunner) {
        this.deviceLogDAO = deviceLogDAO;
        this.taskRunner = taskRunner;
    }

    /**
     * <p>Initializes this module. Schedules the task for purging the outdated device log records from DB on a daily
     * basis.</p>
     */
    @Override
    public void init() {
        taskRunner.submitRepeatableTask(deviceLogDAO::purgeLogRecords, 1, 24, TimeUnit.HOURS);
    }
}
