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
	var dialog = '<span>Sektor ' + system + ':' + x + '/' + y + '</span><a onClick="closeSector()" style="float:right;color:#ff0000;">(x)</a><br><br>';
	$.each(data.users, function()
	{
		var shipcount = 0;
        var shipclassId = this.id + '-shipclasses';
        var shiptypes = '<ul class="invisible" id="'+shipclassId+'" class="shipclasses">';
		$.each(this.shiptypes, function()
		{
            shiptypes += '<li><span>'+this.name+'</span><span style="float:right;">'+this.ships.length+'</span></li>';
			shipcount += this.ships.length;
		});
        shiptypes += '</ul>';
		dialog += '<span onClick="toggleShowShipClasses(\''+shipclassId+'\')">'+this.name+'</span><span style="float:right;">'+shipcount+'</span><br>';
        dialog += shiptypes;
	});
	sector.html(dialog);
}

function toggleShowShipClasses(shipclassId)
{
    $('#' + shipclassId).toggleClass('invisible');
}

function closeSector()
{
	$('#sectortable').addClass('invisible');
}