angular.module('Bilik')
    .controller('TeamCtrl', function($scope, $auth, $alert, Account) {
        var errorHandling = function (error) {
            $auth.logout();
            $alert({
                content: error && error.message ? error.message : "A problem has happened. Please try again!",
                animation: 'fadeZoomFadeDown',
                type: 'material',
                duration: 3
            });
        };

        $scope.getDelegates = function() {
            Account.getDelegates()
                .success(function(data) {
                    $scope.delegates = data;
                })
                .error(errorHandling);
        };

        $scope.getDelegates();

        $scope.inviteDelegate = function() {
            if (!$scope.email) {
                $scope.error = "Missing email address";
            } else {
                var re = /^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/i;
                if (!re.test($scope.email)) {
                    $scope.error = "Invalid email address";
                } else {
                    Account.inviteDelegate($scope.email)
                        .success(function() {
                            $scope.email = "";
                            $scope.error = "";
                            $scope.message = "Invitation sent!";
                            $scope.getDelegates();
                        })
                        .error(errorHandling);
                }
            }
        };

        $scope.removeDelegate = function(delegate) {
            Account.removeDelegate(delegate.email)
                .success(function() {
                    $scope.getDelegates();
                })
                .error(errorHandling);
        }
    });