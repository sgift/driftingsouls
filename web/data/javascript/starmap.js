var StarmapActionOverlay = function(options){
	this.__lastDrag = [];
	
	this.create = function()
	{
		var overlay = "<div id='actionOverlay' />";
		
		$('#mapview').append(overlay);
		
		var self = this;
		$('#actionOverlay').bind('click', function(e) 
		{
			document.body.focus();
			document.onselectstart = function () { return false; };
			
			var offset = $(e.target).offset();
			var x = e.pageX - offset.left;
			var y = e.pageY - offset.top;
			
			e.stopPropagation();
			
			self.onClick(x, y);
		});
		
		$('#actionOverlay').bind('mousedown', function(e) {
			if( e.which != 1 ) {
				return;
			}
			e.stopPropagation();
			self.__lastDrag = [e.pageX, e.pageY];
			
			e.target.ondragstart = function() { return false; };
			document.body.focus();
			document.onselectstart = function () { return false; };
			
			self.onDragStart();
		});
		
		$('#actionOverlay').bind('mouseup mouseout', function(e) {
			self.__lastDrag = [];
			e.stopPropagation();
			
			self.onDragStop();
		});
		
		$('#actionOverlay').bind('mousemove', function(e) 
		{
			var drag = self.__lastDrag;
			if( typeof drag === "undefined" || drag.length == 0 ) {
				return;
			}
			
			document.body.focus();
			document.onselectstart = function () { return false; };
			
			e.stopPropagation();
			
			var moveX = drag[0] - e.pageX;
			var moveY = drag[1] - e.pageY;
			
			self.onDrag(moveX, moveY);
		
			self.__lastDrag = [e.pageX, e.pageY];
		});
	};
	
	this.create();
	
	this.onClick = options.onClick;
	this.onDragStart = options.onDragStart;
	this.onDrag = options.onDrag;
	this.onDragStop = options.onDragStop;
	
	if( typeof this.onClick === "undefined" ) {
		this.onDragClick = function() {};
	}
	if( typeof this.onDragStart === "undefined" ) {
		this.onDragStart = function() {};
	}
	if( typeof this.onDrag === "undefined" ) {
		this.onDrag = function() {};
	}
	if( typeof this.onDragStop === "undefined" ) {
		this.onDragStop = function() {};
	}
};

