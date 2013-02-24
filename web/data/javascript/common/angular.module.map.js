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
	.controller('MapSystemauswahlController', ['$scope', 'dsMap', 'PopupService', 'StarmapService', function($scope, dsMap, PopupService, StarmapService) {
		function __updateJumpnodeList(data)
		{
			var listEl = $('#jumpnodelist');
			listEl.children().remove();

			var jns = '<table class="datatable">';

			for( var i=0; i < data.jumpnodes.length; i++ )
			{
				var jn = data.jumpnodes[i];

				jns += '<tr>';
				jns += '<td><span class="nobr">'+jn.x+'/'+jn.y+'</span></td>';
				jns += '<td>nach</td>';
				jns += '<td><span class="nobr">'+jn.name+' ('+jn.systemout+')'+jn.blocked+'</span></td>';
				jns += '</tr>';
			}
			jns += '</table>';

			listEl.append(jns);
		}

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
				},
				loadCallback : function(data) {
					__updateJumpnodeList(data);
				}
			});
		}

		refresh();

		$scope.sternenkarteLaden = sternenkarteLaden;
	}])
	.controller('MapController', ['$scope', 'dsMap', 'PopupService', 'StarmapService', function($scope, dsMap, PopupService, StarmapService) {
		function init() {
			$(document).ready(function() {
				__createJumpnodePopup();
				StarmapService.create('#mapcontent');
			});
		}
		function showSystemSelection() {
			PopupService.open('systemSelection');
		};
		function __createJumpnodePopup() {
			if( $('#jumpnodebox').size() == 0 ) {
				$('body').append('<div id="jumpnodebox"><h3>Sprungpunkte</h3><div id="jumpnodelist"><span>Kein System ausgewählt.</span></div></div>');
				$('#jumpnodebox').dsBox({
					center:true,
					width:400,
					draggable:true
				});
			}
		};
		function showJumpnodeList() {
			$('#jumpnodebox').dsBox('show');
		};
		function showJumpToPosition() {
			if( !StarmapService.get().isReady() ) {
				return;
			}

			PopupService.open('gotoLocationPopup');
		}

		init();

		$scope.showJumpToPosition = showJumpToPosition;
		$scope.showJumpnodeList = showJumpnodeList;
		$scope.showSystemSelection = showSystemSelection;

	}]);
