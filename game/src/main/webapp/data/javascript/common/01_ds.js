var DS = {
	location : {
		getCurrent : function() {
			return window.location.href;
		},
		setCurrent : function(url) {
			window.location.href = url;
		}
	},
	ln : function(number, decimals, decimal_sep, thousands_sep) {
		// siehe [http://stackoverflow.com/questions/149055/how-can-i-format-numbers-as-money-in-javascript]

		var n = number,
		   c = isNaN(decimals) ? 0 : Math.abs(decimals),
		   d = decimal_sep || ',',
		   t = (typeof thousands_sep === 'undefined') ? '.' : thousands_sep,
		   sign = (n < 0) ? '-' : '',
		   i = parseInt(n = Math.abs(n).toFixed(c)) + '',
		   j = ((j = i.length) > 3) ? j % 3 : 0;

		return sign +
		   	(j ? i.substr(0, j) + t : '') +
		   	i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) +
		   	(c ? d + Math.abs(n - i).toFixed(c).slice(2) : '');
	},
	/**
	 * Prueft, ob der angegebene Fehler anzeigt, dass der Benutzer momentan nicht eingeloggt ist.
	 * @param jsonResult Das JSON-Objekt mit dem Fehler
	 * @returns {boolean} true falls dem so ist
	 */
	istNichtEingeloggtFehler : function(jsonResult) {
		if( typeof jsonResult.message !== 'undefined' && jsonResult.message.type === 'error' ) {
			var msg = jsonResult.message;

			return msg.cls.indexOf('NotLoggedInException') > -1;
		}

		return false;
	},
	getJSON : function(params, resultFunction) {
		var url = this.getUrl();

		params.FORMAT = 'JSON';

		jQuery.getJSON( url, params, resultFunction);
	},
	get : function(params, resultFunction) {
		var url = this.getUrl();

		jQuery.get( url, params, resultFunction);
	},
	ask : function(text,url)
	{
		if( confirm(text) ) {
			DS.location.setCurrent(url);
		}
	},

	/**
	 * Gibt die URL zur aktuell laufenden DS-Version zurueck.
	 * Die URL endet immer mit /ds
	 * @returns {string} Die URL
	 */
	getUrl : function()
	{
		var url = DS.location.getCurrent();
		if( url.indexOf('?') > -1 )
		{
			url = url.substring(0,url.indexOf('?'));
		}
		if( url.indexOf('#') > -1 ) {
			url = url.substring(0,url.indexOf('#'));
		}
		if( url.indexOf('/ds',url.length-3) === -1 ) {
			url = url.substring(0,url.lastIndexOf('/'))+'/ds'
		}
		return url;
	},

	render : function(template, data, partials) {
		data.URL = this.getUrl();

		return Mustache.render(template, data, partials);
	},

	plot : function(id, data, options) {
		if( typeof options === 'undefined' ) {
			options = {};
		}
		if( typeof options.grid === 'undefined' ) {
			options.grid = {};
		}
		if( typeof options.axesDefaults === 'undefined' ) {
			options.axesDefaults = {};
		}
		if( typeof options.seriesColors === 'undefined' ) {
			options.seriesColors = [
					"#FEB626", "#c5b47f", "#FA9248", "#579575", "#A3B557", "#958c12",
					"#953579", "#4b5de4", "#d8b83f", "#ff5800", "#0085cc"];
		}

		options.grid.background='#0f1513';
		options.grid.borderColor='#243717';
		options.grid.gridLineColor='#243717';
		options.axesDefaults.labelRenderer = $.jqplot.CanvasAxisLabelRenderer;
		options.axesDefaults.labelOptions = {textColor:'#c7c7c7'};

		$.jqplot(id, data, options);
	}
};