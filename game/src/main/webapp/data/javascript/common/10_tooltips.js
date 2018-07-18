var DsTooltip = {
	__lastTimeout : null,
	__currentTarget : null,
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

		this.__currentTarget = content;

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

		if( this.__lastTimeout !== null ) {
			clearTimeout(this.__lastTimeout);
			this.__lastTimeout = null;
		}

		if( content.hasClass("ttitem") ) {
			var self = this;
			this.__lastTimeout = setTimeout(function() {
				self.showItemTooltip(content);
			}, 1000);
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
		this.__currentTarget = null;
		if( this.__lastTimeout != null ) {
			clearTimeout(this.__lastTimeout);
			this.__lastTimeout = null;
		}
		var ttdiv = $('#tt_div');
		ttdiv.empty();
		ttdiv.css('display','none');

		$(document).unbind('mousemove', DsTooltip._move);
	},
	showItemTooltip : function(target) {
		if( this.__currentTarget == null || target !== this.__currentTarget ) {
			return;
		}

		var self = this;
		DS.get(
				{module:'iteminfo',action:'details',item:target.attr('ds-item-id')},
				function(resp) {
					self.__processItemTooltipResponse(target, resp);
				});
	},
	__processItemTooltipResponse : function(target, resp) {
		if( target !== this.__currentTarget ) {
			return;
		}
		var ttcontent = $('#tt_div');
		ttcontent.empty();
		ttcontent.append($(resp).filter('.gfxbox').children());
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
