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

(function( $ ) {
	var methods = {
		init : function(options) {
			var content = this.html();
			this.empty();

			this.addClass('gfxbox');
			this.addClass('popupbox');
			this.append("<div class='content'></div><button class='closebox'>schließen</button>");
			var contentEl = this.find('.content');
			contentEl.append(content);

			var self = this;
			this.find('button.closebox').bind('click.dsbox', function() {
				methods.hide.apply(self, arguments);
			});

			if( typeof options !== 'undefined' ) {
				if( typeof options.draggable !== 'undefined' && options.draggable ) {
					this.draggable();
				}
				if( typeof options.width !== 'undefined' ) {
					this.css('width', options.width);
				}
				if( typeof options.height !== 'undefined' ) {
					contentEl.css('height', options.height);
				}
				if( typeof options.x !== 'undefined' ) {
					this.css('left', options.x);
				}
				else if( typeof options.centerX !== 'undefined' && options.centerX ) {
					this.css('left', Math.floor($("body").width()/2-this.width()/2)+"px");
				}
				if( typeof options.y !== 'undefined' ) {
					this.css('top', options.y);
				}

				if( typeof options.center !== 'undefined' && options.center ) {
					this.css({
						left : Math.floor($("body").width()/2-this.width()/2)+"px",
						top : $(window).scrollTop()+Math.max(Math.floor(($(window).height()-this.height())/2),10)+"px"
					});
				}

				if( typeof options.closeButton !== 'undefined' && !options.closeButton ) {
					this.find('button.closebox').remove();
				}
				if( typeof options.closeButtonLabel !== 'undefined' ) {
					this.find('button.closebox').text(options.closeButtonLabel);
				}
			}
		},
		show : function() {
			this.css('display', 'block');
		},
		hide : function() {
			this.css('display', 'none');
			this.trigger('closed');
		}
	};

	$.fn.dsBox = function(method) {
		if( !this.hasClass('gfxbox') )	{
			if( arguments.length > 1 ) {
				methods.init.apply( this, Array.prototype.slice.call( arguments, 1 ) );
			}
			else {
				methods.init.apply( this, arguments );
			}
		}

		if ( methods[method] ) {
			methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
		}
		else if(method === 'content') {
			return this.find('.content');
		}
		return this;
	};
})( jQuery );

var ShiptypeBox = {
	show : function(shiptypeId)
	{
		var box = $('#shiptypeBox');
		if( box.size() == 0 )
		{
			$("body").append("<div id='shiptypeBox' class='gfxbox'><div class='content'><div>Lade...</div></div>"+
					"<button class='closebox'>schließen</button></div>");
			box = $('#shiptypeBox');
			var self = this;
			box.find('button.closebox').bind('click', function() {self.hide()});
		}

		box.css('display', 'block');

		var url = DS.getUrl();
		var params = {
				module:'schiffinfo',
				action:'default',
				ship:shiptypeId
		};

		$.get( url, params, this.__processResult );
	},
	__processResult : function(result)
	{
		var boxContent = $('#shiptypeBox .content');
		boxContent.children().remove();

		var table = $(result).find('#infotable');
		if( table.size() == 0 ) {
			boxContent.append('<div>Fehler bei Ermittlung der Schiffsdaten.</div>');
			return;
		}
		boxContent.append(table);
		DsTooltip.update(boxContent);
	},
	hide : function()
	{
		var box = $('#shiptypeBox');

		box.css('display', 'none');
		var content = box.find('.content');
		content.children().remove();
		content.append("<div>Lade...</div>");
	}
};

var DsTooltip = {
	create : function() {
		var ttdiv = $('#tt_div');
		if( ttdiv.size() == 0 ) {
			$("body").append('<div id="tt_div" />');
		}
	},
	show : function(event) {
		var target = $(event.currentTarget);
		var content = target.find('.ttcontent');
		if( content.size() == 0 ) {
			return;
		}

		var offset = target.offset();

		var ttdiv = $('#tt_div');
		ttdiv.empty();
		ttdiv.append(content.html());

		var height = target.height();

		// Inline-Elemente nehmen nicht die Hoehe ihrer innenliegenden Bilder an.
		// Daher die Hoehe des ersten Bildes nehmen (Annahnme: es gibt im Regelfall nur ein Bild
		// und das ist das Groesste)
		var img = target.find('img');
		if( img.size() > 0 ) {
			height = Math.max(height, target.find('img').height());
		}

		if( height < 40 ) {
			$(document).unbind('mousemove', DsTooltip._move);

			// Die berechnete Hoehe darf nicht(!) fuer die Positionierung
			// verwendet werden
			ttdiv.css({
				display:'block',
				top : (offset.top+target.height()+5)+'px',
				left : (offset.left+5)+"px"
			});
		}
		else {
			ttdiv.css({
				display:'block',
				top : (event.pageY+8)+'px',
				left : (event.pageY+8)+"px"
			});

			$(document).bind('mousemove', DsTooltip._move);
		}
	},
	hide : function(event) {
		var ttdiv = $('#tt_div');
		ttdiv.empty();
		ttdiv.css('display','none');

		$(document).unbind('mousemove', DsTooltip._move);
	},
	_move : function(event) {
		var ttdiv = $('#tt_div');
		ttdiv.css({
			top : (event.pageY+8)+'px',
			left : (event.pageX+8)+"px"
		});
	},
	update : function(root) {
		root.find('.tooltip')
			.bind({
				mouseover : function(event) {
					DsTooltip.show(event);
					return false;
				},
				mouseout :  function(event) {
					DsTooltip.hide(event);
					return false;
				}
			});
	}
};

var DsAutoComplete = {
	users : function(pattern, response) {
		var url = DS.getUrl();
		var params = {
				module:'search',
				action:'search',
				search:pattern.term,
				only:'users',
				max:5
		};

		jQuery.getJSON( url, params, function(result) {
			var data = [];
			for( var i=0; i < result.users.length; i++ ) {
				var user = result.users[i];
				data.push({label: user.name+" ("+user.id+")", value:user.id});
			}
			response(data);
		});
	}
}

$(document).ready(function() {
	DsTooltip.create();
	DsTooltip.update($("body"));
});