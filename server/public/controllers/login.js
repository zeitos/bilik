angular.module('Bilik')
    .controller('LoginCtrl', function($scope, $alert, $auth) {
        $scope.authenticate = function(provider) {
            $auth.authenticate(provider)
                .then(function() {
                    $alert({
                        content: 'You have successfully logged in',
                        animation: 'fadeZoomFadeDown',
                        type: 'material',
                        duration: 3
                    });
                })
                .catch(function(response) {
                    $alert({
                        content: response.data.message || "Something went wrong. Please try again!",
                        animation: 'fadeZoomFadeDown',
                        type: 'material',
                        duration: 3
                    });
                });
        };
    });