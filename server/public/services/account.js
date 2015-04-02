angular.module('Bilik')
    .factory('Account', function($http, $auth) {
        return {
            getProfile: function() {
                return $http.get('/api/me');
            },
            getDevices: function() {
                return $http.get('/api/devices');
            },
            getSubscriptions: function() {
                return $http.get('/api/subscriptions');
            },
            updateSubscription: function(areaName, isSubscribed) {
                return $http.post('/api/updateSubscription', {areaName: areaName, isSubscribed: isSubscribed});
            },
            getDelegates: function() {
                return $http.get('/api/delegates');
            },
            inviteDelegate: function(email) {
                return $http.post('/api/inviteDelegate', {email: email});
            },
            removeDelegate: function(email) {
                return $http.post('/api/removeDelegate', {email: email});
            }
        };
    });