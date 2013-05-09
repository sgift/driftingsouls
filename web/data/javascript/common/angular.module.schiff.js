'use strict';

angular.module('ds.schiff', ['ds.service.ds'])
	.factory('dsSchiff', ['ds', function(ds) {
		return {
			fliegeSchiff : function(schiffId, x, y) {
				var options = {};
				options.module='schiffAjax';
				options.action='fliegeSchiff';
				options.schiff = schiffId;
				options.x = x;
				options.y = y;
				return ds(options);
			},
			alarmstufeAendern : function(schiffId, alarmstufe) {
				var options = {};
				options.module='schiffAjax';
				options.action='alarm';
				options.schiff = schiffId;
				options.alarm = alarmstufe;
				return ds(options);
			},
			springen : function(schiffId, sprungpunkt) {
				var options = {};
				options.module='schiffAjax';
				options.action='springen';
				options.schiff = schiffId;
				options.sprungpunkt = sprungpunkt;
				return ds(options);
			},
			springenViaSchiff : function(schiffId, sprungpunktSchiffId) {
				var options = {};
				options.module='schiffAjax';
				options.action='springenViaSchiff';
				options.schiff = schiffId;
				options.sprungpunktSchiff = sprungpunktSchiffId;
				return ds(options);
			}
		};
	}]);