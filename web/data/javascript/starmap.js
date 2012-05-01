var Starmap = {
	__SECTOR_IMAGE_SIZE:25,
	__TILE_SIZE:20,
	showSystemSelection : function() {
		$('#systemauswahl').removeClass('invisible');
	},
	hideSystemSelection : function() {
		$('#systemauswahl').addClass('invisible');
	},
	showJumpnodeList : function() {
		$('#jumpnodebox').removeClass('invisible');
	},
	hideJumpnodeList : function() {
		$('#jumpnodebox').addClass('invisible');
	},
	load : function()
	{
		var sys = document.mapform.sys.value;
		var xstart = document.mapform.xstart.value;
		var ystart = document.mapform.ystart.value;
		var xend = document.mapform.xend.value;
		var yend = document.mapform.yend.value;
		
		this.hideSystemSelection();
		this.showLoader();
		this.clearMap();
		
		var self = this;
		
		$.getJSON('ds',
			{'sys': sys, 'xstart' : xstart, 'xend' : xend, 'ystart' : ystart, 'yend' : yend, 'loadmap' : '1', 'module': 'map', 'action' : 'map'},
			function(data) {
				self.renderMap(data);
				self.hideLoader();
			});
		
		return false;
	},
	clearMap : function()
	{
		$('#legend').remove();
		$('#tileOverlay').remove();
		$('#tiles').remove();
		$('#mapcontent').css('height', '0px');
	},
	renderMap : function(data)
	{
		var map = $('#mapcontent');
		map.css('height', ((data.size.maxy-data.size.miny+1)*this.__SECTOR_IMAGE_SIZE+70)+'px');
		
		var tiles = this.__renderTiles(data);
		var legend = this.__renderLegend(data);
		var overlay = this.__renderOverlay(data);
		
		map.append(tiles+legend+overlay);
		
		this.__updateJumpnodeList(data);
	},
	__updateJumpnodeList : function(data)
	{
		var listEl = $('#jumpnodelist');
		listEl.children().remove();
		
		var jns = '<table class="noBorderX">';
		
		for( var i=0; i < data.jumpnodes.length; i++ )
		{
			var jn = data.jumpnodes[i];
			
			jns += '<tr>';
			jns += '<td class="noBorderX" valign="top"><span class="nobr">'+jn.x+'/'+jn.y+'</span></td>';
			jns += '<td class="noBorderX">nach</td>';
			jns += '<td class="noBorderX"><span class="nobr">'+jn.name+' ('+jn.systemout+')'+jn.blocked+'</span></td>';
			jns += '</tr>';
		}
		jns += '</table>';
		
		listEl.append(jns);
	},
	__renderOverlay : function(data)
	{
		var overlay = "<div id='tileOverlay' style=\"width:"+(data.size.maxx-(data.size.minx-1))*this.__SECTOR_IMAGE_SIZE+"px;height:"+(data.size.maxy-(data.size.miny-1))*this.__SECTOR_IMAGE_SIZE+"px\">";
		for( var i=0; i < data.locations.length; i++ ) {
			var loc = data.locations[i];
			
			var posx = (loc.x-data.size.minx)*this.__SECTOR_IMAGE_SIZE;
			var posy = (loc.y-data.size.miny)*this.__SECTOR_IMAGE_SIZE;
			
			if( loc.bg != null ) {
				overlay += "<div style=\"top:"+posy+"px;left:"+posx+"px;background-image:url('"+data.dataPath+loc.bg+"')\" >";
			}
			else if( loc.fg != null ) {
				overlay += "<div style=\"top:"+posy+"px;left:"+posx+"px\" >";
			}
			
			if( loc.fg != null ) {
				overlay += "<img src=\""+data.dataPath+loc.fg;
				if( loc.scanner != null ) {
					overlay += "\" alt=\""+loc.x+"/"+loc.y+"\" class=\"showsector\" onclick=\"Starmap.showSector("+data.system+","+loc.x+","+loc.y+","+loc.scanner+")\" />";
				}
				else {
					overlay += "\" alt=\""+loc.x+"/"+loc.y+"\" />";
				}
			}
			
			if( loc.fg != null || loc.bg != null ) {
				overlay += "</div>";
			}
		}
		overlay += "</div>";
		
		return overlay;
	},
	__renderTiles : function(data)
	{
		var tiles = '<div id="tiles" style="width:'+(data.size.maxx-(data.size.minx-1))*this.__SECTOR_IMAGE_SIZE+'px;height:'+(data.size.maxy-(data.size.miny-1))*this.__SECTOR_IMAGE_SIZE+'px">';
		var startTileY = Math.floor((data.size.miny-1)/this.__TILE_SIZE);
		var startTileX = Math.floor((data.size.minx-1)/this.__TILE_SIZE);
		for( var y=startTileY; y <= Math.floor((data.size.maxy-1)/this.__TILE_SIZE); y++ )
		{
			for( var x=startTileX; x <= Math.floor((data.size.maxx-1)/this.__TILE_SIZE); x++ )
			{
				var xOffset = ((x-startTileX)*this.__TILE_SIZE-(data.size.minx-1)%this.__TILE_SIZE)*this.__SECTOR_IMAGE_SIZE;
				var yOffset = ((y-startTileY)*this.__TILE_SIZE-(data.size.miny-1)%this.__TILE_SIZE)*this.__SECTOR_IMAGE_SIZE;
				tiles += "<div style=\"left:"+xOffset+"px;top:"+yOffset+"px;background-image:url('./ds?module=map&action=tile&sys="+data.system+"&tileX="+x+"&tileY="+y+"')\" />";
			}
		}
		tiles += "</div>";
		return tiles;
	},
	__renderLegend : function(data)
	{
		var legend = "<table id=\"legend\" style='width:"+((data.size.maxx-(data.size.minx-1))*this.__SECTOR_IMAGE_SIZE+50)+"px;height:"+((data.size.maxy-(data.size.miny-1))*this.__SECTOR_IMAGE_SIZE+50)+"px'>";
		legend += this.__printXLegend(data.size.minx, data.size.maxx);
		var contentTd = false;
		
		for(var y = data.size.miny; y <= data.size.maxy; y++)
		{
			legend += "<tr>";
			legend += "<td class=\"border\">" + y +"</td>";
			if( !contentTd ) {
				legend += "<td colspan='"+(data.size.maxx-data.size.minx+1)+"' rowspan='"+(data.size.maxy-data.size.miny+1)+"' />";
				contentTd = true;
			}
			legend += "<td class=\"border\">" + y + "</td>";
			legend += "</tr>";
		}
		
		legend += this.__printXLegend(data.size.minx, data.size.maxx);
		legend += "</table>";
		return legend;
	},
	__printXLegend : function(start, end)
	{
		var legend = "<tr class=\"border\">";
		legend += "<td>x/y</td>";
		for(var x = start; x <= end; x++)
		{
			legend += "<td>"+x+"</td>";
		}
		legend += "</tr>";
		return legend;
	},
	__createSectorView : function()
	{
		if( $('#sectortable').size() == 0 )
		{
			var sectorview = "<div class='invisible gfxbox' id='sectortable' style='width:400px'><div><div>";
			sectorview += "<div id='sectorview'>";
			//Text is inserted here - using javascript
			sectorview += "</div>";
			sectorview += "</div></div></div>";
			sectorview += "</div>";
			$('#mapcontent').append(sectorview);
		}
	},
	showLoader : function()
	{
		this.__createSectorView();
		
		$('#sectorview').html("<div class='loader'>Verbinde mit interplanetarem Überwachungsnetzwerk...<br /><img src='./data/interface/ajax-loader.gif' alt='Lade' /></div>");
		$('#sectortable').removeClass('invisible');
	},
	hideLoader : function() 
	{
		$('#sectortable').addClass('invisible');
	},
	showSector : function (system, x, y, scanShip)
	{
		this.__createSectorView();
		
		$('#sectorview').html('Lade Sektor ' + system + ':' + x + '/' + y);
		$('#sectortable').removeClass('invisible');
		var self = this;
		$.getJSON('ds', 
			 {sys: system, x: x, y: y, scanship: scanShip, module: 'map', action:'sector'},
			 function(data)
			 {
				 self.openSector(system, x, y, data);
			 });
	},

	openSector : function (system, x, y, data)
	{
		var sector = $('#sectorview');
		var dialog = '<span>Sektor ' + system + ':' + x + '/' + y + '</span><a onclick="Starmap.closeSector()" style="float:right;color:#ff0000;">(x)</a><br><br>';
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
			dialog += '<span onclick="Starmap.toggleShowShipClasses(\''+shipclassId+'\')"><span id="'+shipclassId+'Toggle">+</span> '+this.name+'</span><span style="float:right;">'+shipcount+'</span><br>';
			dialog += shiptypes;
		});
		sector.html(dialog);
	},

	toggleShowShipClasses : function (shipclassId)
	{
		$('#' + shipclassId).toggleClass('invisible');
		var node = $('#' + shipclassId+'Toggle');
		if( node.text() == '+' )
		{
			node.text('-');
		}
		else
		{
			node.text('+');
		}
	},

	closeSector : function ()
	{
		$('#sectortable').addClass('invisible');
	}
};