'use strict';
(function() {
	/**
	 *
	 * @param $scope
	 * @param {NpcControllerStub} NpcControllerStub
	 * @constructor
	 */
	function NpcShopController($scope, NpcControllerStub) {
		function refresh() {
			NpcControllerStub
				.shopMenu()
				.success(function (data) {
					$scope.menu = data.menu;
					$scope.transporter = data.transporter;
				});
		}

		refresh();
	}

	/**
	 *
	 * @param $scope
	 * @param {NpcControllerStub} NpcControllerStub
	 * @constructor
	 */
	function DummyController($scope, NpcControllerStub) {
		$scope.transport = {};
		$scope.transport.ship = {};
		$scope.transport.ship.name = "Testschiff";
		$scope.transport.status = "Teststatus";
	}

	/**
	 *
	 * @param $scope
	 * @param {NpcControllerStub} NpcControllerStub
	 * @constructor
	 */
	function NpcLpController($scope, NpcControllerStub) {
		function refresh() {
			NpcControllerStub
				.lpMenu($scope.editUserId, $scope.alleMeldungenAnzeigen)
				.success(function (data) {
					$scope.menu = data.menu;
					$scope.edituser = data.user;
					$scope.edituserPresent = $scope.edituser != null;
					$scope.meldungen = data.meldungen;
					$scope.lpListe = data.lpListe;
					$scope.rang = data.rang;
					$scope.lpBeiNpc = data.lpBeiNpc;
					$scope.alleMeldungenAnzeigen = data.alleMeldungen;

					$scope.lpNeu = {
						grund: "",
						anmerkungen: "",
						punkte: "",
						pm: true
					};
				});
		}

		$scope.meldungBearbeitet = function (meldung) {
			NpcControllerStub.meldungBearbeitet(meldung.id)
				.success(function (data) {
					if (data.message.type === 'success') {
						refresh();
					}
				});
		};

		$scope.alleMeldungenChanged = function () {
			refresh();
		};

		$scope.changeUser = function () {
			refresh();
		};

		$scope.lpAendern = function () {
			NpcControllerStub.editLp(
				$scope.edituser.id,
				$scope.lpNeu.grund,
				$scope.lpNeu.anmerkungen,
				$scope.lpNeu.punkte,
				$scope.lpNeu.pm
			)
				.success(function (data) {
					if (data.message.type === 'success') {
						refresh();
					}
				});
		};

		$scope.lpLoeschen = function (lp) {
			NpcControllerStub.deleteLp($scope.edituser.id, lp.id)
				.success(function (data) {
					if (data.message.type === 'success') {
						refresh();
					}
				});
		};

		refresh();
	}

	/**
	 *
	 * @param $scope
	 * @param {NpcControllerStub} NpcControllerStub
	 * @param $routeParams
	 * @constructor
	 */
	function NpcRaengeController($scope, NpcControllerStub, $routeParams) {
		function refresh() {
			NpcControllerStub
				.raengeMenu($scope.editUserId)
				.success(function (data) {
					$scope.menu = data.menu;
					$scope.edituser = data.user;
					$scope.edituserPresent = $scope.edituser != null;
					$scope.raenge = data.raenge;
					$scope.medals = data.medals;
					$scope.medalSelected = -1;

					if ($scope.raenge) {
						$scope.aktiverRang = $scope.raenge[0];

						angular.forEach($scope.raenge, function (value) {
							if (value.id == data.aktiverRang) {
								$scope.aktiverRang = value;
							}
						});
					}
				});
		}

		$scope.changeUser = function () {
			refresh();
		};

		$scope.rangAendern = function () {
			NpcControllerStub.changeRank($scope.edituser.id, $scope.aktiverRang.id);
		};

		$scope.medailleVergeben = function () {
			if ($scope.medalSelected == -1) {
				$scope.message = 'Du musst einen Orden auswählen';
				return;
			}
			NpcControllerStub.awardMedal($scope.edituser.id, $scope.medalSelected, $scope.begruendung);
		};

		$scope.editUserId = $routeParams.userId;

		refresh();
	}

	/**
	 *
	 * @param $scope
	 * @param {NpcControllerStub} NpcControllerStub
	 * @constructor
	 */
	function NpcOrderController($scope, NpcControllerStub) {
		function refresh() {
			NpcControllerStub.orderMenu().success(function (data) {
				$scope.menu = data.menu;
				$scope.ships = data.ships;
				angular.forEach($scope.ships, function (ship) {
					ship.neworder = 0;
				});
				$scope.offiziere = data.offiziere;
				$scope.npcpunkte = data.npcpunkte;
				$scope.lieferpositionen = data.lieferpositionen;
				$scope.shipflags = {
					disableiff: false,
					nichtkaperbar: false,
					handelsposten: false
				};
				angular.forEach($scope.lieferpositionen, function (pos) {
					pos.label = pos.name + " (" + pos.pos + ")";
				});
				if (data.aktuelleLieferposition) {
					for (var i = 0; i < data.lieferpositionen.length; i++) {
						if (data.lieferpositionen[i].pos === data.aktuelleLieferposition) {
							$scope.lieferposition = data.lieferpositionen[i];
						}
					}
				}
			});
		}

		$scope.orderShips = function () {
			var params = {};
			angular.forEach($scope.ships, function (ship) {
				if (ship.neworder > 0) {
					params[ship.id] = ship.neworder;
				}
			});
			NpcControllerStub.orderShips($scope.shipflags.disableiff,$scope.shipflags.handelsposten,$scope.shipflags.nichtkaperbar,params)
				.success(function (data) {
					if (data.message.type === 'success') {
						refresh();
					}
				});
		};

		$scope.orderOffizier = function (offizier, count) {
			NpcControllerStub.order(offizier.id, count)
				.success(function (data) {
					if (data.message.type === 'success') {
						refresh();
					}
				});
		};

		$scope.lieferpositionAendern = function () {
			NpcControllerStub.changeOrderLocation($scope.lieferposition != null ? $scope.lieferposition.pos : "");
		};

		refresh();
	}

	angular.module('ds.npc', ['ds.service.ds', 'ds.service.ajax'])
		.controller('NpcShopController', ['$scope', 'NpcControllerStub', NpcShopController])
		.controller('DummyController', ['$scope', 'NpcControllerStub', DummyController])
		.controller('NpcLpController', ['$scope', 'NpcControllerStub', NpcLpController])
		.controller('NpcRaengeController', ['$scope', 'NpcControllerStub', '$routeParams', NpcRaengeController])
		.controller('NpcOrderController', ['$scope', 'NpcControllerStub', NpcOrderController]
	);
})();