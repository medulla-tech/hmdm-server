/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

angular.module('plugin-netfilter', ['ngResource', 'ui.bootstrap', 'ui.router', 'ngTagsInput', 'ncy-angular-breadcrumb'])
    .config(function ($stateProvider) {
        try {
            $stateProvider.state('plugin-netfilter', {
                url: '/plugin-netfilter',
                templateUrl: 'app/components/main/view/content.html',
                controller: 'TabController',
                ncyBreadcrumb: {
                    label: '{{"breadcrumb.plugin.netfilter.main" | localize}}'
                },
                resolve: {
                    openTab: function () { return 'plugin-netfilter'; }
                }
            });
        } catch (e) {
            console.log('An error when adding state plugin-netfilter', e);
        }
        try {
            $stateProvider.state('plugin-settings-netfilter', {
                url: '/plugin-settings-netfilter',
                templateUrl: 'app/components/main/view/content.html',
                controller: 'TabController',
                ncyBreadcrumb: {
                    label: '{{"breadcrumb.plugin.netfilter.settings" | localize}}'
                },
                resolve: {
                    openTab: function () { return 'plugin-settings-netfilter'; }
                }
            });
        } catch (e) {
            console.log('An error when adding state plugin-settings-netfilter', e);
        }
    })
    .run(function (localization) {
        localization.loadPluginResourceBundles('netfilter');
    })

