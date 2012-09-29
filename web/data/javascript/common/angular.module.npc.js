'use strict';

angular.module('ds.npc', ['ds.service.ds'])
.factory('dsNpc', ['ds', function(ds) {
	return {
		awardMedal : function(options) {
			options.module='npc';
			options.action='awardMedal';
			return ds(options);
		},
		changeRank : function(options) {
			options.module='npc';
			options.action='changeRank';
			return ds(options);
		},
		deleteLp : function(options) {
			options.module='npc';
			options.action='deleteLp';
			return ds(options);
		},
		editLp : function(options) {
			options.module='npc';
			options.action='editLp';
			return ds(options);
		},
		changeOrderLocation : function(options) {
			options.module='npc';
			options.action='changeOrderLocation';
			return ds(options);
		},
		orderShips : function(options) {
			options.module='npc';
			options.action='orderShips';
			return ds(options);
		},
		order : function(options) {
			options.module='npc';
			options.action='order';
			return ds(options);
		},
		shopMenu : function(options) {
			options.module='npc';
			options.action='shopMenu';
			return ds(options);
		},
		lpMenu : function(options) {
			options.module='npc';
			options.action='lpMenu';
			return ds(options);
		},
		raengeMenu : function(options) {
			options.module='npc';
			options.action='raengeMenu';
			return ds(options);
		},
		orderMenu : function(options) {
			options.module='npc';
			options.action='orderMenu';
			return ds(options);
		}
	};
}])
.controller('NpcShopController', ['$scope', 'dsNpc', function($scope, dsNpc) {
	function refresh() {
		dsNpc
		.shopMenu({edituser:$scope.editUserId})
		.success(function(data) {
			$scope.menu = data.menu;
			$scope.transporter = data.transporter;
		});
	}
	refresh();
}])
.controller('DummyController', ['$scope', 'dsNpc', function($scope, dsNpc) {
	$scope.transport = {};
	$scope.transport.ship = {};
	$scope.transport.ship.name = "Testschiff";
	$scope.transport.status = "Teststatus";
}])
.controller('NpcLpController', ['$scope', 'dsNpc', function($scope, dsNpc) {
	function refresh() {
		dsNpc
		.lpMenu({edituser:$scope.editUserId})
		.success(function(data) {
			$scope.menu = data.menu;
			$scope.edituser = data.user;
			$scope.edituserPresent =  $scope.edituser != null;
			$scope.lpListe = data.lpListe;
			$scope.rang = data.rang;
			$scope.lpBeiNpc = data.lpBeiNpc;
			
			$scope.lpNeu = {
				grund: "",
				anmerkungen: "",
				punkte: ""
			};
		});
	}
	
	$scope.changeUser = function() {
		refresh();
	}
	
	$scope.lpAendern = function() {
		dsNpc.editLp({
			edituser:$scope.edituser.id,
			grund:$scope.lpNeu.grund,
			anmerkungen:$scope.lpNeu.anmerkungen,
			punkte:$scope.lpNeu.punkte
		})
		.success(function(data) {
			if( data.message.type === 'success' ) {
				refresh();
			}
		});
	}
	
	$scope.lpLoeschen = function(lp) {
		dsNpc.deleteLp({
			edituser:$scope.edituser.id,
			lp:lp.id
		})
		.success(function(data) {
			if( data.message.type === 'success' ) {
				refresh();
			}
		});
	}
	
	refresh();
}])
.controller('NpcRaengeController', ['$scope', 'dsNpc', '$routeParams', function($scope, dsNpc, $routeParams) {
	function refresh() {
		dsNpc
		.raengeMenu({edituser:$scope.editUserId})
		.success(function(data) {
			$scope.menu = data.menu;
			$scope.edituser = data.user;
			$scope.edituserPresent =  $scope.edituser != null;
			$scope.raenge = data.raenge;
			$scope.medals = data.medals;
			$scope.medalSelected = -1;
			
			if( $scope.raenge ) {
				$scope.aktiverRang = $scope.raenge[0];
				
				angular.forEach($scope.raenge, function(value) {
					if( value.id == data.aktiverRang ) {
						$scope.aktiverRang = value;
					}
				});
			}
		});
	}
	
	$scope.changeUser = function() {
		refresh();
	}
	
	$scope.rangAendern = function() {
		dsNpc.changeRank({
			edituser:$scope.edituser.id, 
			rank:$scope.aktiverRang.id
		});
	}
	
	$scope.medailleVergeben = function() {
		if( $scope.medalSelected == -1 ) {
			$scope.message = 'Du musst einen Orden auswählen';
			return;
		}
		dsNpc.awardMedal({
			edituser:$scope.edituser.id,
			reason:$scope.begruendung,
			medal:$scope.medalSelected
		});
	}
	
	$scope.editUserId = $routeParams.userId;
	
	refresh();
}])
.controller('NpcOrderController', ['$scope','dsNpc',function($scope, dsNpc) {
	function refresh() {
		dsNpc.orderMenu({}).success(function(data) {
			$scope.menu = data.menu;
			$scope.ships = data.ships;
			angular.forEach($scope.ships, function(ship) {
				ship.neworder = 0;
			});
			$scope.offiziere = data.offiziere;
			$scope.npcpunkte = data.npcpunkte;
			$scope.lieferpositionen = data.lieferpositionen;
			$scope.shipflags = {
				disableiff:false,
				nichtkaperbar:false,
				handelsposten:false
			};
			angular.forEach($scope.lieferpositionen, function(pos) {
				pos.label = pos.name+" ("+pos.pos+")";
			});
			if( data.aktuelleLieferposition ) {
				for( var i=0; i < data.lieferpositionen.length; i++ ) {
					if( data.lieferpositionen[i].pos === data.aktuelleLieferposition ) {
						$scope.lieferposition = data.lieferpositionen[i];
					}
				}
			}
		});
	}
	
	$scope.orderShips = function() {
		var params = {};
		if( $scope.shipflags.disableiff ) {
			params.shipflag_disableiff = 1;
		}
		if( $scope.shipflags.nichtkaperbar ) {
			params.shipflag_nichtkaperbar=1;
		}
		if( $scope.shipflags.handelsposten ) {
			params.shipflag_handelsposten=1;
		}
		angular.forEach($scope.ships, function(ship) {
			if( ship.neworder > 0 ) {
				params["ship"+ship.id+"_count"]=ship.neworder;
			}
		});
		dsNpc.orderShips(params)
		.success(function(data) {
			if( data.message.type === 'success' ) {
				refresh();
			}
		});
	}
	
	$scope.orderOffizier = function(offizier, count) {
		dsNpc.order({order:offizier.id, count:count})
		.success(function(data) {
			if( data.message.type === 'success' ) {
				refresh();
			}
		});
	}
	
	$scope.lieferpositionAendern = function() {
		dsNpc.changeOrderLocation({
			lieferposition:$scope.lieferposition != null ? $scope.lieferposition.pos : ""
		});
	}
	
	refresh();
}]
);