var StarmapLegend = function(screenSize, mapSize) {
	this.__screenSize = [screenSize[0], screenSize[1]];
	this.__currentShiftOffset = [0, 0];
	this.__size = {minx:0, miny:0, maxy:0, maxy:0};
	
	this.__renderLegend = function(screenSize, mapSize)
	{
		this.__size = {minx:mapSize.minx, maxx:mapSize.maxx, miny:mapSize.miny, maxy:mapSize.maxy};
		var legend = "<div id=\"legend\" style='width:"+(screenSize[0]+50)+"px;height:"+(screenSize[1]+50)+"px'>";
		legend += "<div class='top'>";
		legend += "<div class='corner'>x/y</div>";
		legend += this.__printLegend("hbar", mapSize.minx, mapSize.maxx);
		legend += "<div class='corner'></div>";
		legend += "</div>";
		
		legend += "<div class='left'>";
		legend += this.__printLegend("vbar", mapSize.miny, mapSize.maxy);
		legend += "</div>";
		
		legend += "<div class='right'>";
		legend += this.__printLegend("vbar", mapSize.miny, mapSize.maxy);
		legend += "</div>";
		
		legend += "<div class='bottom'>";
		legend += "<div class='corner'>x/y</div>";
		legend += this.__printLegend("hbar", mapSize.minx, mapSize.maxx);
		legend += "<div class='corner'></div>";
		legend += "</div></div>";
		return legend;
	};
	this.__printLegend = function(cls, start, end)
	{
		var legend = "<div class=\""+cls+"\"><div class=\"scroll\">";
		for(var x = start; x <= end; x++)
		{
			legend += "<div>"+x+"</div>";
		}
		legend += "</div></div>";
		return legend;
	};
	this.prepareShift = function(moveX, moveY) {
		var hbar = $('#legend .hbar .scroll');
		var vbar = $('#legend .vbar .scroll');
		
		this.__currentShiftOffset = [this.__currentShiftOffset[0]-moveX, this.__currentShiftOffset[1]-moveY];
		
		if( this.__currentShiftOffset[0] >= Starmap.__SECTOR_IMAGE_SIZE ) {
			this.__currentShiftOffset[0] -= Starmap.__SECTOR_IMAGE_SIZE;
			
			hbar.prepend("<div>"+(this.__size.minx-1)+"</div>");

			this.__size.minx -= 1;
		}
		else if( this.__currentShiftOffset[0] + (this.__size.maxx-this.__size.minx)*Starmap.__SECTOR_IMAGE_SIZE < this.__screenSize[0] ) {
			hbar.append("<div>"+(this.__size.maxx+1)+"</div>");
			this.__size.maxx += 1;
		}
		
		if( this.__currentShiftOffset[1] >= Starmap.__SECTOR_IMAGE_SIZE ) {
			this.__currentShiftOffset[1] -= cnt*Starmap.__SECTOR_IMAGE_SIZE;
			
			vbar.prepend("<div>"+(this.__size.miny-1)+"</div>");
			this.__size.miny -= 1;
		}
		else if( this.__currentShiftOffset[1] + (this.__size.maxy-this.__size.miny)*Starmap.__SECTOR_IMAGE_SIZE < this.__screenSize[1] ) {
			vbar.append("<div>"+(this.__size.maxy+1)+"</div>");
			this.__size.maxy += 1;
		}
	};
	this.storeShift = function() {
		var hbar = $('#legend .hbar .scroll');
		var vbar = $('#legend .vbar .scroll');
		
		hbar.css('left' , (this.__currentShiftOffset[0])+'px');
		vbar.css('top' , (this.__currentShiftOffset[1])+'px');
	};
	
	$('#mapcontent').append(this.__renderLegend(screenSize, mapSize));
};

