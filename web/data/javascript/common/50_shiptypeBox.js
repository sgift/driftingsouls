

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