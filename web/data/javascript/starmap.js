function showSector(system, x, y) 
{ 
	$.getJSON('ds', 
			 {sys: system, x: x, y: y, module: 'map', action:'sector'}, 
			 function(data)
			 {
				 openSector(system, x, y, data);
			 });
}

function openSector(system, x, y, data)
{
	var id = system+'-'+x+'-'+y;
	var dialog = '<div id="'+id+'"><div id="users">';
	$.each(data.ships, function()
	{
		dialog += '<h3><a href="#">'+this.name+' ('+this.shiptypes.length+')</a></h3>';
		dialog += '<div><p>Hier koennte auch ihr Text stehen.</p></div>';
	});
	dialog += '</div></div>';
	$('#sectors').append(dialog);
	$('#'+id).dialog();
	$('#'+id).find('#users').accordion();
}