'use strict';

angular.module('ds.map', ['ds.service.ds','ds.starmap'])
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
			},
			speichereSystemkarte: function(requestData) {
				requestData.module = 'map';
				requestData.action = 'speichereSystemkarte';
				return ds(requestData);
			}
		};
	}])
	.factory('StarmapService', ['PopupService', '$rootScope', 'Starmap', function(PopupService, $rootScope,  Starmap) {
		/**
		 * @type Starmap
		 */
		var starmap;

		return {
			create : function(targetSelector) {
				starmap = new Starmap($(targetSelector));
			},
			load : function(sys, x, y, adminSicht, sectorClickCallback) {
				starmap.load(sys,x,y, {
					request : {
						admin : adminSicht ? 1 : 0
					},
					onSectorClicked : function(system, x, y, locationinfo) {
						$rootScope.$apply(function() {
							sectorClickCallback(system, x, y, locationinfo);
						});
					},
					loadCallback: function() {
						if( x > 1 || y > 1 ) {
							starmap.highlight({x:x,y:y},"gotoLocation");
						}
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
					sektor.subraumspaltenCount = result.subraumspaltenCount;
					sektor.roterAlarm = result.roterAlarm
					$.each(sektor.users, function()
					{
						var shipcount = 0;
						$.each(this.shiptypes, function()
						{
							$.each(this.ships, function() {
								this.x = parameters.x;
								this.y = parameters.y;
							});
							shipcount += this.ships.length;
						});
						this.shipcount = shipcount;
					});

					$scope.sektor = sektor;
					$scope.geladen = true;
				});

			$scope.highlightSensoren = function(ship) {
				if( ship.sensorRange ) {
					StarmapService.get().highlight({x:parameters.x, y:parameters.y, size:ship.sensorRange}, "lrs", "lrs");
				}
			}
			$scope.unhighlightSensoren = function(ship) {
				StarmapService.get().unhighlightGroup("lrs");
			}
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
			StarmapService.get().unhighlightGroup('gotoLocation');
			StarmapService.get().highlight({x:x,y:y},'gotoLocation');
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
	.controller('MapSystemuebersichtController', ['$scope', 'dsMap', 'PopupService', '$location', function($scope, dsMap, PopupService, $location) {
		function wechselZuSystem(node) {
			var sys = node.id;

			PopupService.close($scope.dsPopupName);

			$location.path("/map/"+sys+"/1/1");
			/*StarmapService.get().load(sys,1,1, {
				request : {
					admin : false
				}
			});*/
		}

		function speichern() {
			var req = {};
			angular.forEach($scope.sysGraph.nodes, function(node) {
				if( node.moved && (node.posX || node.posY) ) {
					req['sys'+node.id+'x'] = Math.round(node.posX);
					req['sys'+node.id+'y'] = Math.round(node.posY);
				}
			});
			dsMap.speichereSystemkarte(req);
		}

		function refresh() {
			dsMap
				.systemauswahl()
				.success(function(data) {
					var sysGraph = {
						nodes: [],
						edges : []
					};

					var hasNpcSystems = false;
					var hasAdminSystems = false;

					var systeme = data.systeme;
					angular.forEach(systeme, function(system) {
						if( system.npcOnly ) {
							hasNpcSystems = true;
							if( !$scope.ansicht.npcSystemeAnzeigen ) {
								return;
							}
						}
						if( system.adminOnly ) {
							hasAdminSystems = true;
							if( !$scope.ansicht.adminSystemeAnzeigen ) {
								return;
							}
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
							group: group,
							posX: system.mapX,
							posY: system.mapY
						});

						angular.forEach(system.sprungpunkte, function(jn) {
							sysGraph.edges.push({
								source:jn.system,
								target:jn.systemout
							});
						});
					});

					var ansicht = $scope.ansicht;
					ansicht.systemkarteEditierbar = data.systemkarteEditierbar;
					ansicht.adminSysteme = hasAdminSystems;
					ansicht.npcSysteme = hasNpcSystems;
					$scope.ansicht = ansicht;

					$scope.sysGraph = sysGraph;
					$scope.wechselZuSystem = wechselZuSystem;

					if( data.systemkarteEditierbar ) {
						$scope.speichern = speichern;
					}
				});
		}

		var ansicht;
		if( $scope.ansicht ) {
			ansicht = $scope.ansicht;
		}
		else {
			ansicht = {adminSystemeAnzeigen: false, npcSystemeAnzeigen:false};
			ansicht.toggleNpcSystemeAnzeigen = function() {
				this.npcSystemeAnzeigen = !this.npcSystemeAnzeigen;
				refresh();
			};
			ansicht.toggleAdminSystemeAnzeigen = function() {
				this.adminSystemeAnzeigen = !this.adminSystemeAnzeigen;
				refresh();
			};
		}
		$scope.ansicht = ansicht;
		$scope.$on('dsPopupOpen', function(event, popup) {
			if( popup == $scope.dsPopupName ) {
				refresh();
			}
		});
	}])
	.controller('MapSystemauswahlController', ['$scope', 'dsMap', 'PopupService', '$location', function($scope, dsMap, PopupService, $location) {
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

			$location.path("/map/"+sys+"/"+x+"/"+y);
			$location.search("admin", adminSicht ? "true" : "false");
			//StarmapService.load(sys,x,y, adminSicht);
		}

		refresh();

		$scope.sternenkarteLaden = sternenkarteLaden;
	}])
	.controller('MapController', ['$scope', 'PopupService', 'StarmapService', '$routeParams', '$location', 'dsSchiff', function($scope, PopupService, StarmapService, $routeParams, $location, dsSchiff) {
		function init() {
			$(document).ready(function() {
				StarmapService.create('#mapcontent');
			});
		}

		function showSystemSelection() {
			PopupService.open('systemSelection');
		}

		function showJumpToPosition() {
			if( !StarmapService.get().isReady() ) {
				return;
			}

			PopupService.open('gotoLocationPopup');
		}

		init();

		$scope.showJumpToPosition = showJumpToPosition;
		$scope.showSystemSelection = showSystemSelection;
		$scope.log = [];

		$scope.fliegeMitSchiff = function (schiff) {
			if (!StarmapService.get().isReady()) {
				return;
			}
			var fokusaktion = {
				schiff: schiff,
				aktion: 'flug',
				ausfuehren : function() {
					var logentry = new Date().toLocaleTimeString()+": "+this.schiff.name+" fliegt nach "+this.ziel.x+"/"+this.ziel.y+"<br />";
					$scope.log.push({message: logentry});
					var idx = $scope.log.length-1;
					dsSchiff
						.fliegeSchiff(this.schiff.id, this.ziel.x, this.ziel.y)
						.success(function(result) {
							$scope.log[idx].message += result.log;
							StarmapService.get().refresh();
						});
				},
				zielMarkiert : function() {
					this.entfernung = Math.max(
						Math.abs(this.ziel.x - this.schiff.x),
						Math.abs(this.ziel.y - this.schiff.y)
					);
				}
			};
			if (schiff.fleet) {
				fokusaktion.flotte = schiff.fleet;
			}
			$scope.fokusaktion = fokusaktion;
		};

		$scope.kartenaktionAbbrechen = function(schiff) {
			$scope.fokusaktion = null;
		};

		$scope.kartenaktionBestaetigen = function() {
			if( $scope.fokusaktion.ausfuehren() !== true ) {
				$scope.fokusaktion = null;
				StarmapService.get().unhighlightGroup('fokusaktion');
			}
		};

		if( $routeParams.system ) {
			var sys = $routeParams.system;
			var x = $routeParams.x;
			var y = $routeParams.y;
			var adminSicht = $location.search().admin == "true";

			StarmapService.load(sys,x,y, adminSicht, function(system, x, y, locationinfo) {
				var fokusaktion = $scope.fokusaktion;
				if( fokusaktion == null || fokusaktion.aktion == null ) {
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
					return;
				}

				StarmapService.get().unhighlightGroup('fokusaktion');
				if( typeof(x) === 'number' && typeof(y) === 'number' ) {
					StarmapService.get().highlight({x:x,y:y}, 'fokusaktion');
					fokusaktion.ziel = {x: x, y: y};
					fokusaktion.zielMarkiert();
				}
			});
		}
		else {
			PopupService.open('systemSelection');
		}
	}]);
