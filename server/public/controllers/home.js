angular.module('Bilik')
    .controller('HomeCtrl', function($scope, $auth, $alert, Account) {
        /**
         * Get user's profile information.
         */
        $scope.getProfile = function() {
            Account.getProfile()
                .success(function(data) {
                    $scope.user = data;
                })
                .error(function(error) {
                    $auth.logout();
                    $alert({
                        content: error && error.message ? error.message : "A problem has happened. Please try again!",
                        animation: 'fadeZoomFadeDown',
                        type: 'material',
                        duration: 3
                    });
                });
        };

        $scope.getProfile();

        /**
         * Get user's devices
         */
        $scope.getDevicesByArea = function() {
            Account.getDevices()
                .success(function(devices) {
                    $scope.areas = _.sortBy(_.map(_.groupBy(_.map(devices, function(device) {
                        return {
                            resourceName: device.resourceName,
                            deviceId: device.deviceId,
                            resourceStatus: device.resourceStatus,
                            lastUpdate: device.lastUpdate,
                            battery: ("" + (device.batteryLevel * 100) + "% " + (device.batteryIsCharging ? "(charging)" : "(NOT CHARGING)")),
                            status: (device.status === "okay" ? "success" : device.status === "warning" ? "warning" : "danger"),
                            area: device.area
                        }
                    }), function(device) {
                        return device.area;
                    }), function(value, key) {
                        return {
                            name: key,
                            devices: _.sortBy(value, function(device) {
                                return device.resourceName + "-" + device.deviceId;
                            })
                        };
                    }), function(area) {
                        return area.name;
                    });
                })
                .error(function(error) {
                    $auth.logout();
                    $location.path("/#/login");
                    $alert({
                        content: error && error.message ? error.message : "A problem has happened. Please try again!",
                        animation: 'fadeZoomFadeDown',
                        type: 'material',
                        duration: 3
                    });
                });
        };

        $scope.getDevicesByArea();
    });