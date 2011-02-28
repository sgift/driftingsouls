function showSector(system, x, y) 
{
	$('#sectorview').html('Lade Sektor ' + system + ':' + x + '/' + y);
	$('#sectortable').removeClass('invisible');
	$.getJSON('ds', 
			 {sys: system, x: x, y: y, module: 'map', action:'sector'}, 
			 function(data)
			 {
				 openSector(system, x, y, data);
			 });
}

function openSector(system, x, y, data)
{
	var sector = $('#sectorview');
	var dialog = '<span>Sektor ' + system + ':' + x + '/' + y + '</span><a onClick="closeSector()" style="float:right;color:red;">(x)</a><br><br>';
	$.each(data.users, function()
	{
		var shipcount = 0;
		$.each(this.shiptypes, function()
		{
			shipcount += this.ships.length;
		});
		dialog += '<span>'+this.name+'</span><span style="float:right;">'+shipcount+'</span><br>';
	});
	sector.html(dialog);
}

function closeSector()
{
	$('#sectortable').addClass('invisible');
}