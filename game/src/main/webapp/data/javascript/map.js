/**
 * Hilfsklasse zur Verwaltung des momentanen Anzeigebereichs (Kartengroesse).
 */
var StarmapScreen = function(selector, system) {
	this.__screen = [0,0];
	this.__currentSystem = system;
	this.__max = [this.__currentSystem.width*Starmap.__SECTOR_IMAGE_SIZE, this.__currentSystem.height*Starmap.__SECTOR_IMAGE_SIZE];
	this.__selector = selector;
	
	this.width = function() {
		return Math.min(this.__screen[0], this.__max[0]);
	};
	this.height = function() {
		return Math.min(this.__screen[1], this.__max[1]);
	};
	this.widthInSectors = function() {
		return Math.min(Math.ceil(this.__screen[0] / Starmap.__SECTOR_IMAGE_SIZE), this.__currentSystem.width);
	};
	this.heightInSectors = function() {
		return Math.min(Math.ceil(this.__screen[1] / Starmap.__SECTOR_IMAGE_SIZE), this.__currentSystem.height);
	};
	this.widthGap = function(offset, minx, maxx) {
		var gap = this.width() - (offset + (maxx-minx)*Starmap.__SECTOR_IMAGE_SIZE);
		if( gap > 0 ) {
			return Math.ceil(gap/Starmap.__SECTOR_IMAGE_SIZE);
		}
		return 0;
	};
	this.heightGap = function(offset, minx, maxx) {
		var gap = this.height() - (offset + (maxx-minx)*Starmap.__SECTOR_IMAGE_SIZE);
		if( gap > 0 ) {
			return Math.ceil(gap/Starmap.__SECTOR_IMAGE_SIZE);
		}
		return 0;
	};
	
	this.update = function() {
		var el = $(this.__selector);
		this.__screen = [el.width(),el.height()];
	};
	
	this.update();
};
/**
 * Das Aktionsoverlay der Sternenkarte. Nimmt Aktionen (Clicks, Drags) usw
 * entgegen und berechnet entsprechende Events fuer die Sternenkarte.
 * Die Eventhandler muessen durch die Sternenkarte ueberschrieben werden.
 */
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

/**
 * Die Legende (Rahmen) der Sternenkarte. Erzeugt oben und unten
 * eine Achse mit x-Koordinaten des momentanen Ausschnitts und rechts
 * und links eine Achse mit y-Koordinaten des momentanen Ausschnitts.
 * Das Rendering erfolgt allein clientseitig.
 */
