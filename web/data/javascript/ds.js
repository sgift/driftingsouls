jQuery.noConflict();

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