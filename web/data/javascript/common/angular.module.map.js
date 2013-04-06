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
			},
			sektor : function(systemId, x, y, scanShip, adminSicht) {
				var options = {};
				options.module = 'map';
				options.action = 'sector';
				options.sys = systemId;
				options.x = x;
				options.y = y;
				options.scanship = scanShip;
				options.admin = adminSicht ? 1 : 0;
				return ds(options);
			}
		};
	}])
	.factory('StarmapService', ['PopupService', '$rootScope', function(PopupService, $rootScope) {
		var starmap;

		return {
			create : function(targetSelector) {
				starmap = new Starmap($(targetSelector));
			},
			load : function(sys, x, y, adminSicht) {
				starmap.load(sys,x,y, {
					request : {
						admin : adminSicht ? 1 : 0
					},
					onSectorClicked : function(system, x, y, locationinfo) {
						$rootScope.$apply(function() {
							if( locationinfo == null || !locationinfo.scanner ) {
								return;
							}
							PopupService.open('starmapSectorPopup', {parameters : {
								system:system,
								x:x,
								y:y,
								locationinfo:locationinfo,
								adminSicht : adminSicht
							}});
						});
					}
				});
			},
			get : function() {
				return starmap;
			}
		};
	}])
	.controller('MapSectorPopupController', ['$scope', 'dsMap', 'PopupService', 'StarmapService', function($scope, dsMap, PopupService, StarmapService) {
		function refresh(parameters) {
			if( !StarmapService.get().isReady() ) {
				return;
			}
			$scope.position = {
				system: parameters.system.id,
				x: parameters.x,
				y: parameters.y
			};
			$scope.geladen = false;

			dsMap
				.sektor(parameters.system.id, parameters.x, parameters.y, parameters.locationinfo.scanner, parameters.adminSicht)
				.success(function(result) {
					var sektor = {};
					sektor.nebel = result.nebel;
					sektor.jumpnodes = result.jumpnodes;
					sektor.bases = result.bases;
					sektor.battles = result.battles;
					sektor.users = result.users;
					$.each(sektor.users, function()
					{
						var shipcount = 0;
						$.each(this.shiptypes, function()
						{
							shipcount += this.ships.length;
						});
						this.shipcount = shipcount;
					});

					$scope.sektor = sektor;
					$scope.geladen = true;
				});
		}

		$scope.$on('dsPopupOpen', function(event, popup, parameters) {
			if( popup == $scope.dsPopupName ) {
				refresh(parameters);
			}
		});

		$scope.toggle = function(user) {
			user.expanded = !user.expanded;
		}
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

						var group = {
							id: 'none',
							styleClass: 'noGroup'
						};

						if( system.allianz != null ) {
							group = {
								id: system.allianz.id,
								styleClass: system.allianz.plainname.toLowerCase().replace(/[^a-zA-Z0-9]/g, '')
							};
						}

						sysGraph.nodes.push({
							id: system.id,
							label: system.name,
							allianz: system.allianz,
							basis: system.basis,
							schiff: system.schiff,
							group: group
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
			var adminSicht = $scope.adminSicht;

			PopupService.close("systemSelection");

			StarmapService.load(sys,x,y, adminSicht);
		}

		refresh();

		$scope.sternenkarteLaden = sternenkarteLaden;
	}])
	.controller('MapController', ['$scope', 'dsMap', 'PopupService', 'StarmapService', '$routeParams', function($scope, dsMap, PopupService, StarmapService, $routeParams) {
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

		if( $routeParams.system ) {
			var sys = $routeParams.system;
			var x = $routeParams.x;
			var y = $routeParams.y;
			var adminSicht = false;

			StarmapService.load(sys,x,y, adminSicht);
		}
		else {
			PopupService.open('systemSelection');
		}
	}]);
