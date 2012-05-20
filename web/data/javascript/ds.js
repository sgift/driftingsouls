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
			