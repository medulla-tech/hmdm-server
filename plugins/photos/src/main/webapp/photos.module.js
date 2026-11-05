/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

// Localization completed
angular.module('plugin-photos', ['ngResource', 'ui.bootstrap', 'ui.router', 'ngTagsInput', 'ncy-angular-breadcrumb'])
    .config(function ($stateProvider) {
        try {
            $stateProvider.state('plugin-photos', {
                url: "/" + 'plugin-photos',
                templateUrl: 'app/components/main/view/content.html',
                controller: 'TabController',
                ncyBreadcrumb: {
                    label: '{{"breadcrumb.plugin.photos.main" | localize}}',
                },
                resolve: {
                    openTab: function () {
                        return 'plugin-photos';
                    }
                },
            });
        } catch (e) {
            console.log('An error when adding state ' + 'plugin-photos', e);
        }

        try {
            $stateProvider.state('plugin-settings-photos', {
                url: "/" + 'plugin-settings-photos',
                templateUrl: 'app/components/main/view/content.html',
                controller: 'TabController',
                ncyBreadcrumb: {
                    label: '{{"breadcrumb.plugin.photos.settings" | localize}}',
                },
                resolve: {
                    openTab: function () {
                        return 'plugin-settings-photos';
                    }
                },
            });
        } catch (e) {
            console.log('An error when adding state ' + 'plugin-settings-photos', e);
        }
    })
    .factory('pluginPhotosService', function ($resource) {
        return $resource('', {}, {
            getSettings: {url: 'rest/plugins/photos/private/settings', method: 'GET'},
            saveSettings: {url: 'rest/plugins/photos/private/settings', method: 'PUT'},
            listPhotos: {url: 'rest/plugins/photos/private/list', method: 'POST'},
            deletePhoto: {url: 'rest/plugins/photos/private/photo/:id', method: 'DELETE'},
        });
    })
    .controller('PluginPhotosSettingsController', function ($scope, $rootScope, localization, pluginPhotosService) {
        $scope.successMessage = undefined;
        $scope.errorMessage = undefined;

        $rootScope.settingsTabActive = true;
        $rootScope.pluginsTabActive = false;

        $scope.settings = {};

        pluginPhotosService.getSettings(function (response) {
            if (response.status === 'OK') {
                $scope.settings = response.data;
            } else {
                $scope.errorMessage = localization.localizeServerResponse(response);
            }
        });

        $scope.save = function () {
            $scope.successMessage = undefined;
            $scope.errorMessage = undefined;

            pluginPhotosService.saveSettings($scope.settings, function (response) {
                if (response.status === 'OK') {
                    $scope.successMessage = localization.localize('success.plugin.photos.settings.saved');
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            });
        };
    })
    .controller('PluginPhotosController', function ($scope, $rootScope, $window, localization, confirmModal, pluginPhotosService) {
        $scope.errorMessage = undefined;

        $rootScope.settingsTabActive = false;
        $rootScope.pluginsTabActive = true;

        $scope.paging = {
            pageNum: 1,
            pageSize: 50,
            totalItems: 0,
            deviceNumber: '',
            dateFrom: null,
            dateTo: null,
        };

        $scope.$watch('paging.pageNum', function () {
            $window.scrollTo(0, 0);
            loadData();
        });

        $scope.dateFormat = localization.localize('format.date.plugin.photos.datePicker');
        $scope.datePickerOptions = {'show-weeks': false};
        $scope.openDatePickers = {dateFrom: false, dateTo: false};

        $scope.openDateCalendar = function ($event, isStartDate) {
            $event.preventDefault();
            $event.stopPropagation();
            if (isStartDate) {
                $scope.openDatePickers.dateFrom = true;
            } else {
                $scope.openDatePickers.dateTo = true;
            }
        };

        $scope.search = function () {
            $scope.errorMessage = undefined;
            $scope.paging.pageNum = 1;
            loadData();
        };

        $scope.deletePhoto = function (photo) {
            var localizedText = localization.localize('plugin.photos.browse.question.delete');
            confirmModal.getUserConfirmation(localizedText, function () {
                pluginPhotosService.deletePhoto({id: photo.id}, function (response) {
                    if (response.status === 'OK') {
                        loadData();
                    } else {
                        $scope.errorMessage = localization.localizeServerResponse(response);
                    }
                });
            });
        };

        $scope.getPhotoUrl = function (photo) {
            return 'rest/plugins/photos/private/photo/' + photo.id + '/file';
        };

        $scope.formatDate = function (ts) {
            if (!ts) return '';
            return new Date(ts).toLocaleString();
        };

        var loading = false;
        var loadData = function () {
            $scope.errorMessage = undefined;

            if (loading) {
                return;
            }
            loading = true;

            var request = {
                deviceNumber: $scope.paging.deviceNumber,
                dateFrom: $scope.paging.dateFrom ? $scope.paging.dateFrom.getTime() : null,
                dateTo: $scope.paging.dateTo ? $scope.paging.dateTo.getTime() : null,
                pageNum: ($scope.paging.pageNum - 1) * $scope.paging.pageSize,
                pageSize: $scope.paging.pageSize,
            };

            pluginPhotosService.listPhotos(request, function (response) {
                loading = false;
                if (response.status === 'OK') {
                    $scope.photos = response.data.items;
                    $scope.paging.totalItems = response.data.totalItemsCount;
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, function () {
                loading = false;
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        loadData();
    })
    .run(function ($rootScope, $location, localization) {
        localization.loadPluginResourceBundles("photos");
    });
