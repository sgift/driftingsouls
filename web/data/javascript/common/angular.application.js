'use strict';

angular.module('ds.application', 
	[
	'ds.npc',
	'ds.map',
	'ds.demo',
	'ds.directives',
	'ui',
	'jquery-ui.directives'
	]
)
.config(['$routeProvider', function($routeProvider) {
	$routeProvider
		.when('/demo', {templateUrl: 'data/cltemplates/demo/demo.html',   controller: 'DemoController'})
		.when('/npc/order', {templateUrl: 'data/cltemplates/npc/order.html',   controller: 'NpcOrderController'})
		.when('/npc/raenge', {templateUrl: 'data/cltemplates/npc/raenge.html', controller: 'NpcRaengeController'})
		.when('/npc/raenge/:userId', {templateUrl: 'data/cltemplates/npc/raenge.html', controller: 'NpcRaengeController'})
		.when('/npc/lp', {templateUrl: 'data/cltemplates/npc/lp.html', controller: 'NpcLpController'})
		.when('/npc/shop', {templateUrl: 'data/cltemplates/npc/shop.html', controller: 'NpcShopController'})
		.when('/map', {templateUrl: 'data/cltemplates/map.html', controller: 'MapController'})
		.otherwise({redirectTo: '/map'});
	
}]);