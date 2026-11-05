/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

angular.module('plugin-deviceexport', ['ngResource', 'ui.bootstrap', 'ui.router', 'ncy-angular-breadcrumb'])
    .run(function (localization) {
        localization.loadPluginResourceBundles("deviceexport");
    })
    .config(function ($stateProvider) {
        try {
            $stateProvider.state('plugin-deviceexport', {
                url: '/plugin-deviceexport',
                templateUrl: 'app/components/main/view/content.html',
                controller: 'TabController',
                ncyBreadcrumb: {label: 'Device Export/Import'},
                resolve: {openTab: function () {return 'plugin-deviceexport';}}
            });
        } catch (e) {
            console.log('An error when adding state plugin-deviceexport', e);
        }
    })
    .directive('fileModel', function ($parse) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var model = $parse(attrs.fileModel);
                var setter = model.assign;
                element.bind('change', function () {
                    scope.$apply(function () {
                        setter(scope, element[0].files[0]);
                    });
                });
            }
        };
    })
    .controller('PluginDeviceExportController', function ($scope, $rootScope, $http, groupService, configurationService) {

        $rootScope.settingsTabActive = false;
        $rootScope.pluginsTabActive = true;

        // ---- column definitions ----
        $scope.allColumns = [
            {id: 'description',     label: 'Description',      selected: true,  readonly: false},
            {id: 'configuration',   label: 'Configuration',    selected: true,  readonly: false},
            {id: 'imei',            label: 'IMEI',             selected: true,  readonly: false},
            {id: 'phone',           label: 'Phone',            selected: true,  readonly: false},
            {id: 'serial',          label: 'Serial',           selected: true,  readonly: true},
            {id: 'androidVersion',  label: 'Android Version',  selected: false, readonly: true},
            {id: 'launcherVersion', label: 'Launcher Version', selected: false, readonly: true},
            {id: 'groups',          label: 'Groups',           selected: true,  readonly: false},
            {id: 'lastUpdate',      label: 'Last Online',      selected: true,  readonly: true},
            {id: 'enrollTime',      label: 'Enrollment Date',  selected: false, readonly: true},
            {id: 'publicIp',        label: 'Public IP',        selected: false, readonly: true},
            {id: 'custom1',         label: 'Custom 1',         selected: true,  readonly: false},
            {id: 'custom2',         label: 'Custom 2',         selected: false, readonly: false},
            {id: 'custom3',         label: 'Custom 3',         selected: false, readonly: false}
        ];

        // ---- export filters ----
        $scope.filter = {groupId: null, configurationId: null, text: ''};
        $scope.exportBusy = false;
        $scope.exportError = null;

        // ---- import state ----
        $scope.importFile = null;
        $scope.importBusy = false;
        $scope.importResult = null;
        $scope.importError = null;

        // ---- load dropdowns ----
        $scope.groups = [{id: null, name: '— All groups —'}];
        $scope.configurations = [{id: null, name: '— All configurations —'}];

        groupService.getAllGroups(function (response) {
            var data = (response && response.data) ? response.data : [];
            $scope.groups = [{id: null, name: '— All groups —'}].concat(data);
        });

        configurationService.getAllConfigNames(function (response) {
            var data = (response && response.data) ? response.data : [];
            $scope.configurations = [{id: null, name: '— All configurations —'}].concat(data);
        });

        // ---- column select/clear all ----
        $scope.selectAllColumns = function () {
            $scope.allColumns.forEach(function (col) { col.selected = true; });
        };

        $scope.clearAllColumns = function () {
            $scope.allColumns.forEach(function (col) { col.selected = false; });
        };

        // ---- export ----
        $scope.doExport = function () {
            var selectedCols = $scope.allColumns.filter(function (c) {return c.selected;}).map(function (c) {return c.id;});
            if (selectedCols.length === 0) {
                $scope.exportError = 'Please select at least one column.';
                return;
            }
            $scope.exportBusy = true;
            $scope.exportError = null;

            var req = {
                groupId:         $scope.filter.groupId || null,
                configurationId: $scope.filter.configurationId || null,
                filter:          $scope.filter.text || null,
                columns:         selectedCols
            };

            $http.post('rest/plugins/deviceexport/private/export', req, {responseType: 'arraybuffer'})
                .then(function (resp) {
                    $scope.exportBusy = false;
                    var blob = new Blob([resp.data], {type: 'text/csv;charset=utf-8'});
                    var url = URL.createObjectURL(blob);
                    var a = document.createElement('a');
                    a.href = url;
                    a.download = 'devices.csv';
                    document.body.appendChild(a);
                    a.click();
                    document.body.removeChild(a);
                    URL.revokeObjectURL(url);
                }, function () {
                    $scope.exportBusy = false;
                    $scope.exportError = 'Export failed. Please try again.';
                });
        };

        // ---- import ----
        $scope.doImport = function () {
            if (!$scope.importFile) {
                $scope.importError = 'Please select a CSV file first.';
                return;
            }
            $scope.importBusy = true;
            $scope.importResult = null;
            $scope.importError = null;

            var reader = new FileReader();
            reader.onload = function (e) {
                var content = e.target.result;
                $http.post('rest/plugins/deviceexport/private/import', content, {
                    headers: {'Content-Type': 'text/plain; charset=UTF-8'}
                }).then(function (resp) {
                    $scope.importBusy = false;
                    if (resp.data && resp.data.status === 'OK') {
                        $scope.importResult = resp.data.data;
                    } else {
                        $scope.importError = (resp.data && resp.data.message) ? resp.data.message : 'Import failed.';
                    }
                }, function (err) {
                    $scope.importBusy = false;
                    $scope.importError = 'Import request failed: ' + (err.statusText || 'server error');
                });
            };
            reader.onerror = function () {
                $scope.$apply(function () {
                    $scope.importBusy = false;
                    $scope.importError = 'Failed to read the selected file.';
                });
            };
            reader.readAsText($scope.importFile, 'UTF-8');
        };
    });
