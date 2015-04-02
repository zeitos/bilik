angular.module('Bilik')
    .controller('HomeCtrl', function($scope, $auth, $alert, Account) {
        var errorHandling = function(error) {
            $auth.logout();
            $alert({
                content: error && error.message ? error.message : "A problem has happened. Please try again!",
                animation: 'fadeZoomFadeDown',
                type: 'material',
                duration: 3
            });
        };

        /**
         * Get user's profile information.
         */
        $scope.getProfile = function() {
            Account.getProfile()
                .success(function(data) {
                    $scope.user = data;
                })
                .error(errorHandling);
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
                            isSubscribed: false,
                            devices: _.sortBy(value, function(device) {
                                return device.resourceName + "-" + device.deviceId;
                            })
                        };
                    }), function(area) {
                        return area.name;
                    });
                })
                .error(errorHandling);
        };

        $scope.getDevicesByArea();

        /**
         * Get user's subscriptions
         */
        $scope.getSubscriptions = function() {
            Account.getSubscriptions()
                .success(function (subscriptions) {
                    $scope.subscriptionsByArea = subscriptions;
                })
                .error(errorHandling);
        }

        $scope.getSubscriptions();

        /**
         * React to changes in subscriptions
         * @param area to subscribe/un-subscribe
         */
        $scope.toggleSubscription = function(area) {
            Account.updateSubscription(area.name, !$scope.isSubscribed(area))
                .success(function (subscriptions) {
                    $scope.subscriptionsByArea = subscriptions;
                })
                .error(errorHandling);
        }

        /**
         * Determines if a given area is subscribed
         * @param area
         * @returns boolean
         */
        $scope.isSubscribed = function(area) {
            return _.has($scope.subscriptionsByArea, area.name) && $scope.subscriptionsByArea[area.name].isSubscribed;
        }
    });