;(function () {
    'use strict';

    angular.module('plugin-netfilter')
        .factory('netFilterService', netFilterService)
        .controller('NetFilterPluginTabController', NetFilterPluginTabController)
        .controller('NetFilterSettingsController', NetFilterSettingsController);

    netFilterService.$inject = ['$http'];
    function netFilterService($http) {
        return {
            // Legacy settings
            getSettings: function () {
                return $http.get('rest/plugins/netfilter/private/settings');
            },
            saveSettings: function (settings) {
                return $http.put('rest/plugins/netfilter/private/settings', settings);
            },

            // Profiles
            getProfiles: function () {
                return $http.get('rest/plugins/netfilter/private/profiles');
            },
            createProfile: function (profile) {
                return $http.post('rest/plugins/netfilter/private/profiles', profile);
            },
            updateProfile: function (id, profile) {
                return $http.put('rest/plugins/netfilter/private/profiles/' + id, profile);
            },
            deleteProfile: function (id) {
                return $http.delete('rest/plugins/netfilter/private/profiles/' + id);
            },

            // Profile rules
            getProfileRules: function (profileId) {
                return $http.get('rest/plugins/netfilter/private/profiles/' + profileId + '/rules');
            },
            addProfileRule: function (profileId, rule) {
                return $http.post('rest/plugins/netfilter/private/profiles/' + profileId + '/rules', rule);
            },
            updateProfileRule: function (profileId, ruleId, rule) {
                return $http.put('rest/plugins/netfilter/private/profiles/' + profileId + '/rules/' + ruleId, rule);
            },
            deleteProfileRule: function (profileId, ruleId) {
                return $http.delete('rest/plugins/netfilter/private/profiles/' + profileId + '/rules/' + ruleId);
            },

            // Profile configs
            getProfileConfigs: function (profileId) {
                return $http.get('rest/plugins/netfilter/private/profiles/' + profileId + '/configs');
            },
            addConfigToProfile: function (profileId, configurationId) {
                return $http.post('rest/plugins/netfilter/private/profiles/' + profileId + '/configs/' + configurationId);
            },
            removeConfigFromProfile: function (profileId, configurationId) {
                return $http.delete('rest/plugins/netfilter/private/profiles/' + profileId + '/configs/' + configurationId);
            },

            // HMDM configurations list (for the config picker)
            getConfigurations: function () {
                return $http.get('rest/private/configurations/list');
            }
        };
    }

    NetFilterPluginTabController.$inject = ['$scope', 'netFilterService', 'localization', 'confirmModal'];
    function NetFilterPluginTabController($scope, netFilterService, localization, confirmModal) {

        // ── State ──────────────────────────────────────────────────────────────
        $scope.profiles          = [];
        $scope.allConfigurations = [];
        $scope.editingProfile    = null;   // null = list view, object = edit view
        $scope.profileRules      = [];
        $scope.configSelection   = {};     // { configId: true/false }
        $scope.loading           = false;
        $scope.errorMessage      = '';
        $scope.successMessage    = '';

        $scope.showCreate  = false;
        $scope.newProfile  = { name: '', filterMode: 'BLOCKLIST' };
        $scope.newRule     = { domain: '' };

        // ── Helpers ───────────────────────────────────────────────────────────
        function clearMessages() {
            $scope.errorMessage   = '';
            $scope.successMessage = '';
        }

        function flashSuccess(key) {
            $scope.successMessage = localization.localize(key);
            setTimeout(function () { $scope.$apply(function () { $scope.successMessage = ''; }); }, 3000);
        }

        // ── Load data ─────────────────────────────────────────────────────────
        $scope.loadProfiles = function () {
            $scope.loading = true;
            netFilterService.getProfiles().then(function (response) {
                if (response.data.status === 'OK') {
                    $scope.profiles = response.data.data;
                } else {
                    $scope.errorMessage = response.data.message;
                }
            }).catch(function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            }).finally(function () { $scope.loading = false; });
        };

        $scope.loadAllConfigurations = function () {
            netFilterService.getConfigurations().then(function (response) {
                if (response.data.status === 'OK') {
                    $scope.allConfigurations = response.data.data;
                }
            }).catch(angular.noop);
        };

        // ── Create form ───────────────────────────────────────────────────────
        $scope.openCreate = function () {
            clearMessages();
            $scope.newProfile = { name: '', filterMode: 'BLOCKLIST' };
            $scope.showCreate = true;
        };

        $scope.cancelCreate = function () {
            $scope.showCreate = false;
        };

        $scope.submitCreate = function () {
            if (!$scope.newProfile.name || $scope.newProfile.name.trim() === '') {
                $scope.errorMessage = localization.localize('plugin.netfilter.error.name.required');
                return;
            }
            clearMessages();
            netFilterService.createProfile($scope.newProfile).then(function (response) {
                if (response.data.status === 'OK') {
                    $scope.showCreate = false;
                    $scope.loadProfiles();
                    flashSuccess('plugin.netfilter.profile.created');
                } else {
                    $scope.errorMessage = response.data.message;
                }
            }).catch(function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        // ── Edit view ─────────────────────────────────────────────────────────
        $scope.openEdit = function (profile) {
            clearMessages();
            $scope.editingProfile = angular.copy(profile);
            $scope.profileRules   = [];
            $scope.configSelection = {};

            netFilterService.getProfileRules(profile.id).then(function (response) {
                if (response.data.status === 'OK') {
                    $scope.profileRules = response.data.data;
                }
            }).catch(angular.noop);

            netFilterService.getProfileConfigs(profile.id).then(function (response) {
                if (response.data.status === 'OK') {
                    var ids = response.data.data;
                    // Build selection map from all configs
                    $scope.allConfigurations.forEach(function (c) {
                        $scope.configSelection[c.id] = ids.indexOf(c.id) !== -1;
                    });
                }
            }).catch(angular.noop);
        };

        $scope.cancelEdit = function () {
            $scope.editingProfile = null;
            $scope.loadProfiles();
        };

        $scope.saveProfile = function () {
            clearMessages();
            netFilterService.updateProfile($scope.editingProfile.id, $scope.editingProfile).then(function (response) {
                if (response.data.status === 'OK') {
                    flashSuccess('plugin.netfilter.profile.saved');
                } else {
                    $scope.errorMessage = response.data.message;
                }
            }).catch(function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        // ── Toggle enabled from list view ─────────────────────────────────────
        $scope.toggleProfileEnabled = function (profile) {
            netFilterService.updateProfile(profile.id, profile).then(function (response) {
                if (response.data.status !== 'OK') {
                    profile.enabled = !profile.enabled;
                    $scope.errorMessage = response.data.message;
                }
            }).catch(function () {
                profile.enabled = !profile.enabled;
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        // ── Delete profile ─────────────────────────────────────────────────────
        $scope.deleteProfile = function (profile) {
            confirmModal.getUserConfirmation(
                localization.localize('plugin.netfilter.profile.delete.confirm'),
                function () {
                    netFilterService.deleteProfile(profile.id).then(function (response) {
                        if (response.data.status === 'OK') {
                            var idx = $scope.profiles.indexOf(profile);
                            if (idx !== -1) { $scope.profiles.splice(idx, 1); }
                            if ($scope.editingProfile && $scope.editingProfile.id === profile.id) {
                                $scope.editingProfile = null;
                            }
                        } else {
                            $scope.errorMessage = response.data.message;
                        }
                    }).catch(function () {
                        $scope.errorMessage = localization.localize('error.request.failure');
                    });
                }
            );
        };

        // ── Rules (inside edit view) ──────────────────────────────────────────
        $scope.addProfileRule = function () {
            if (!$scope.newRule.domain || $scope.newRule.domain.trim() === '') {
                $scope.errorMessage = localization.localize('plugin.netfilter.error.domain.required');
                return;
            }
            clearMessages();
            // ruleType is inferred from the profile's filterMode
            var rule = {
                domain: $scope.newRule.domain,
                ruleType: $scope.editingProfile.filterMode === 'ALLOWLIST' ? 'ALLOW' : 'BLOCK'
            };
            netFilterService.addProfileRule($scope.editingProfile.id, rule).then(function (response) {
                if (response.data.status === 'OK') {
                    $scope.profileRules.push(response.data.data);
                    $scope.newRule = { domain: '' };
                } else {
                    $scope.errorMessage = response.data.message;
                }
            }).catch(function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.deleteProfileRule = function (rule) {
            netFilterService.deleteProfileRule($scope.editingProfile.id, rule.id).then(function (response) {
                if (response.data.status === 'OK') {
                    var idx = $scope.profileRules.indexOf(rule);
                    if (idx !== -1) { $scope.profileRules.splice(idx, 1); }
                } else {
                    $scope.errorMessage = response.data.message;
                }
            }).catch(function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        // ── Config checkboxes (inside edit view) ──────────────────────────────
        $scope.onConfigToggle = function (configId) {
            var checked = $scope.configSelection[configId];
            if (checked) {
                netFilterService.addConfigToProfile($scope.editingProfile.id, configId).then(function (response) {
                    if (response.data.status !== 'OK') {
                        $scope.configSelection[configId] = false;
                        $scope.errorMessage = response.data.message;
                    }
                }).catch(function () {
                    $scope.configSelection[configId] = false;
                    $scope.errorMessage = localization.localize('error.request.failure');
                });
            } else {
                netFilterService.removeConfigFromProfile($scope.editingProfile.id, configId).then(function (response) {
                    if (response.data.status !== 'OK') {
                        $scope.configSelection[configId] = true;
                        $scope.errorMessage = response.data.message;
                    }
                }).catch(function () {
                    $scope.configSelection[configId] = true;
                    $scope.errorMessage = localization.localize('error.request.failure');
                });
            }
        };

        // ── Init ──────────────────────────────────────────────────────────────
        $scope.loadProfiles();
        $scope.loadAllConfigurations();
    }

    NetFilterSettingsController.$inject = ['$scope', 'netFilterService', 'localization'];
    function NetFilterSettingsController($scope, netFilterService, localization) {

        $scope.settings = { enabled: false, filterMode: 'BLOCKLIST' };
        $scope.loading = false;
        $scope.errorMessage = '';
        $scope.successMessage = '';

        $scope.loadSettings = function () {
            $scope.loading = true;
            netFilterService.getSettings().then(function (response) {
                if (response.data.status === 'OK') {
                    $scope.settings = response.data.data;
                } else {
                    $scope.errorMessage = response.data.message;
                }
            }).catch(function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            }).finally(function () {
                $scope.loading = false;
            });
        };

        $scope.save = function () {
            $scope.errorMessage = '';
            netFilterService.saveSettings($scope.settings).then(function (response) {
                if (response.data.status === 'OK') {
                    $scope.successMessage = localization.localize('plugin.netfilter.settings.saved');
                    setTimeout(function () { $scope.$apply(function () { $scope.successMessage = ''; }); }, 3000);
                } else {
                    $scope.errorMessage = response.data.message;
                }
            }).catch(function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.loadSettings();
    }

})();
