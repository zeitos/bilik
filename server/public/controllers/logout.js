angular.module('Bilik')
    .controller('LogoutCtrl', function($auth, $alert, $location) {
        if (!$auth.isAuthenticated()) {
            return;
        }
        $auth.logout()
            .then(function() {
                $location.path("/#/login");
                $alert({
                    content: 'You have been logged out',
                    animation: 'fadeZoomFadeDown',
                    type: 'material',
                    duration: 3
                });
            });
    });