var StarmapLegend = function(screen, mapSize) {
	this.__screen = screen;
	this.__currentShiftOffset = [0, 0];
	this.__size = {minx:0, miny:0, maxy:0, maxy:0};
	
	this.__renderLegend = function(mapSize)
	{
		this.__size = {minx:mapSize.minx, maxx:mapSize.maxx, miny:mapSize.miny, maxy:mapSize.maxy};
		var legend = "<div id=\"legend\">";
		legend += "<div class='top'>";
		legend += "<div class='corner'>x/y</div>";
		legend += this.__printLegend("hbar", this.__size.minx, this.__size.maxx);
		legend += "</div>";
		
		legend += "<div class='left'>";
		legend += this.__printLegend("vbar", this.__size.miny, this.__size.maxy);
		legend += "</div>";
		
		legend += "<div class='right'>";
		legend += this.__printLegend("vbar", this.__size.miny, this.__size.maxy);
		legend += "</div>";
		
		legend += "<div class='bottom'>";
		legend += "<div class='corner'>x/y</div>";
		legend += this.__printLegend("hbar", this.__size.minx, this.__size.maxx);
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
		else if( this.__screen.widthGap(this.__currentShiftOffset[0], this.__size.minx,this.__size.maxx) > 0 ) {
			var cnt = this.__screen.widthGap(this.__currentShiftOffset[0], this.__size.minx,this.__size.maxx);
			var content = "";
			for( var i=1; i <= cnt; ++i ) {
				content += "<div>"+(this.__size.maxx+i)+"</div>";
			}
			hbar.append(content);
			this.__size.maxx += cnt;
		}
		
		if( this.__currentShiftOffset[1] >= Starmap.__SECTOR_IMAGE_SIZE ) {
			this.__currentShiftOffset[1] -= Starmap.__SECTOR_IMAGE_SIZE;
			
			vbar.prepend("<div>"+(this.__size.miny-1)+"</div>");
			this.__size.miny -= 1;
		}
		else if( this.__screen.heightGap(this.__currentShiftOffset[1], this.__size.miny,this.__size.maxy) > 0 ) {
			var cnt = this.__screen.heightGap(this.__currentShiftOffset[1], this.__size.miny,this.__size.maxy);
			var content = "";
			for( var i=1; i <= cnt; ++i ) {
				content += "<div>"+(this.__size.maxy+i)+"</div>";
			}
			vbar.append(content);
			this.__size.maxy += cnt;
		}
	};
	this.storeShift = function() {
		var hbar = $('#legend .hbar .scroll');
		var vbar = $('#legend .vbar .scroll');
		
		hbar.css('left' , (this.__currentShiftOffset[0])+'px');
		vbar.css('top' , (this.__currentShiftOffset[1])+'px');
	};
	
	$('#mapcontent').append(this.__renderLegend(mapSize));
};
/**
 * Der Hintergrund (Tiles) der Sternenkarte. Dieser besteht aus einzelnen Kacheln.
 * Die Identifikation der Kacheln geschieht allein clientseitig.
 */
