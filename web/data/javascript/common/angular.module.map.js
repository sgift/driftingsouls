'use strict';

angular.module('ds.map', ['ds.service.ds'])
.factory('dsMap', ['ds', function(ds) {
	return {
		systemauswahl : function() {
			var options = {};
			options.module='map';
			options.action='systemauswahl';
			return ds(options);
		}
	};
}])
.controller('MapController', ['$scope', 'dsMap', 'PopupService', function($scope, dsMap, PopupService) {
	var __starmap = null;
	function init() {
		$(document).ready(function() {
			__createJumpnodePopup();
			__starmap = new Starmap($('#mapcontent'));
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
		if( !__starmap.isReady() ) {
			return;
		}

		new StarmapGotoLocationPopup(function(x, y) {
			if( !x || !y ) {
				return;
			}
			x = parseInt(x);
			y = parseInt(y);
			__starmap.gotoLocation(x,y);
		});
	}

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

		__starmap.load(sys,x,y, {
			request : {
				admin : adminSicht
			},
			loadCallback : function(data) {
				__updateJumpnodeList(data);
			}
		});
	}

	init();
	refresh();

	$scope.showJumpToPosition = showJumpToPosition;
	$scope.showJumpnodeList = showJumpnodeList;
	$scope.showSystemSelection = showSystemSelection;
	$scope.sternenkarteLaden = sternenkarteLaden;
}]);
