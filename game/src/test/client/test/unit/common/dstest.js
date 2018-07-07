'use strict';

describe('DS', function() {
	it('DS.getUrl bei einer ds-URL ohne Parameter sollte genau diese zurueckgeben', function() {
		spyOn(DS.location, 'getCurrent').andReturn("http://localhost/path/to/ds");

		var url = DS.getUrl();
		expect(url).toBe("http://localhost/path/to/ds");
	});

	it('DS.getUrl bei einer ds-URL mit Parameter sollte diese ohne Parameter zurueckgeben', function() {
		spyOn(DS.location, 'getCurrent').andReturn("http://localhost/path/to/ds?module=foo&action=bar");

		var url = DS.getUrl();
		expect(url).toBe("http://localhost/path/to/ds");
	});

	it('DS.getUrl bei einer modul-URL mit Parameter sollte diese als ds-URL ohne Parameter zurueckgeben', function() {
		spyOn(DS.location, 'getCurrent').andReturn("http://localhost/path/to/client?param1=foo&action=bar");

		var url = DS.getUrl();
		expect(url).toBe("http://localhost/path/to/ds");
	});

	it('DS.istNichtEingeloggtFehler sollte bei einem normalen Fehler false zurueckgeben', function() {
		var error = {
			message: {type:'errorlist'},
			errors: [{description:'Mist'}]
		};
		var result = DS.istNichtEingeloggtFehler(error);
		expect(result).not.toBeTruthy();
	});

	it('DS.istNichtEingeloggtFehler sollte bei einer Nicht Eingeloggt-Exception true zurueckgeben', function() {
		var error = {
			message: {
				type:'error',
				description:'Du musst eingeloggt sein, um diese Seite zu sehen.',
				cls:'net.driftingsouls.ds2.server.framework.NotLoggedInException'
			}
		};
		var result = DS.istNichtEingeloggtFehler(error);
		expect(result).toBeTruthy();
	});

	it('DS.istNichtEingeloggtFehler sollte bei einer normalen Exception false zurueckgeben', function() {
		var error = {
			message: {type:'error', description:'NullPointer', cls:'java.lang.NullPointerException'}
		};
		var result = DS.istNichtEingeloggtFehler(error);
		expect(result).not.toBeTruthy();
	});

	it('DS.ln sollte eine Zahl ohne Nachkommastellen korrekt formatieren', function() {
		var number = 123;
		var result = DS.ln(number);
		expect(result).toBe("123");
	});

	it('DS.ln sollte eine Zahl mit Nachkommastellen korrekt formatieren', function() {
		var number = 123.45678;
		var result = DS.ln(number,5);
		expect(result).toBe("123,45678");
	});

	it('DS.ln sollte eine vierstellige Zahl ohne Nachkommastellen korrekt mit Tausenderpunkt formatieren', function() {
		var number = 1234;
		var result = DS.ln(number);
		expect(result).toBe("1.234");
	});

	it('DS.ln sollte eine vierstellige Zahl mit Nachkommastellen korrekt mit Tausenderpunkt formatieren', function() {
		var number = 1123.45678;
		var result = DS.ln(number,5);
		expect(result).toBe("1.123,45678");
	});

	it('DS.ln sollte eine 10-stellige Zahl mit Nachkommastellen korrekt mit Tausenderpunkten formatieren', function() {
		var number = 1234567890.45678;
		var result = DS.ln(number,5);
		expect(result).toBe("1.234.567.890,45678");
	});

	it('DS.ln sollte eine Zahl mit Nachkommastellen aber 0 konfigurierten gerundet formatieren', function() {
		var number = 123.55678;
		var result = DS.ln(number,0);
		expect(result).toBe("124");
	});

	it('DS.ln sollte eine Zahl mit Nachkommastellen aber ohne konfigurierte gerundet formatieren', function() {
		var number = 123.55678;
		var result = DS.ln(number,0);
		expect(result).toBe("124");
	});

	it('DS.ln sollte eine Zahl mit Nachkommastellen aber mit mehr konfigurierten mit 0 auffuellend formatieren', function() {
		var number = 123.5678;
		var result = DS.ln(number,10);
		expect(result).toBe("123,5678000000");
	});

	it('DS.ln sollte eine Zahl mit 4 Stellen mit dem angegebenen Tausenderseparator formatieren', function() {
		var number = 1234;
		var result = DS.ln(number, 0, ',', '#');
		expect(result).toBe("1#234");
	});

	it('DS.ln sollte eine Zahl mit Nachkommastellen mit dem angegebenen Dezimaltrenner formatieren', function() {
		var number = 123.4;
		var result = DS.ln(number, 1, '#');
		expect(result).toBe("123#4");
	});

	it('DS.ln sollte eine Zahl mit den angegebenen Trennern formatieren', function() {
		var number = 1234;
		var result = DS.ln(number, 1, '#', '/');
		expect(result).toBe("1/234#0");
	});
});