var StarmapTiles = function(systemId, screenSize, mapSize) {
	this.__TILE_SIZE = 20;
	this.__screenSize = [screenSize[0], screenSize[1]];
	this.__currentShiftOffset = [0, 0];
	this.__size = {minx:mapSize.minx, maxx:mapSize.maxx, miny:mapSize.miny, maxy:mapSize.maxy};
	this.__systemId = systemId;
	this.__renderedTiles = [];
	
	this.__renderTiles = function(mapSize)
	{
		var tiles = '<div id="tiles">';
		var startTileY = Math.floor((mapSize.miny-1)/this.__TILE_SIZE);
		var startTileX = Math.floor((mapSize.minx-1)/this.__TILE_SIZE);
		for( var y=startTileY; y <= Math.floor((mapSize.maxy-1)/this.__TILE_SIZE); y++ )
		{
			for( var x=startTileX; x <= Math.floor((mapSize.maxx-1)/this.__TILE_SIZE); x++ )
			{
				var xOffset = ((x-startTileX)*this.__TILE_SIZE-(mapSize.minx-1)%this.__TILE_SIZE)*Starmap.__SECTOR_IMAGE_SIZE;
				var yOffset = ((y-startTileY)*this.__TILE_SIZE-(mapSize.miny-1)%this.__TILE_SIZE)*Starmap.__SECTOR_IMAGE_SIZE;
				tiles += "<img style=\"left:"+xOffset+"px;top:"+yOffset+"px;\" src=\"./ds?module=map&action=tile&sys="+this.__systemId+"&tileX="+x+"&tileY="+y+"\" />";
				this.__renderedTiles[x+"/"+y] = true;
			}
		}
		tiles += "</div>";
		return tiles;
	};
	
	this.prepareShift = function(moveX, moveY) {
		var tiles = $('#tiles');
			
		this.__currentShiftOffset = [this.__currentShiftOffset[0]-moveX, this.__currentShiftOffset[1]-moveY];
		
		var realSize = {minx:this.__size.minx, maxx:this.__size.maxx, miny:this.__size.miny, maxy:this.__size.maxy};
		
		var mod = false;
		if( this.__currentShiftOffset[0] >= this.__TILE_SIZE ) {
			var cnt = Math.max(this.__currentShiftOffset[0] / this.__TILE_SIZE);
			this.__currentShiftOffset[0] -= cnt*this.__SECTOR_IMAGE_SIZE;
			realSize.minx -= cnt;
			mod = true;
		}
		else if( this.__currentShiftOffset[0] + (this.__size.maxx-this.__size.minx)*this.__TILE_SIZE < this.__screenSize[0] ) {
			var gap = this.__screenSize[0] - (this.__currentShiftOffset[0] + (this.__size.maxx-this.__size.minx)*this.__TILE_SIZE);
			realSize.maxx += Math.ceil(gap / Starmap.__SECTOR_IMAGE_SIZE);
			mod = true;
		}
		
		if( this.__currentShiftOffset[1] >= Starmap.__SECTOR_IMAGE_SIZE ) {
			var cnt = Math.max(this.__currentShiftOffset[1] / this.__TILE_SIZE);
			this.__currentShiftOffset[1] -= cnt*Starmap.__SECTOR_IMAGE_SIZE;
			realSize.miny -= cnt;
			mod = true;
		}
		else if( this.__currentShiftOffset[1] + (this.__size.maxy-this.__size.miny)*this.__TILE_SIZE < this.__screenSize[1] ) {
			var gap = this.__screenSize[1] - (this.__currentShiftOffset[1] + (this.__size.maxy-this.__size.miny)*this.__TILE_SIZE);
			realSize.maxy += Math.ceil(gap / Starmap.__SECTOR_IMAGE_SIZE);
			mod = true;
		}
		
		if( mod ) {
			var newTiles = "";
			
			var startTileY = Math.floor((realSize.miny-1)/this.__TILE_SIZE);
			var startTileX = Math.floor((realSize.minx-1)/this.__TILE_SIZE);
			for( var y=startTileY; y <= Math.floor((realSize.maxy-1)/this.__TILE_SIZE); y++ )
			{
				for( var x=startTileX; x <= Math.floor((realSize.maxx-1)/this.__TILE_SIZE); x++ )
				{
					if( typeof this.__renderedTiles[x+"/"+y] !== "undefined" ) {
						continue;
					}
					var xOffset = ((x-startTileX)*this.__TILE_SIZE-(realSize.minx-1)%this.__TILE_SIZE)*Starmap.__SECTOR_IMAGE_SIZE;
					var yOffset = ((y-startTileY)*this.__TILE_SIZE-(realSize.miny-1)%this.__TILE_SIZE)*Starmap.__SECTOR_IMAGE_SIZE;
					newTiles += "<img style=\"left:"+xOffset+"px;top:"+yOffset+"px;\" src=\"./ds?module=map&action=tile&sys="+this.__systemId+"&tileX="+x+"&tileY="+y+"\" />";
					this.__renderedTiles[x+"/"+y] = true;
				}
			}
			
			tiles.append(newTiles);
			
			this.__size = realSize;
		}
	};
	this.storeShift = function(mapview) {
		var tiles = mapview.find('#tiles');
		tiles.css({'left' : (this.__currentShiftOffset[0])+'px', 'top' : (this.__currentShiftOffset[1])+'px'});
	};
	
	$('#mapview').append(this.__renderTiles(mapSize));
};

