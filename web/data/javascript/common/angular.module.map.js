'use strict';

angular.module('ds.map', ['ds.service.ds'])
	.factory('dsMap', ['ds', function(ds) {
		return {
			systemauswahl : function() {
				var options = {};
				options.module='map';
				options.action='systemauswahl';
				return ds(options);
			},
			wichtigeObjekte : function(systemId) {
				var options = {};
				options.module = 'impobjects';
				options.action = 'json';
				options.system = systemId;
				return ds(options);
			}
		};
	}])
	.factory('StarmapService', [function() {
		var starmap;

		return {
			create : function(targetSelector) {
				starmap = new Starmap($(targetSelector));
			},
			get : function() {
				return starmap;
			}
		};
	}])
	.controller('MapGeheZuPositionController', ['$scope', 'dsMap', 'PopupService', 'StarmapService', function($scope, dsMap, PopupService, StarmapService) {
		function zurPositionSpringen(objekt) {
			var x, y;
			if( objekt ) {
				x = objekt.x;
				y = objekt.y;
			}
			else {
				x = $scope.x;
				y = $scope.y;
			}
			StarmapService.get().gotoLocation(x,y);
		}

		function refresh() {
			if( !StarmapService.get().isReady() ) {
				return;
			}
			$scope.sprungpunkte = [];
			$scope.posten = [];
			$scope.basen = [];
			dsMap
				.wichtigeObjekte(StarmapService.get().getSystemId())
				.success(function(result) {
					$scope.sprungpunkte = result.jumpnodes;
					$scope.posten = result.posten;
					$scope.basen = result.bases;
				});

			$scope.x = 1;
			$scope.y = 1;
		}

		$scope.$on('dsPopupOpen', function(event, popup) {
			if( popup == $scope.dsPopupName ) {
				refresh();
			}
		});

		$scope.zurPositionSpringen = zurPositionSpringen;
	}])
	.controller('MapSystemuebersichtController', ['$scope', 'dsMap', 'PopupService', 'StarmapService', function($scope, dsMap, PopupService, StarmapService) {
		function wechselZuSystem(node) {
			var sys = node.id;

			PopupService.close($scope.dsPopupName);

			StarmapService.get().load(sys,1,1, {
				request : {
					admin : false
				}
			});
		}

		function refresh() {
			dsMap
				.systemauswahl()
				.success(function(data) {
					var sysGraph = {
						nodes: [],
						edges : []
					};

					var systeme = data.systeme;
					angular.forEach(systeme, function(system) {
						if( system.npcOnly || system.adminOnly ) {
							return;
						}
						sysGraph.nodes.push({
							id: system.id,
							label: system.name,
							allianz: system.allianz,
							basis: system.basis,
							schiff: system.schiff
						});

						angular.forEach(system.sprungpunkte, function(jn) {
							sysGraph.edges.push({
								source:jn.system,
								target:jn.systemout
							});
						});
					});

					$scope.sysGraph = sysGraph;
					$scope.wechselZuSystem = wechselZuSystem;
				});
		}

		$scope.$on('dsPopupOpen', function(event, popup) {
			if( popup == $scope.dsPopupName ) {
				refresh();
			}
		});
	}])
	.controller('MapSystemauswahlController', ['$scope', 'dsMap', 'PopupService', 'StarmapService', function($scope, dsMap, PopupService, StarmapService) {
		function refresh() {
			dsMap
				.systemauswahl()
				.success(function(data) {
					var systeme = data.systeme;
					angular.forEach(systeme, function(system) {
						if( system.id == data.system ) {
							$scope.systemSelected = system;
						}
						system.label = system.name+" ("+system.id+")"+system.addinfo;
					});
					$scope.systeme = systeme;
					$scope.adminSichtVerfuegbar = data.adminSichtVerfuegbar;
					$scope.locationX = 1;
					$scope.locationY = 1;
					$scope.adminSicht = false;
				});
		}

		function sternenkarteLaden() {
			var sys = $scope.systemSelected.id;
			var x = $scope.locationX;
			var y = $scope.locationY;
			var adminSicht = $scope.adminSicht ? 1 : 0;

			PopupService.close("systemSelection");

			StarmapService.get().load(sys,x,y, {
				request : {
					admin : adminSicht
				}
			});
		}

		refresh();

		$scope.sternenkarteLaden = sternenkarteLaden;
	}])
	.controller('MapController', ['$scope', 'dsMap', 'PopupService', 'StarmapService', function($scope, dsMap, PopupService, StarmapService) {
		function init() {
			$(document).ready(function() {
				StarmapService.create('#mapcontent');
			});
		}
		function showSystemSelection() {
			PopupService.open('systemSelection');
		};
		function showJumpToPosition() {
			if( !StarmapService.get().isReady() ) {
				return;
			}

			PopupService.open('gotoLocationPopup');
		}

		init();

		$scope.showJumpToPosition = showJumpToPosition;
		$scope.showSystemSelection = showSystemSelection;

	}]);
