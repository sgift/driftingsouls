(function() {
	'use strict';

	function TechListeController($scope, TechListeControllerStub) {
		function refresh(rasseId) {
			TechListeControllerStub
				.defaultAction(rasseId)
				.success(function(data) {
					$scope.viewModel = {
						rassenName: data.rassenName,
						auswaehlbareRassen: data.auswaehlbareRassen,
						erforschbar: data.erforschbar,
						erforscht: data.erforscht,
						nichtErforscht: data.nichtErforscht,
						unsichtbar: data.unsichtbar
					};
				});
		}

		$scope.wechselZuRasse = function(rasseId) {
			refresh(rasseId);
		};

		refresh(null);
	}

	angular.module('ds.module.techliste', ['ds.service.ajax'])
		.controller('TechListeController', ['$scope', 'TechListeControllerStub', TechListeController]);
}());