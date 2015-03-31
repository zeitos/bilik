angular.module('Bilik')
    .factory('Account', function($http, $auth) {
        return {
            getProfile: function() {
                return $http.get('/api/me');
            },
            getDevices: function() {
                return $http.get('/api/devices');
            }
        };
    });