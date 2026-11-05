/*
 * SPDX-FileCopyrightText: 2024-2026 Medulla / Natsu, http://www.medulla-tech.io
 * SPDX-License-Identifier: GPL-3.0-or-later
*/

angular.module('plugin-remotecontrol', ['ngResource', 'ui.bootstrap', 'ui.router', 'ngTagsInput', 'ncy-angular-breadcrumb'])
    .config(function ($stateProvider) {
        try {
            $stateProvider.state('plugin-remotecontrol', {
                url: "/" + 'plugin-remotecontrol',
                templateUrl: 'app/components/main/view/content.html',
                controller: 'TabController',
                ncyBreadcrumb: {
                    label: '{{"breadcrumb.plugin.remotecontrol.main" | localize}}',
                },
                resolve: {
                    openTab: function () {
                        return 'plugin-remotecontrol';
                    }
                },
            });
        } catch (e) {
            console.log('An error when adding state plugin-remotecontrol', e);
        }

        try {
            $stateProvider.state('plugin-settings-remotecontrol', {
                url: "/" + 'plugin-settings-remotecontrol',
                templateUrl: 'app/components/main/view/content.html',
                controller: 'TabController',
                ncyBreadcrumb: {
                    label: '{{"breadcrumb.plugin.remotecontrol.settings" | localize}}',
                },
                resolve: {
                    openTab: function () {
                        return 'plugin-settings-remotecontrol';
                    }
                },
            });
        } catch (e) {
            console.log('An error when adding state plugin-settings-remotecontrol', e);
        }
    })
    .factory('pluginRemoteControlService', function ($resource) {
        return $resource('', {}, {
            getSettings: { url: 'rest/plugins/remotecontrol/private/settings', method: 'GET' },
            saveSettings: { url: 'rest/plugins/remotecontrol/private/settings', method: 'PUT' },
            startSession: { url: 'rest/plugins/remotecontrol/private/sessions/:deviceNumber/start', method: 'POST' },
            stopSession: { url: 'rest/plugins/remotecontrol/private/sessions/:sessionId/stop', method: 'POST' },
            getSession: { url: 'rest/plugins/remotecontrol/private/sessions/:sessionId', method: 'GET' },
        });
    })
    .controller('PluginRemoteControlSettingsController', function ($scope, $rootScope, localization, pluginRemoteControlService) {
        $scope.successMessage = undefined;
        $scope.errorMessage = undefined;

        $rootScope.settingsTabActive = true;
        $rootScope.pluginsTabActive = false;

        $scope.settings = {};

        pluginRemoteControlService.getSettings(function (response) {
            if (response.status === 'OK') {
                $scope.settings = response.data;
            } else {
                $scope.errorMessage = localization.localizeServerResponse(response);
            }
        });

        $scope.save = function () {
            $scope.successMessage = undefined;
            $scope.errorMessage = undefined;

            pluginRemoteControlService.saveSettings($scope.settings, function (response) {
                if (response.status === 'OK') {
                    $scope.successMessage = localization.localize('success.plugin.remotecontrol.settings.saved');
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            });
        };
    })
    .controller('PluginRemoteControlController', function ($scope, $rootScope, $timeout, localization, pluginRemoteControlService) {
        $scope.errorMessage = undefined;
        $scope.statusMessage = undefined;

        $rootScope.settingsTabActive = false;
        $rootScope.pluginsTabActive = true;

        $scope.deviceNumber = '';
        $scope.session = null;
        $scope.connected = false;
        $scope.connecting = false;

        var peerConnection = null;
        var webSocket = null;
        var dataChannel = null;

        $scope.startSession = function () {
            if (!$scope.deviceNumber || $scope.deviceNumber.trim() === '') {
                $scope.errorMessage = localization.localize('plugin.remotecontrol.error.no.device');
                return;
            }

            $scope.errorMessage = undefined;
            $scope.statusMessage = localization.localize('plugin.remotecontrol.status.connecting');
            $scope.connecting = true;

            pluginRemoteControlService.startSession({ deviceNumber: $scope.deviceNumber }, {}, function (response) {
                if (response.status === 'OK') {
                    $scope.session = response.data;
                    connectWebSocket(response.data);
                } else {
                    $scope.connecting = false;
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, function () {
                $scope.connecting = false;
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.stopSession = function () {
            if ($scope.session) {
                pluginRemoteControlService.stopSession({ sessionId: $scope.session.id }, {}, function () { });
            }
            cleanup();
            $scope.statusMessage = localization.localize('plugin.remotecontrol.status.disconnected');
        };

        function connectWebSocket(session) {
            var protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            var wsUrl = protocol + '//' + window.location.host +
                window.location.pathname.replace(/\/[^/]*$/, '') +
                '/ws/remotecontrol/' + session.id + '?token=' + encodeURIComponent(session.token);

            webSocket = new WebSocket(wsUrl);

            webSocket.onopen = function () {
                $scope.$apply(function () {
                    $scope.statusMessage = localization.localize('plugin.remotecontrol.status.waiting.device');
                });
            };

            webSocket.onmessage = function (event) {
                var msg;
                try {
                    msg = JSON.parse(event.data);
                } catch (e) {
                    return;
                }
                handleSignaling(msg);
            };

            webSocket.onclose = function () {
                $scope.$apply(function () {
                    if ($scope.connected || $scope.connecting) {
                        $scope.statusMessage = localization.localize('plugin.remotecontrol.status.disconnected');
                        cleanup();
                    }
                });
            };

            webSocket.onerror = function () {
                $scope.$apply(function () {
                    $scope.errorMessage = localization.localize('plugin.remotecontrol.error.websocket');
                    cleanup();
                });
            };
        }

        var pendingIceCandidates = [];

        function handleSignaling(msg) {
            if (msg.type === 'sdpOffer') {
                pendingIceCandidates = [];
                createPeerConnectionAndHandleOffer(msg.sdp);
            } else if (msg.type === 'iceCandidate') {
                if (peerConnection) {
                    var candidate = new RTCIceCandidate(msg.candidate);
                    peerConnection.addIceCandidate(candidate).catch(function (err) {
                        console.error('Error adding ICE candidate', err);
                    });
                } else {
                    pendingIceCandidates.push(msg.candidate);
                }
            }
        }

        function createPeerConnectionAndHandleOffer(sdp) {
            pluginRemoteControlService.getSettings(function (response) {
                var iceServers = [];
                if (response.status === 'OK' && response.data) {
                    if (response.data.stunUrl) {
                        iceServers.push({ urls: response.data.stunUrl });
                    }
                    if (response.data.turnUrl) {
                        iceServers.push({
                            urls: response.data.turnUrl,
                            username: response.data.turnUsername || '',
                            credential: response.data.turnPassword || ''
                        });
                    }
                }
                if (iceServers.length === 0) {
                    iceServers.push({ urls: 'stun:stun.l.google.com:19302' });
                }
                initPeerConnection(iceServers);

                var offer = new RTCSessionDescription({ type: 'offer', sdp: sdp });
                peerConnection.setRemoteDescription(offer).then(function () {
                    // Add any ICE candidates that arrived while we were setting up
                    pendingIceCandidates.forEach(function (c) {
                        peerConnection.addIceCandidate(new RTCIceCandidate(c)).catch(function (err) {
                            console.error('Error adding queued ICE candidate', err);
                        });
                    });
                    pendingIceCandidates = [];
                    return peerConnection.createAnswer();
                }).then(function (answer) {
                    return peerConnection.setLocalDescription(answer);
                }).then(function () {
                    sendSignaling({ type: 'sdpAnswer', sdp: peerConnection.localDescription.sdp });
                }).catch(function (err) {
                    console.error('Error handling SDP offer', err);
                    $scope.$apply(function () {
                        $scope.errorMessage = localization.localize('plugin.remotecontrol.error.webrtc');
                        cleanup();
                    });
                });
            });
        }

        function initPeerConnection(iceServers) {
            var config = { iceServers: iceServers };
            peerConnection = new RTCPeerConnection(config);

            peerConnection.onicecandidate = function (event) {
                if (event.candidate) {
                    sendSignaling({ type: 'iceCandidate', candidate: event.candidate });
                }
            };

            peerConnection.ontrack = function (event) {
                $scope.$apply(function () {
                    $scope.connected = true;
                    $scope.connecting = false;
                    $scope.statusMessage = localization.localize('plugin.remotecontrol.status.connected');
                });
                // Set srcObject after Angular has shown the video element
                $timeout(function () {
                    var videoElement = document.getElementById('rc-video');
                    if (videoElement) {
                        if (event.streams && event.streams[0]) {
                            videoElement.srcObject = event.streams[0];
                        } else {
                            if (!videoElement.srcObject) {
                                videoElement.srcObject = new MediaStream();
                            }
                            videoElement.srcObject.addTrack(event.track);
                        }
                        videoElement.play().catch(function (e) {
                            console.warn('RC video autoplay failed:', e);
                        });
                    }
                }, 0);
            };

            peerConnection.ondatachannel = function (event) {
                dataChannel = event.channel;
                dataChannel.onopen = function () {
                    console.log('Data channel opened');
                };
            };

            peerConnection.onconnectionstatechange = function () {
                if (peerConnection.connectionState === 'disconnected' ||
                    peerConnection.connectionState === 'failed' ||
                    peerConnection.connectionState === 'closed') {
                    $scope.$apply(function () {
                        $scope.statusMessage = localization.localize('plugin.remotecontrol.status.disconnected');
                        cleanup();
                    });
                }
            };
        }

        function sendSignaling(msg) {
            if (webSocket && webSocket.readyState === WebSocket.OPEN) {
                webSocket.send(JSON.stringify(msg));
            }
        }

        function sendInput(inputEvent) {
            if (dataChannel && dataChannel.readyState === 'open') {
                dataChannel.send(JSON.stringify(inputEvent));
            }
        }

        $scope.onVideoMouseDown = function ($event) {
            if (!$scope.connected) return;
            var coords = getVideoCoords($event);
            if (coords) {
                sendInput({ type: 'touch', action: 'down', x: coords.x, y: coords.y });
            }
        };

        $scope.onVideoMouseUp = function ($event) {
            if (!$scope.connected) return;
            var coords = getVideoCoords($event);
            if (coords) {
                sendInput({ type: 'touch', action: 'up', x: coords.x, y: coords.y });
            }
        };

        $scope.onVideoMouseMove = function ($event) {
            if (!$scope.connected || !$event.buttons) return;
            var coords = getVideoCoords($event);
            if (coords) {
                sendInput({ type: 'touch', action: 'move', x: coords.x, y: coords.y });
            }
        };

        $scope.sendBackKey = function () {
            sendInput({ type: 'key', keyCode: 4 });
        };

        $scope.sendHomeKey = function () {
            sendInput({ type: 'key', keyCode: 3 });
        };

        $scope.sendRecentKey = function () {
            sendInput({ type: 'key', keyCode: 187 });
        };

        function getVideoCoords($event) {
            var video = document.getElementById('rc-video');
            if (!video) return null;
            var rect = video.getBoundingClientRect();
            var x = ($event.clientX - rect.left) / rect.width;
            var y = ($event.clientY - rect.top) / rect.height;
            if (x < 0 || x > 1 || y < 0 || y > 1) return null;
            return { x: x, y: y };
        }

        function cleanup() {
            $scope.connected = false;
            $scope.connecting = false;
            if (dataChannel) {
                try { dataChannel.close(); } catch (e) { }
                dataChannel = null;
            }
            if (peerConnection) {
                try { peerConnection.close(); } catch (e) { }
                peerConnection = null;
            }
            if (webSocket) {
                try { webSocket.close(); } catch (e) { }
                webSocket = null;
            }
        }

        $scope.$on('$destroy', function () {
            if ($scope.session) {
                pluginRemoteControlService.stopSession({ sessionId: $scope.session.id }, {}, function () { });
            }
            cleanup();
        });
    })
    .run(function ($rootScope, $location, localization) {
        localization.loadPluginResourceBundles("remotecontrol");
    });
