angular.module('Bilik', ['ngResource', 'ngMessages', 'ui.router', 'mgcrea.ngStrap', 'satellizer'])
    .config(function($stateProvider, $urlRouterProvider, $authProvider) {
        $stateProvider
            .state('home', {
                url: '/',
                templateUrl: 'partials/home.html',
                controller: 'HomeCtrl'
            })
            .state('team', {
                url: '/team',
                templateUrl: 'partials/team.html',
                controller: 'TeamCtrl'
            })
            .state('login', {
                url: '/login',
                templateUrl: 'partials/login.html',
                controller: 'LoginCtrl'
            })
            .state('logout', {
                url: '/logout',
                template: null,
                controller: 'LogoutCtrl'
            });

        $urlRouterProvider.otherwise('/');

        $authProvider.google({
            clientId: '269809107413-9s8ljn9n7vq9ordoc6f1js6r8ffsr57e.apps.googleusercontent.com'
        });
    })
    .run(function($rootScope, $location, $state, $auth) {
        $rootScope.$on( '$stateChangeStart', function(e, toState, toParams, fromState, fromParams) {
            var isLogin = toState.name === "login";
            if(isLogin){
                return; // no need to redirect
            }
            if(!$auth.isAuthenticated()) {
                e.preventDefault(); // stop current execution
                $state.go('login'); // go to login
            }
        });
    });