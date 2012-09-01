'use strict';

angular.module('ds.demo', [])
.controller('DemoController', ['$scope', function($scope) {
	$scope.generateNewLineChartData = function() {
		$scope.lineChartData = [
			{label: 'Foo', data:[[1,Math.random()*5],[2,Math.random()*3],[3,Math.random()*10]]},
			{label: 'bar', data:[[1,Math.random()*2],[2,Math.random()*10],[3,Math.random()*9]]}
		];
	}

	$scope.generateNewPieChartData = function() {
		$scope.pieChartData = [[
			['Deuteriumfässer', Math.round(Math.random()*500)],
			['Adamantiumsäcke',Math.round(Math.random()*300)],
			['Antimaterieballen',Math.round(Math.random()*100)],
			['Heiße Luft',Math.round(Math.random()*200)],
			['Kalte Luft',Math.round(Math.random()*100)],
			['Was auch immer',Math.round(Math.random()*900)]
		]];
	}
	
	$scope.generateNewLineChartData();
	$scope.generateNewPieChartData();
}])
.controller('DemoPopupController', ['$scope', function($scope) {
	$scope.transport = {};
	$scope.transport.ship = {};
	$scope.transport.ship.name = "Testschiff";
	$scope.transport.status = "Teststatus";
	$scope.transport.assignment = "Testauftrag";
}]);