var StarmapTiles = function(systemId, screenSize, mapSize) {
	this.__TILE_SIZE = 20;
	this.__screen = screenSize;
	this.__currentShiftOffset = [0, 0];
	this.__size = {minx:mapSize.minx, maxx:mapSize.maxx, miny:mapSize.miny, maxy:mapSize.maxy};
	this.__systemId = systemId;
	this.__renderedTiles = [];
	this.__startPos = [Math.floor((mapSize.minx-1)/this.__TILE_SIZE), Math.floor((mapSize.miny-1)/this.__TILE_SIZE)];
	this.__startSectorPos = [mapSize.minx, mapSize.miny];
	
	this.__renderTiles = function(mapSize)
	{
		var url = Starmap.__getDsUrl();
		var tiles = '<div id="tiles">';
		var startTileY = Math.floor((mapSize.miny-1)/this.__TILE_SIZE);
		var startTileX = Math.floor((mapSize.minx-1)/this.__TILE_SIZE);
		for( var y=startTileY; y <= Math.floor((mapSize.maxy-1)/this.__TILE_SIZE); y++ )
		{
			for( var x=startTileX; x <= Math.floor((mapSize.maxx-1)/this.__TILE_SIZE); x++ )
			{
				var xOffset = ((x-startTileX)*this.__TILE_SIZE-(mapSize.minx-1)%this.__TILE_SIZE)*Starmap.__SECTOR_IMAGE_SIZE;
				var yOffset = ((y-startTileY)*this.__TILE_SIZE-(mapSize.miny-1)%this.__TILE_SIZE)*Starmap.__SECTOR_IMAGE_SIZE;
				tiles += "<img style=\"left:"+xOffset+"px;top:"+yOffset+"px;\" src=\""+url+"?module=map&action=tile&sys="+this.__systemId+"&tileX="+x+"&tileY="+y+"\" />";
				this.__renderedTiles[x+"/"+y] = true;
			}
		}
		tiles += "</div>";
		return tiles;
	};
	
	this.prepareShift = function(moveX, moveY) {
		var tiles = $('#tiles');
		if( isNaN(moveX) || isNaN(this.__currentShiftOffset[0]) ) {
			throw "NaN";
		}
			
		this.__currentShiftOffset = [this.__currentShiftOffset[0]-moveX, this.__currentShiftOffset[1]-moveY];
		
		var realSize = {minx:this.__size.minx, maxx:this.__size.maxx, miny:this.__size.miny, maxy:this.__size.maxy};
		
		var mod = false;
		if( this.__currentShiftOffset[0] >= Starmap.__SECTOR_IMAGE_SIZE ) {
			var cnt = Math.ceil(this.__currentShiftOffset[0] / Starmap.__SECTOR_IMAGE_SIZE);
			//this.__currentShiftOffset[0] -= cnt*Starmap.__SECTOR_IMAGE_SIZE;
			realSize.minx = this.__startSectorPos[0]-cnt;
			mod = true;
		}
		else if( this.__currentShiftOffset[0] + (this.__size.maxx-this.__size.minx)*this.__TILE_SIZE < this.__screen.width() ) {
			var gap = this.__screen.width() - (this.__currentShiftOffset[0] + (this.__size.maxx-this.__size.minx)*this.__TILE_SIZE);
			realSize.maxx += Math.ceil(gap / Starmap.__SECTOR_IMAGE_SIZE);
			mod = true;
		}
		
		if( this.__currentShiftOffset[1] >= Starmap.__SECTOR_IMAGE_SIZE ) {
			var cnt = Math.ceil(this.__currentShiftOffset[1] / Starmap.__SECTOR_IMAGE_SIZE);
			//this.__currentShiftOffset[1] -= cnt*Starmap.__SECTOR_IMAGE_SIZE;
			realSize.miny = this.__startSectorPos[1]-cnt;
			mod = true;
		}
		else if( this.__currentShiftOffset[1] + (this.__size.maxy-this.__size.miny)*this.__TILE_SIZE < this.__screen.height() ) {
			var gap = this.__screen.height() - (this.__currentShiftOffset[1] + (this.__size.maxy-this.__size.miny)*this.__TILE_SIZE);
			realSize.maxy += Math.ceil(gap / Starmap.__SECTOR_IMAGE_SIZE);
			mod = true;
		}
		
		if( mod ) {
			var newTiles = "";
			var url = Starmap.__getDsUrl();
			var startTileY = Math.floor((realSize.miny-1)/this.__TILE_SIZE);
			var startTileX = Math.floor((realSize.minx-1)/this.__TILE_SIZE);
			if( startTileX < 0 ) {
				throw "OutOfRange";
			}
			for( var y=startTileY; y <= Math.floor((realSize.maxy-1)/this.__TILE_SIZE); y++ )
			{
				for( var x=startTileX; x <= Math.floor((realSize.maxx-1)/this.__TILE_SIZE); x++ )
				{
					if( typeof this.__renderedTiles[x+"/"+y] !== "undefined" ) {
						continue;
					}
					var xOffset = ((x-this.__startPos[0])*this.__TILE_SIZE-(this.__startSectorPos[0]-1)%this.__TILE_SIZE)*Starmap.__SECTOR_IMAGE_SIZE;
					var yOffset = ((y-this.__startPos[1])*this.__TILE_SIZE-(this.__startSectorPos[1]-1)%this.__TILE_SIZE)*Starmap.__SECTOR_IMAGE_SIZE;
					newTiles += "<img style=\"left:"+xOffset+"px;top:"+yOffset+"px;\" src=\""+url+"?module=map&action=tile&sys="+this.__systemId+"&tileX="+x+"&tileY="+y+"\" />";
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

/**
 * Das Overlay der Sternenkarte beinhaltet alle scanbaren Sektoren (LRS).
 * Zum fuellen des Overlays erfolgen bei Bedarf AJAX-Requests.
 */
var StarmapOverlay = function(data, screen) {
	this.__currentShiftOffset = [0,0];
	this.__currentSize = data.size;
	this.__screen = screen;
	this.__reloadTriggered = false;
	this.__currentSystem = data.system;
	this.__currentLocations = data.locations;
	
	this.__reloadOverlay = function()
	{
		var realSize = {
				minx:this.__currentSize.minx, 
				maxx:this.__currentSize.maxx, 
				miny:this.__currentSize.miny, 
				maxy:this.__currentSize.maxy};

		
		// Fehlende Bereiche identifieren und Ausschnitt vergroessern
		var mod = false;
		if( this.__currentShiftOffset[0] >= Starmap.__SECTOR_IMAGE_SIZE ) {
			var cnt = Math.ceil(this.__currentShiftOffset[0] / Starmap.__SECTOR_IMAGE_SIZE);
			realSize.minx -= cnt;
			realSize.maxx = realSize.minx + this.__screen.widthInSectors() + 1;
			mod = true;
		}
		else if( this.__screen.widthGap(this.__currentShiftOffset[0], this.__currentSize.minx, this.__currentSize.maxx) > 0 ) {
			var gap = this.__screen.widthGap(this.__currentShiftOffset[0], this.__currentSize.minx, this.__currentSize.maxx);
			realSize.maxx += gap;
			var newmin = Math.max(1,realSize.maxx - this.__screen.widthInSectors() - 1);
			realSize.minx = newmin;
			mod = true;
		}
		
		if( this.__currentShiftOffset[1] >= Starmap.__SECTOR_IMAGE_SIZE ) {
			var cnt = Math.ceil(this.__currentShiftOffset[1] / Starmap.__SECTOR_IMAGE_SIZE);
			realSize.miny -= cnt;
			realSize.maxy = realSize.miny + this.__screen.heightInSectors() + 1;
			mod = true;
		}
		else if( this.__screen.heightGap(this.__currentShiftOffset[1], this.__currentSize.miny, this.__currentSize.maxy) > 0 ) {
			var gap = this.__screen.heightGap(this.__currentShiftOffset[1], this.__currentSize.miny, this.__currentSize.maxy);
			realSize.maxy += gap;
			var newmin = Math.max(1,realSize.maxy - this.__screen.heightInSectors() - 1);
			realSize.miny = newmin;
			mod = true;
		}
		
		if( mod ) {
			var self = this;
			$.getJSON(Starmap.__getDsUrl(),
					{'sys': this.__currentSystem.id, 'xstart' : realSize.minx, 'xend' : realSize.maxx, 'ystart' : realSize.miny, 'yend' : realSize.maxy, 'loadmap' : '1', 'module': 'map', 'action' : 'map'},
					function(data) {
						self.__updateShiftOffset(data.size);
						self.__currentLocations = data.locations;
						self.__currentSize = data.size;
						var overlay = self.__renderOverlay(data);
						$('#tileOverlay').remove();
						$('#mapview').append(overlay);
						self.__reloadTriggered = false;
					});
		}

		this.__reloadTriggered = false;
	};
	
	this.__updateShiftOffset = function(newSize) {
		//if( newSize.minx > this.__currentSize.minx ) {
			this.__currentShiftOffset[0] -= (this.__currentSize.minx-newSize.minx)*Starmap.__SECTOR_IMAGE_SIZE;
		//}
		this.__currentShiftOffset[1] -= (this.__currentSize.miny-newSize.miny)*Starmap.__SECTOR_IMAGE_SIZE;
	};
	
	this.__renderOverlay = function(data)
	{
		var overlay = "<div id='tileOverlay' style='left:"+(this.__currentShiftOffset[0])+"px;top:"+(this.__currentShiftOffset[1])+"px'>";
		for( var i=0; i < data.locations.length; i++ ) {
			var loc = data.locations[i];
			
			var posx = (loc.x-this.__currentSize.minx)*Starmap.__SECTOR_IMAGE_SIZE;
			var posy = (loc.y-this.__currentSize.miny)*Starmap.__SECTOR_IMAGE_SIZE;
			
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
	};
	
	this.onClick = function(x, y) {
		x -= this.__currentShiftOffset[0];
		y -= this.__currentShiftOffset[1];
		var sectorX = this.__currentSize.minx+Math.floor(x / Starmap.__SECTOR_IMAGE_SIZE);
		var sectorY = this.__currentSize.miny+Math.floor(y / Starmap.__SECTOR_IMAGE_SIZE);
		
		for( var i=0; i < this.__currentLocations.length; i++ )
		{
			var loc = this.__currentLocations[i];
			if( loc.x == sectorX && loc.y == sectorY )
			{
				if( loc.scanner != null )
				{
					Starmap.showSector(this.__currentSystem, sectorX, sectorY, loc.scanner);
				}
				break;
			}
		}
	};
	
	this.prepareShift = function(moveX, moveY) {
		this.__currentShiftOffset = [this.__currentShiftOffset[0]-moveX, this.__currentShiftOffset[1]-moveY];
		
		if( !this.__reloadTriggered ) {
			this.__reloadTriggered = true;
			var self = this;
			setTimeout(function() {self.__reloadOverlay();}, 500);
		}
	};
	
	this.storeShift = function(cl) {
		var overlay = cl.find('#tileOverlay');
		overlay.css({'left' : (this.__currentShiftOffset[0])+'px', 'top' : (this.__currentShiftOffset[1])+'px'});
	};
	
	var content = this.__renderOverlay(data);
	$('#mapview').append(content);
};

var Starmap = {
	__SECTOR_IMAGE_SIZE:25,
	__currentSize:{minx:0,maxx:0,miny:0,maxy:0},
	__currentSystem:null,
	__screen:null,
	__currentShiftOffset:[0,0],
	__actionOverlay:null,
	__starmapLegend:null,
	__starmapTiles:null,
	__starmapOverlay:null,
	__dataPath:null,
	__getDsUrl : function() {
		var url = location.href;
		if( url.indexOf('?') > -1 ) {
			url = url.substring(0, url.indexOf('?'));
		}
		return url;
	},
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
		if( $('#mapview').size() == 0 ) {
			$('#mapcontent').append('<div id="mapview" />');
		}
		
		var sys = document.mapform.sys.value;
		var x = document.mapform.xstart.value;
		var y = document.mapform.ystart.value;
	
		var width = Math.floor(($('#mapview').width())/this.__SECTOR_IMAGE_SIZE);
		var height = Math.floor(($('#mapview').height())/this.__SECTOR_IMAGE_SIZE);
		
		var xstart = Math.max(1, x-Math.floor(width/2));
		var ystart = Math.max(1, y-Math.floor(height/2));
		
		this.hideSystemSelection();
		this.showLoader();
		this.clearMap();
		
		var self = this;
		
		$.getJSON(this.__getDsUrl(),
			{'sys': sys, 'xstart' : xstart, 'xend' : xstart+width, 'ystart' : ystart, 'yend' : ystart+height, 'loadmap' : '1', 'module': 'map', 'action' : 'map'},
			function(data) {
				self.renderMap(data);
				self.hideLoader();
			});
		
		$(window).bind('resize', function() {
			if( self.__screen === null ) {
				return;
			}
			
			self.__screen.update();
			self.__onMove(0,0);
		});
		
		return false;
	},
	clearMap : function()
	{
		$('#legend').remove();
		$('#tileOverlay').remove();
		$('#tiles').remove();
		$('#actionOverlay').remove();
		this.__currentShiftOffset = [0,0];
	},
	renderMap : function(data)
	{
		this.__dataPath = data.dataPath;
		this.__currentSize = data.size;
		this.__currentSystem = data.system;
		this.__screen = new StarmapScreen('#mapview', this.__currentSystem);
		
		var map = $('#mapcontent');

		this.__starmapLegend = new StarmapLegend(this.__screen, this.__currentSize);
		this.__starmapTiles = new StarmapTiles(this.__currentSystem.id, this.__screen, this.__currentSize);
		this.__starmapOverlay = new StarmapOverlay(data, this.__screen);
		
		var self = this;
		this.__actionOverlay = new StarmapActionOverlay({
			onClick : function(x, y) {
				self.__starmapOverlay.onClick(x, y);
			},
			onDragStart : function() {
			},
			onDrag : function(moveX, moveY) {
				self.__onMove(moveX, moveY);
			}
		});

		this.__updateJumpnodeList(data);
	},
	__onMove : function(moveX, moveY) {
		var newOff = [this.__currentShiftOffset[0]-moveX, this.__currentShiftOffset[1]-moveY];
		
		if( newOff[0] > (this.__currentSize.minx-1)*this.__SECTOR_IMAGE_SIZE ) {
			newOff[0] = (this.__currentSize.minx-1)*this.__SECTOR_IMAGE_SIZE
		}
		else if( newOff[0] < 0 && -newOff[0] > (this.__currentSystem.width*this.__SECTOR_IMAGE_SIZE-this.__screen.width()) ) {
			newOff[0] = -(this.__currentSystem.width*this.__SECTOR_IMAGE_SIZE-this.__screen.width());
		}
		
		if( newOff[1] > (this.__currentSize.miny-1)*this.__SECTOR_IMAGE_SIZE ) {
			newOff[1] = (this.__currentSize.miny-1)*this.__SECTOR_IMAGE_SIZE;
		}
		else if( newOff[1] < 0 && -newOff[1] > this.__currentSystem.height*this.__SECTOR_IMAGE_SIZE-this.__screen.height() ) {
			newOff[1] = -(this.__currentSystem.height*this.__SECTOR_IMAGE_SIZE-this.__screen.height());
		}

		this.__starmapTiles.prepareShift(this.__currentShiftOffset[0]-newOff[0], this.__currentShiftOffset[1]-newOff[1]);
		this.__starmapLegend.prepareShift(this.__currentShiftOffset[0]-newOff[0], this.__currentShiftOffset[1]-newOff[1]);
		this.__starmapOverlay.prepareShift(this.__currentShiftOffset[0]-newOff[0], this.__currentShiftOffset[1]-newOff[1]);
		
		var cl = $('#mapview');//.clone(true);
		
		this.__starmapLegend.storeShift();
		this.__starmapTiles.storeShift(cl);
		this.__starmapOverlay.storeShift(cl);
	
		//$('#mapview').remove();
		//$('#mapcontent').append(cl);
		if( isNaN(newOff[0]) || isNaN(newOff[1]) ) {
			throw "NaN";
		}
		
		this.__currentShiftOffset = newOff;
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
	__createSectorView : function()
	{
		if( $('#sectortable').size() == 0 )
		{
			var sectorview = "<div class='invisible gfxbox' id='sectortable' style='width:400px'>";
			sectorview += "<div id='sectorview'>";
			//Text is inserted here - using javascript
			sectorview += "</div>";
			sectorview += "</div>";
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
		$.getJSON(this.__getDsUrl(), 
			 {sys: system.id, x: x, y: y, scanship: scanShip, module: 'map', action:'sector'},
			 function(data)
			 {
				 self.openSector(system.id, x, y, data);
			 });
	},

	openSector : function (system, x, y, data)
	{
		var sector = $('#sectorview');
		var dialog = '<div class="content"><span>Sektor ' + system + ':' + x + '/' + y + '</span><a onclick="Starmap.closeSector()" style="float:right;color:#ff0000;">(x)</a>';
		if( data.bases.length > 0 ) {
			var self = this;
			dialog += "<ul class='bases'>";
			$.each(data.bases, function() {
				dialog+= "<li><img src='"+self.__dataPath+"kolonie"+this.klasse+"_srs.png' /><div class='name'>"+this.name+"</div><div class='owner'>"+this.username+"</div></li>";
			});
			dialog += "</ul>";
		}
		dialog += "<div class='ships'>"
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
		dialog += '</div></div>';
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