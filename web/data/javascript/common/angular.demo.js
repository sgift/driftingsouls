'use strict';

angular.module('ds.demo', [])
.controller('DemoController', ['$scope', function($scope) {
}])
.controller('DemoPopupController', ['$scope', function($scope) {
	$scope.transport = {};
	$scope.transport.ship = {};
	$scope.transport.ship.name = "Testschiff";
	$scope.transport.status = "Teststatus";
	$scope.transport.assignment = "Testauftrag";
}]);
