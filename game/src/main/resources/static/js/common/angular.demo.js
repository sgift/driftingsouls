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

	$scope.sysGraph = {
		nodes: [
			{id:1,label:'Delta Serpentis'},
			{id:2,label:'Regulus'},
			{id:4,label:'Epsilon Pegasi'},
			{id:5,label:'Chyron'},
			{id:7,label:'Sigma Draconis'},
			{id:8,label:'Yalon Tarh'},
			{id:40,label:'Sh Donth'},
			{id:41,label:'Batra Karf'},
			{id:75,label:'Sherkat'}
		],
		edges : [
			{source:1,target:2},
			{source:2,target:1},
			{source:1,target:1},
			{source:2,target:4},
			{source:4,target:2},
			{source:1,target:5},
			{source:5,target:1},
			{source:5,target:8},
			{source:8,target:5},
			{source:1,target:4},
			{source:4,target:1},
			{source:8,target:7},
			{source:4,target:7},
			{source:7,target:4},
			{source:7,target:8},
			{source:75,target:7},
			{source:7,target:75},
			{source:40,target:41},
			{source:41,target:40},
			{source:41,target:75},
			{source:75,target:41}
		]
	};
}])
.controller('DemoPopupController', ['$scope', function($scope) {
	$scope.transport = {};
	$scope.transport.ship = {};
	$scope.transport.ship.name = "Testschiff";
	$scope.transport.status = "Teststatus";
	$scope.transport.assignment = "Testauftrag";
}]);
