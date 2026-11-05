/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

angular.module('plugin-contacts', ['ngResource', 'ui.bootstrap', 'ui.router', 'ncy-angular-breadcrumb'])
    .run(function (localization) {
        localization.loadPluginResourceBundles("contacts");
    })
    .config(function ($stateProvider) {
        try {
            $stateProvider.state('plugin-contacts', {
                url: '/plugin-contacts',
                templateUrl: 'app/components/main/view/content.html',
                controller: 'TabController',
                ncyBreadcrumb: {label: 'Contacts Sync'},
                resolve: {openTab: function () { return 'plugin-contacts'; }}
            });
        } catch (e) {
            console.log('An error when adding state plugin-contacts', e);
        }
    })
    .factory('pluginContactsService', function ($resource) {
        return $resource('', {}, {
            getAllConfigurations: {url: 'rest/private/configurations/list', method: 'GET'},
            getConfig:           {url: 'rest/plugins/contacts/private/config/:configurationId', method: 'GET'},
            saveConfig:          {url: 'rest/plugins/contacts/private/config', method: 'PUT'}
        });
    })
    .controller('PluginContactsController', function ($scope, $rootScope, pluginContactsService) {

        $rootScope.settingsTabActive = false;
        $rootScope.pluginsTabActive = true;

        $scope.configurations = [];
        $scope.selectedConfigurationId = null;
        $scope.config = {};
        $scope.successMessage = null;
        $scope.errorMessage = null;
        $scope.loading = false;
        $scope.saving = false;

        // Load all configurations for the dropdown
        pluginContactsService.getAllConfigurations(function (response) {
            $scope.configurations = response.data || [];
        });

        $scope.onConfigurationSelected = function () {
            if (!$scope.selectedConfigurationId) { return; }
            $scope.loading = true;
            $scope.successMessage = null;
            $scope.errorMessage = null;
            pluginContactsService.getConfig({configurationId: $scope.selectedConfigurationId}, function (response) {
                $scope.loading = false;
                $scope.config = response.data || {};
                $scope.config.configurationId = parseInt($scope.selectedConfigurationId);
            }, function () {
                $scope.loading = false;
                $scope.errorMessage = 'Failed to load configuration settings.';
            });
        };

        $scope.save = function () {
            if (!$scope.selectedConfigurationId) { return; }
            $scope.saving = true;
            $scope.successMessage = null;
            $scope.errorMessage = null;
            $scope.config.configurationId = parseInt($scope.selectedConfigurationId);
            pluginContactsService.saveConfig($scope.config, function () {
                $scope.saving = false;
                $scope.successMessage = 'Settings saved successfully.';
            }, function () {
                $scope.saving = false;
                $scope.errorMessage = 'Failed to save settings.';
            });
        };
    });