var Starmap = {
	__SECTOR_IMAGE_SIZE:25,
	__currentSize:{minx:0,maxx:0,miny:0,maxy:0},
	__currentLocations:[],
	__currentSystem:null,
	__screen:[],
	__currentShiftOffset:[],
	__actionOverlay:null,
	__starmapLegend:null,
	__starmapTiles:null,
	__reloadTriggered:false,
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
		$('#actionOverlay').remove();
		$('#mapcontent').css('height', '0px');
		this.__currentShiftOffset = [];
	},
	renderMap : function(data)
	{
		this.__currentSize = data.size;
		this.__currentLocations = data.locations;
		this.__currentSystem = data.system;
		this.__screen[0] = (this.__currentSize.maxx-this.__currentSize.minx+1)*this.__SECTOR_IMAGE_SIZE;
		this.__screen[1] = (this.__currentSize.maxy-this.__currentSize.miny+1)*this.__SECTOR_IMAGE_SIZE;
		
		var map = $('#mapcontent');
		map.css('height', (this.__screen[1]+70)+'px');
		
		if( $('#mapview').size() == 0 ) {
			map.append('<div id="mapview" />');
		}
		
		$('#mapview').css({"width" : this.__screen[0]+"px", "height" : this.__screen[1]+"px"});
		
		this.__starmapLegend = new StarmapLegend(this.__screen, this.__currentSize);
		this.__starmapTiles = new StarmapTiles(this.__currentSystem.id, this.__screen, this.__currentSize);
		
		//var tiles = this.__renderTiles(data);
		var overlay = this.__renderOverlay(data);
		
		$('#mapview').append(overlay);
		
		var self = this;
		this.__actionOverlay = new StarmapActionOverlay({
			onClick : function(x, y) {
				x -= self.__currentShiftOffset[0];
				y -= self.__currentShiftOffset[1];
				var sectorX = self.__currentSize.minx+Math.floor(x / self.__SECTOR_IMAGE_SIZE);
				var sectorY = self.__currentSize.miny+Math.floor(y / self.__SECTOR_IMAGE_SIZE);
				
				for( var i=0; i < self.__currentLocations.length; i++ )
				{
					var loc = self.__currentLocations[i];
					if( loc.x == sectorX && loc.y == sectorY )
					{
						if( loc.scanner != null )
						{
							self.showSector(self.__currentSystem, sectorX, sectorY, loc.scanner);
						}
						break;
					}
				}
			},
			onDragStart : function() {
				if( self.__currentShiftOffset.length == 0 ) {
					self.__currentShiftOffset = [0,0];
				}
			},
			onDrag : function(moveX, moveY) {
				var newOff = [self.__currentShiftOffset[0]-moveX, self.__currentShiftOffset[1]-moveY];
				
				if( newOff[0] > (self.__currentSize.minx-1)*self.__SECTOR_IMAGE_SIZE ) {
					newOff[0] = (self.__currentSize.minx-1)*self.__SECTOR_IMAGE_SIZE
				}
				else if( newOff[0] < 0 && -newOff[0] > (self.__currentSystem.width-self.__currentSize.maxx)*self.__SECTOR_IMAGE_SIZE ) {
					newOff[0] = -(self.__currentSystem.width-self.__currentSize.maxx)*self.__SECTOR_IMAGE_SIZE
				}
				
				if( newOff[1] > (self.__currentSize.minx-1)*self.__SECTOR_IMAGE_SIZE ) {
					newOff[1] = (self.__currentSize.minx-1)*self.__SECTOR_IMAGE_SIZE
				}
				else if( newOff[1] < 0 && -newOff[1] > (self.__currentSystem.height-self.__currentSize.maxy)*self.__SECTOR_IMAGE_SIZE ) {
					newOff[1] = -(self.__currentSystem.height-self.__currentSize.maxy)*self.__SECTOR_IMAGE_SIZE
				}
		
				self.__starmapTiles.prepareShift(self.__currentShiftOffset[0]-newOff[0], self.__currentShiftOffset[1]-newOff[1]);
				self.__starmapLegend.prepareShift(self.__currentShiftOffset[0]-newOff[0], self.__currentShiftOffset[1]-newOff[1]);
				
				var cl = $('#mapview');//.clone(true);
				
				self.__starmapLegend.storeShift();
				self.__starmapTiles.storeShift(cl);
				
				var overlay = cl.find('#tileOverlay');
				overlay.css({'left' : (newOff[0])+'px', 'top' : (newOff[1])+'px'});
				
				//$('#mapview').remove();
				//$('#mapcontent').append(cl);
				
				self.__currentShiftOffset = newOff;
				
				if( !self.__reloadTriggered ) {
					self.__reloadTriggered = true;
					setTimeout(function() {self.__reloadOverlay();}, 500);
				}
			}
		});

		this.__updateJumpnodeList(data);
	},
	__reloadOverlay : function()
	{
		var realSize = {minx:this.__currentSize.minx, maxx:this.__currentSize.maxx, miny:this.__currentSize.miny, maxy:this.__currentSize.maxy};
		
		var mod = false;
		if( this.__currentShiftOffset[0] >= this.__SECTOR_IMAGE_SIZE ) {
			var cnt = Math.max(this.__currentShiftOffset[0] / this.__SECTOR_IMAGE_SIZE);
			this.__currentShiftOffset[0] -= cnt;
			realSize.minx -= cnt;
			mod = true;
		}
		else if( this.__currentShiftOffset[0] + (this.__currentSize.maxx-this.__currentSize.minx)*this.__SECTOR_IMAGE_SIZE < this.__screen[0] ) {
			var gap = this.__screen[0] - (this.__currentShiftOffset[0] + (this.__currentSize.maxx-this.__currentSize.minx)*this.__SECTOR_IMAGE_SIZE);
			realSize.maxx += Math.ceil(gap / this.__SECTOR_IMAGE_SIZE);
			mod = true;
		}
		
		if( this.__currentShiftOffset[1] >= this.__SECTOR_IMAGE_SIZE ) {
			var cnt = Math.max(this.__currentShiftOffset[1] / this.__SECTOR_IMAGE_SIZE);
			this.__currentShiftOffset[1] -= cnt;
			realSize.miny -= cnt;
			mod = true;
		}
		else if( this.__currentShiftOffset[1] + (this.__currentSize.maxy-this.__currentSize.miny)*this.__SECTOR_IMAGE_SIZE < this.__screen[1] ) {
			var gap = this.__screen[1] - (this.__currentShiftOffset[1] + (this.__currentSize.maxy-this.__currentSize.miny)*this.__SECTOR_IMAGE_SIZE);
			realSize.maxy += Math.ceil(gap / this.__SECTOR_IMAGE_SIZE);
			mod = true;
		}
		
		if( mod ) {
			var self = this;
			$.getJSON('ds',
					{'sys': this.__currentSystem.id, 'xstart' : realSize.minx, 'xend' : realSize.maxx, 'ystart' : realSize.miny, 'yend' : realSize.maxy, 'loadmap' : '1', 'module': 'map', 'action' : 'map'},
					function(data) {
						var overlay = self.__renderOverlay(data);
						$('#tileOverlay').remove();
						$('#mapview').append(overlay);
						self.__currentSize.maxx = data.size.maxx;
						self.__currentSize.maxy = data.size.maxy;
						self.__reloadTriggered = false;
					});
		}
		else {
			this.__reloadTriggered = false;
		}
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
		var overlay = "<div id='tileOverlay' style='left:"+(this.__currentShiftOffset[0])+"px;top:"+(this.__currentShiftOffset[1])+"px'>";
		for( var i=0; i < data.locations.length; i++ ) {
			var loc = data.locations[i];
			
			var posx = (loc.x-this.__currentSize.minx)*this.__SECTOR_IMAGE_SIZE;
			var posy = (loc.y-this.__currentSize.miny)*this.__SECTOR_IMAGE_SIZE;
			
			if( loc.bg != null ) {
				overlay += "<div style=\"top:"+posy+"px;left:"+posx+"px;background-image:url('"+data.dataPath+loc.bg+"')\" >";
			}
			else if( loc.fg != null ) {
				overlay += "<div style=\"top:"+posy+"px;left:"+posx+"px\" >";
			}
			
			if( loc.fg != null ) {
				overlay += "<img src=\""+data.dataPath+loc.fg;
				if( loc.scanner != null ) {
					overlay += "\" alt=\""+loc.x+"/"+loc.y+"\" class=\"showsector\" />";
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
		
		$('#sectorview').html('Lade Sektor ' + system.id + ':' + x + '/' + y);
		$('#sectortable').removeClass('invisible');
		var self = this;
		$.getJSON('ds', 
			 {sys: system.id, x: x, y: y, scanship: scanShip, module: 'map', action:'sector'},
			 function(data)
			 {
				 self.openSector(system.id, x, y, data);
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