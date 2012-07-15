function ask(text,url)
{
	if( confirm(text) ) {
		window.location.href = url;
	}
}

function getDsUrl()
{
	var url = location.href;
	if( url.indexOf('?') > -1 )
	{
		url = url.substring(0,url.indexOf('?'));
	}
	return url;
}

var DS = {
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
			window.location.href = url;
		}
	},

	getUrl : function()
	{
		var url = location.href;
		if( url.indexOf('?') > -1 )
		{
			url = url.substring(0,url.indexOf('?'));
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
					"#FEB626", "#c5b47f", "#EAA228", "#579575", "#839557", "#958c12",
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