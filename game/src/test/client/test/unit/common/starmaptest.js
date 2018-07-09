'use strict';

describe('Starmap', function() {
	beforeEach(module('ds.starmap'));

	var scope, Starmap, fixture;

	beforeEach(inject(function($rootScope, _Starmap_) {
		scope = $rootScope.$new();
		Starmap = _Starmap_;

		fixture = setFixtures('<div id="map" style="width:200px;height:200px"/>')
	}));

	describe('gotoLocation', function() {
		var mapActionResult = {
			"system": {"id": 1, "width": 200, "height": 500},
			"size": {"minx": 1, "miny": 1, "maxx": 62, "maxy": 28},
			"locations": [
				{"x": 25, "y": 28, "scan": true, "bg": {"image": "data/starmap/jumpnode/jumpnode.png", "x": 0, "y": 0}, "scanner": -1, "battle": false, "roterAlarm": false}
			]
		};

		it('Bei einer exakt angegebenen Position sollte ueber dieser zentriert werden', function () {
			// setup
			var map = new Starmap($('#map'));
			var jsonSpy = spyOn($, 'getJSON');
			jsonSpy.andCallFake(function (url, data, callback) {
				callback(mapActionResult);
			});
			map.load(1, 2, 3);

			// run
			var gotoOptions = {};
			jsonSpy.andCallFake(function (url, data, callback) {
				gotoOptions = data;
				callback(mapActionResult);
			});
			runs(function () {
				map.gotoLocation(100, 200);
			});

			// assert
			waitsFor(function () {
				return $.getJSON.callCount === 2
			}, 'Asynchrones Reload der Sternenkarte', 1000);

			runs(function () {
				expect($.getJSON.callCount).toBe(2);
				expect(gotoOptions.xstart).not.toBeUndefined();
				expect(gotoOptions.ystart).not.toBeUndefined();
				expect(gotoOptions.xend).not.toBeUndefined();
				expect(gotoOptions.xend).not.toBeUndefined();
				var centerX = gotoOptions.xstart + Math.round((gotoOptions.xend - gotoOptions.xstart) / 2);
				expect(centerX).toBe(100);
				var centerY = gotoOptions.ystart + Math.round((gotoOptions.yend - gotoOptions.ystart) / 2);
				expect(centerY).toBe(200);
			});
		});

		it('Bei einer ungefaehr angegebenen Position sollte ueber dieser zentriert werden', function () {
			// setup
			var map = new Starmap($('#map'));
			var jsonSpy = spyOn($, 'getJSON');
			jsonSpy.andCallFake(function (url, data, callback) {
				callback(mapActionResult);
			});
			map.load(1, 2, 3);

			// run
			var gotoOptions = {};
			jsonSpy.andCallFake(function (url, data, callback) {
				gotoOptions = data;
				callback(mapActionResult);
			});
			runs(function () {
				map.gotoLocation('10x', '20x');
			});

			// assert
			waitsFor(function () {
				return $.getJSON.callCount === 2
			}, 'Asynchrones Reload der Sternenkarte', 1000);

			runs(function () {
				expect($.getJSON.callCount).toBe(2);
				expect(gotoOptions.xstart).not.toBeUndefined();
				expect(gotoOptions.ystart).not.toBeUndefined();
				expect(gotoOptions.xend).not.toBeUndefined();
				expect(gotoOptions.xend).not.toBeUndefined();
				var centerX = gotoOptions.xstart + Math.round((gotoOptions.xend - gotoOptions.xstart) / 2);
				expect(centerX).toBe(105);
				var centerY = gotoOptions.ystart + Math.round((gotoOptions.yend - gotoOptions.ystart) / 2);
				expect(centerY).toBe(205);
			});
		});
	});
});
