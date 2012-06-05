if( typeof OLpageDefaults !== 'undefined' ) {
	OLpageDefaults(TEXTPADDING,0,TEXTFONTCLASS,'tooltip',FGCLASS,'tooltip',BGCLASS,'tooltip');
}

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
				if( typeof options.x !== 'undefined' ) {
					this.css('left', options.x);
				}
				else if( typeof options.centerX !== 'undefined' && options.centerX ) {
					this.css('left', Math.floor($("body").width()/2-this.width()/2)+"px");
				}
				if( typeof options.y !== 'undefined' ) {
					this.css('top', options.y);
				}
				if( typeof options.closeButton !== 'undefined' && !options.closeButton ) {
					this.find('button.closebox').remove();
				}
				if( typeof options.width !== 'undefined' ) {
					this.css('width', options.width);
				}
				if( typeof options.height !== 'undefined' ) {
					contentEl.css('height', options.height);
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
		var box = jQuery('#shiptypeBox');
		if( box.size() == 0 )
		{
			jQuery("body").append("<div id='shiptypeBox' class='gfxbox'><div class='content'><div>Lade...</div></div>"+
					"<button class='closebox'>schließen</button></div>");
			box = jQuery('#shiptypeBox');
			var self = this;
			box.find('button.closebox').bind('click', function() {self.hide()});
		}

		box.css('display', 'block');

		var url = getDsUrl();
		var params = {
				module:'schiffinfo',
				action:'default',
				ship:shiptypeId
		};

		jQuery.get( url, params, this.__processResult );
	},
	__processResult : function(result)
	{
		var boxContent = jQuery('#shiptypeBox .content');
		boxContent.children().remove();

		var table = jQuery(result).find('#infotable');
		if( table.size() == 0 ) {
			boxContent.append('<div>Fehler bei Ermittlung der Schiffsdaten.</div>');
			return;
		}
		boxContent.append(table);
		DsTooltip.update(boxContent);
	},
	hide : function()
	{
		var box = jQuery('#shiptypeBox');

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

$(document).ready(function() {
	DsTooltip.create();
	DsTooltip.update($("body"));
});