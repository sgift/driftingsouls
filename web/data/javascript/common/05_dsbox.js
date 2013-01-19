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
					this.css('left', Math.floor($("body").width()/2-this.outerWidth()/2)+"px");
				}
				if( typeof options.y !== 'undefined' ) {
					this.css('top', options.y);
				}

				if( typeof options.center !== 'undefined' && options.center ) {
					this.css({
						left : Math.floor($("body").width()/2-this.outerWidth()/2)+"px",
						top : $(window).scrollTop()+Math.max(Math.floor(($(window).height()-this.outerHeight())/2),10)+"px"
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