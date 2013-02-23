var Starmap = function(jqElement) {
	var SECTOR_IMAGE_SIZE = 25;

	/**
	 * Hilfsklasse zur Verwaltung des momentanen Anzeigebereichs (Kartengroesse).
	 * Die Groesse ist dabei durch den Anzeigebereich bestimmt, jedoch beschraenkt auf
	 * die maximale Groesse des dargestellten Sternensystems.
	 * @param {String} selector Der JQuery-Selektor zur Ermittlung des als Anzeigebereich verwendeten Elements
	 * @param {Object} system Das Sternensystem-Objekt mit den Breiten- und Hoehenangaben des Sternensystems
	 */
	var Screen = function(selector, system) {
		this.__screen = [0,0];
		this.__currentSystem = system;
		this.__max = [this.__currentSystem.width*SECTOR_IMAGE_SIZE, this.__currentSystem.height*SECTOR_IMAGE_SIZE];
		this.__selector = selector;

		/**
		 * Gibt die Breite des dargestellten Bereichs der Sternenkarte zurueck.
		 * @return {Number} Die Breite in px
		 */
		this.width = function() {
			return Math.min(this.__screen[0], this.__max[0]);
		};
		/**
		 * Gibt die Hoehe des dargestellten Bereichs der Sternenkarte zurueck.
		 * @return {Number} Die Hoehe in px
		 */
		this.height = function() {
			return Math.min(this.__screen[1], this.__max[1]);
		};
		/**
		 * Gibt die Breite des dargestellten Bereichs der Sternenkarte in Sektoren zurueck.
		 * Teilweise dargestellte Sektoren zaehlen vollstaendig mit.
		 * @return {Number} Die Anzahl der Sektoren
		 */
		this.widthInSectors = function() {
			return Math.min(Math.ceil(this.__screen[0] / SECTOR_IMAGE_SIZE), this.__currentSystem.width);
		};
		/**
		 * Gibt die Hoehe des dargestellten Bereichs der Sternenkarte in Sektoren zurueck.
		 * Teilweise dargestellte Sektoren zaehlen vollstaendig mit.
		 * @return {Number} Die Anzahl der Sektoren
		 */
		this.heightInSectors = function() {
			return Math.min(Math.ceil(this.__screen[1] / SECTOR_IMAGE_SIZE), this.__currentSystem.height);
		};
		/**
		 * Gibt die Anzahl der zur Fuellung des dargestellten Bereichs der Sternenkarte in der Breite noch fehlenden
		 * Sektoren zurueck. Dazu wird ein ueber offset sowie Start- und Zielsektor (x) zu ueberpruefender
		 * Bereich gegen die tatsaechliche Breite der Darstellung geprueft. Es wird nicht ueberprueft
		 * ob durch das offset selbst eine Luecke in der Darstellung entsteht (offset > 0).
		 * @param {Number} offset Das zu verwendende x-Offset (< 0)
		 * @param {Number} minx Der Startsektor (x) des Bereichs
		 * @param {Number} maxx Der Endsektor (x) des Bereichs
		 * @return {Number} Die zur Fuellung des Anzeigebereichs noch fehlenden Sektoren oder 0
		 */
		this.widthGap = function(offset, minx, maxx) {
			var gap = this.width() - (offset + (maxx-minx)*SECTOR_IMAGE_SIZE);
			if( gap > 0 ) {
				return Math.ceil(gap/SECTOR_IMAGE_SIZE);
			}
			return 0;
		};
		/**
		 * Gibt die Anzahl der zur Fuellung des dargestellten Bereichs der Sternenkarte in der Hoehe noch fehlenden
		 * Sektoren zurueck. Dazu wird ein ueber offset sowie Start- und Zielsektor (y) zu ueberpruefender
		 * Bereich gegen die tatsaechliche Hoehe der Darstellung geprueft. Es wird nicht ueberprueft
		 * ob durch das offset selbst eine Luecke in der Darstellung entsteht (offset > 0).
		 * @param {Number} offset Das zu verwendende y-Offset (< 0)
		 * @param {Number} miny Der Startsektor (y) des Bereichs
		 * @param {Number} maxy Der Endsektor (y) des Bereichs
		 * @return {Number} Die zur Fuellung des Anzeigebereichs noch fehlenden Sektoren oder 0
		 */
		this.heightGap = function(offset, miny, maxy) {
			var gap = this.height() - (offset + (maxy-miny)*SECTOR_IMAGE_SIZE);
			if( gap > 0 ) {
				return Math.ceil(gap/SECTOR_IMAGE_SIZE);
			}
			return 0;
		};

		/**
		 * Aktualisiert die Breiten- und Hoeheninformationen des Anzeigebereichs.
		 * Diese Methode ist aufzurufen wenn sich die Groesse des Anzeigebereichs
		 * aendert.
		 */
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
	var ActionOverlay = function(options){
		this.__lastDrag = [];

		this.create = function()
		{
			var overlay = "<div id='actionOverlay' />";

			$('#mapview').append(overlay);

			var self = this;
			var actionOverlay = $('#actionOverlay');
			actionOverlay.bind('click', function(e)
			{
				document.body.focus();
				document.onselectstart = function () { return false; };

				var offset = $(e.target).offset();
				var x = e.pageX - offset.left;
				var y = e.pageY - offset.top;

				e.stopPropagation();

				self.onClick(x, y);
			});

			actionOverlay.bind('mousedown', function(e) {
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
			actionOverlay.bind('touchstart', function(e) {
				e.stopPropagation();
				self.__lastDrag = [e.originalEvent.touches[0].pageX, e.originalEvent.touches[0].pageY];

				e.target.ondragstart = function() { return false; };
				document.body.focus();
				document.onselectstart = function () { return false; };

				self.onDragStart();
			});

			actionOverlay.bind('mouseup mouseout touchend', function(e) {
				self.__lastDrag = [];
				e.stopPropagation();

				self.onDragStop();
			});

			actionOverlay.bind('mousemove touchmove', function(e)
			{
				var drag = self.__lastDrag;
				if( typeof drag === "undefined" || drag.length == 0 ) {
					return;
				}

				if( e.type == 'touchmove' ) {
					e.preventDefault();
				}

				document.body.focus();
				document.onselectstart = function () { return false; };

				e.stopPropagation();
				var pageX, pageY;
				if( typeof e.originalEvent.touches !== 'undefined' ) {
					pageX = e.originalEvent.touches[0].pageX;
					pageY = e.originalEvent.touches[0].pageY;
				}
				else {
					pageX = e.pageX;
					pageY = e.pageY;
				}

				var moveX = drag[0] - pageX;
				var moveY = drag[1] - pageY;

				self.onDrag(moveX, moveY);

				self.__lastDrag = [pageX, pageY];
			});
		};

		this.create();

		this.onClick = options.onClick;
		this.onDragStart = options.onDragStart;
		this.onDrag = options.onDrag;
		this.onDragStop = options.onDragStop;

		if( typeof this.onClick === "undefined" ) {
			this.onClick = function() {};
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
	var Legend = function(screen, mapSize) {
		this.__screen = screen;
		this.__currentShiftOffset = [0, 0];
		this.__size = {minx:0, miny:0, maxx:0, maxy:0};

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

			if( this.__currentShiftOffset[0] >= SECTOR_IMAGE_SIZE ) {
				this.__currentShiftOffset[0] -= SECTOR_IMAGE_SIZE;

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

			if( this.__currentShiftOffset[1] >= SECTOR_IMAGE_SIZE ) {
				this.__currentShiftOffset[1] -= SECTOR_IMAGE_SIZE;

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

		jqElement.append(this.__renderLegend(mapSize));
	};
	/**
	 * Der Hintergrund (Tiles) der Sternenkarte. Dieser besteht aus einzelnen Kacheln.
	 * Die Identifikation der Kacheln geschieht allein clientseitig.
	 */
	var Tiles = function(systemId, screenSize, mapSize) {
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
			var url = DS.getUrl();
			var tiles = '<div id="tiles">';
			var startTileY = Math.floor((mapSize.miny-1)/this.__TILE_SIZE);
			var startTileX = Math.floor((mapSize.minx-1)/this.__TILE_SIZE);
			for( var y=startTileY; y <= Math.floor((mapSize.maxy-1)/this.__TILE_SIZE); y++ )
			{
				for( var x=startTileX; x <= Math.floor((mapSize.maxx-1)/this.__TILE_SIZE); x++ )
				{
					var xOffset = ((x-startTileX)*this.__TILE_SIZE-(mapSize.minx-1)%this.__TILE_SIZE)*SECTOR_IMAGE_SIZE;
					var yOffset = ((y-startTileY)*this.__TILE_SIZE-(mapSize.miny-1)%this.__TILE_SIZE)*SECTOR_IMAGE_SIZE;
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
			if( this.__currentShiftOffset[0] >= SECTOR_IMAGE_SIZE ) {
				var cnt = Math.ceil(this.__currentShiftOffset[0] / SECTOR_IMAGE_SIZE);
				//this.__currentShiftOffset[0] -= cnt*SECTOR_IMAGE_SIZE;
				realSize.minx = this.__startSectorPos[0]-cnt;
				mod = true;
			}
			else if( this.__currentShiftOffset[0] + (this.__size.maxx-this.__size.minx)*this.__TILE_SIZE < this.__screen.width() ) {
				var gap = this.__screen.width() - (this.__currentShiftOffset[0] + (this.__size.maxx-this.__size.minx)*this.__TILE_SIZE);
				realSize.maxx += Math.ceil(gap / SECTOR_IMAGE_SIZE);
				mod = true;
			}

			if( this.__currentShiftOffset[1] >= SECTOR_IMAGE_SIZE ) {
				var cnt = Math.ceil(this.__currentShiftOffset[1] / SECTOR_IMAGE_SIZE);
				//this.__currentShiftOffset[1] -= cnt*SECTOR_IMAGE_SIZE;
				realSize.miny = this.__startSectorPos[1]-cnt;
				mod = true;
			}
			else if( this.__currentShiftOffset[1] + (this.__size.maxy-this.__size.miny)*this.__TILE_SIZE < this.__screen.height() ) {
				var gap = this.__screen.height() - (this.__currentShiftOffset[1] + (this.__size.maxy-this.__size.miny)*this.__TILE_SIZE);
				realSize.maxy += Math.ceil(gap / SECTOR_IMAGE_SIZE);
				mod = true;
			}

			if( mod ) {
				var newTiles = "";
				var url = DS.getUrl();
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
						var xOffset = ((x-this.__startPos[0])*this.__TILE_SIZE-(this.__startSectorPos[0]-1)%this.__TILE_SIZE)*SECTOR_IMAGE_SIZE;
						var yOffset = ((y-this.__startPos[1])*this.__TILE_SIZE-(this.__startSectorPos[1]-1)%this.__TILE_SIZE)*SECTOR_IMAGE_SIZE;
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
	var Overlay = function(data, screen, request) {
		this.__currentShiftOffset = [0,0];
		this.__currentSize = data.size;
		this.__screen = screen;
		this.__reloadTriggered = false;
		this.__currentSystem = data.system;
		this.__currentLocations = data.locations;
		this.__request = request;
		if( typeof this.__request === 'undefined' ) {
			this.__request = {};
		}

		this.__reloadOverlay = function()
		{
			var realSize = {
				minx:this.__currentSize.minx,
				maxx:this.__currentSize.maxx,
				miny:this.__currentSize.miny,
				maxy:this.__currentSize.maxy};


			// Fehlende Bereiche identifieren und Ausschnitt vergroessern
			var mod = false;
			if( this.__currentShiftOffset[0] >= SECTOR_IMAGE_SIZE ) {
				var cnt = Math.ceil(this.__currentShiftOffset[0] / SECTOR_IMAGE_SIZE);
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

			if( this.__currentShiftOffset[1] >= SECTOR_IMAGE_SIZE ) {
				var cnt = Math.ceil(this.__currentShiftOffset[1] / SECTOR_IMAGE_SIZE);
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
				var request = this.__request;
				request.sys = this.__currentSystem.id;
				request.xstart = realSize.minx;
				request.xend = realSize.maxx;
				request.ystart = realSize.miny;
				request.yend = realSize.maxy;
				request.loadmap = 1;
				request.module = 'map';
				request.action = 'map';
				$.getJSON(DS.getUrl(),
					request,
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
			this.__currentShiftOffset[0] -= (this.__currentSize.minx-newSize.minx)*SECTOR_IMAGE_SIZE;
			//}
			this.__currentShiftOffset[1] -= (this.__currentSize.miny-newSize.miny)*SECTOR_IMAGE_SIZE;
		};

		this.__renderOverlay = function(data)
		{
			var overlay = "<div id='tileOverlay' style='left:"+(this.__currentShiftOffset[0])+"px;top:"+(this.__currentShiftOffset[1])+"px'>";
			for( var i=0; i < data.locations.length; i++ ) {
				var loc = data.locations[i];

				var posx = (loc.x-this.__currentSize.minx)*SECTOR_IMAGE_SIZE;
				var posy = (loc.y-this.__currentSize.miny)*SECTOR_IMAGE_SIZE;

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

		/**
		 * Gibt die zum angegebenen Sektor vorhandenen Informationen zurueck.
		 * @param {Number} sectorX Die x-Koordinate des Sektors
		 * @param {Number} sectorY Die Y-Koordinate des Sektors
		 * @return {Object} Die Sektorinformationen oder null
		 */
		this.getSectorInformation = function(sectorX,sectorY) {
			for( var i=0; i < this.__currentLocations.length; i++ )
			{
				var loc = this.__currentLocations[i];
				if( loc.x == sectorX && loc.y == sectorY )
				{
					return loc;
				}
			}
			return null;
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


	/**
	 * Erzeugt ein Loader-Popup im versteckten Zustand.
	 * @constructor
	 */
	var LoaderPopup = function() {
		var createLoaderPopup  = function()
		{
			if( $('#starmaploader').size() == 0 )
			{
				var sectorview = "<div id='starmaploader'>";
				//Text is inserted here - using javascript
				sectorview += "</div>";
				jqElement.append(sectorview);
				$('#starmaploader').dsBox({
					width:400,
					center:true,
					closeButton:false
				})
			}
		};

		createLoaderPopup();

		/**
		 * Zeigt das Loader-Popup an.
		 */
		this.show = function() {
			$('#starmaploader .content').html("Verbinde mit interplanetarem Überwachungsnetzwerk...<br /><img src='./data/interface/ajax-loader.gif' alt='Lade' />");
			$('#starmaploader').dsBox('show')
		};
		/**
		 * Verbirgt das Loader-Popup wieder.
		 */
		this.hide = function() {
			$('#starmaploader').dsBox('hide')
		};
	};

	var __currentSize = {minx:0,maxx:0,miny:0,maxy:0};
	var __currentSystem = null;
	var __screen = null;
	var __currentShiftOffset = [0,0];
	var __actionOverlay = null;
	var __starmapLegend = null;
	var __starmapTiles = null;
	var __starmapOverlay = null;
	var __dataPath = null;
	var __request = null;
	var __ready = false;

	function load(sys,x,y,options)
	{
		jqElement.addClass('starmap');

		if( typeof options === 'undefined' ) {
			options = {};
		}
		if( $('#mapview').size() == 0 ) {
			jqElement.append('<div id="mapview" />');
		}

		var mapview = $('#mapview');
		var width = Math.floor((mapview.width())/SECTOR_IMAGE_SIZE);
		var height = Math.floor((mapview.height())/SECTOR_IMAGE_SIZE);

		var xstart = Math.max(1, 1-Math.floor(width/2));
		var ystart = Math.max(1, 1-Math.floor(height/2));

		onSystemLoad();
		clearMap();

		var request = options.request;
		if( typeof request === 'undefined' ) {
			request = {};
		}
		__request = request;

		request.sys = sys;
		request.xstart = xstart;
		request.xend = xstart+width;
		request.ystart = ystart;
		request.yend = ystart+height;
		request.loadmap = 1;
		request.module = 'map';
		request.action = 'map';

		$.getJSON(DS.getUrl(),
			request,
			function(data) {
				renderMap(data, options);
				gotoLocation(x,y);
				onSystemLoaded();
				if( typeof options.loadCallback !== 'undefined' ) {
					options.loadCallback(data);
				}
			});

		$(window).bind('resize', function() {
			if( self.__screen === null ) {
				return;
			}

			__screen.update();
			__onMove(0,0);
		});

		return false;
	};

	function gotoLocation(x, y) {
		var mapview = $('#mapview');
		var width = Math.floor((mapview.width())/SECTOR_IMAGE_SIZE);
		var height = Math.floor((mapview.height())/SECTOR_IMAGE_SIZE);

		var targetX = Math.max(1, x-Math.floor(width/2));
		var targetY = Math.max(1, y-Math.floor(height/2));

		if( targetX > __currentSystem.width ) {
			targetX = __currentSystem.width-width;
		}
		if( targetY > __currentSystem.height ) {
			targetY = __currentSystem.height-height;
		}
		if( targetX < 1 ) {
			targetX = 1;
		}
		if( targetY < 1 ) {
			targetY = 1;
		}

		__onMove((targetX-1)*SECTOR_IMAGE_SIZE+__currentShiftOffset[0],(targetY-1)*SECTOR_IMAGE_SIZE+__currentShiftOffset[1]);
	};

	function clearMap()
	{
		$('#legend').remove();
		$('#tileOverlay').remove();
		$('#tiles').remove();
		$('#actionOverlay').remove();
		__currentShiftOffset = [0,0];
	};
	function renderMap(data, options)
	{
		__dataPath = data.dataPath;
		__currentSize = data.size;
		__currentSystem = data.system;
		__screen = new Screen('#mapview', __currentSystem);

		__starmapLegend = new Legend(__screen, __currentSize);
		__starmapTiles = new Tiles(__currentSystem.id, __screen, __currentSize);
		__starmapOverlay = new Overlay(data, __screen, options.request);

		__actionOverlay = new ActionOverlay({
			onClick : function(x, y) {
				onClick(x,y);
			},
			onDragStart : function() {
			},
			onDrag : function(moveX, moveY) {
				__onMove(moveX, moveY);
			}
		});
	};
	function __onMove(moveX, moveY) {
		var newOff = [__currentShiftOffset[0]-moveX, __currentShiftOffset[1]-moveY];

		if( newOff[0] > (__currentSize.minx-1)*SECTOR_IMAGE_SIZE ) {
			newOff[0] = (__currentSize.minx-1)*SECTOR_IMAGE_SIZE
		}
		else if( newOff[0] < 0 && -newOff[0] > (__currentSystem.width*SECTOR_IMAGE_SIZE-__screen.width()) ) {
			newOff[0] = -(__currentSystem.width*SECTOR_IMAGE_SIZE-__screen.width());
		}

		if( newOff[1] > (__currentSize.miny-1)*SECTOR_IMAGE_SIZE ) {
			newOff[1] = (__currentSize.miny-1)*SECTOR_IMAGE_SIZE;
		}
		else if( newOff[1] < 0 && -newOff[1] > __currentSystem.height*SECTOR_IMAGE_SIZE-__screen.height() ) {
			newOff[1] = -(__currentSystem.height*SECTOR_IMAGE_SIZE-__screen.height());
		}

		__starmapTiles.prepareShift(__currentShiftOffset[0]-newOff[0], __currentShiftOffset[1]-newOff[1]);
		__starmapLegend.prepareShift(__currentShiftOffset[0]-newOff[0], __currentShiftOffset[1]-newOff[1]);
		__starmapOverlay.prepareShift(__currentShiftOffset[0]-newOff[0], __currentShiftOffset[1]-newOff[1]);

		var cl = $('#mapview');//.clone(true);

		__starmapLegend.storeShift();
		__starmapTiles.storeShift(cl);
		__starmapOverlay.storeShift(cl);

		if( isNaN(newOff[0]) || isNaN(newOff[1]) ) {
			throw "NaN";
		}

		__currentShiftOffset = newOff;
	};

	var __loaderPopup = null;
	function onSystemLoad()
	{
		if( __loaderPopup == null ) {
			__loaderPopup = new LoaderPopup();
		}
		__loaderPopup.show();
	};
	function onSystemLoaded()
	{
		__loaderPopup.hide();
		__ready = true;
	};
	function onClick(x,y) {
		x -= __currentShiftOffset[0];
		y -= __currentShiftOffset[1];
		var sectorX = __currentSize.minx+Math.floor(x / SECTOR_IMAGE_SIZE);
		var sectorY = __currentSize.miny+Math.floor(y / SECTOR_IMAGE_SIZE);
		var locationInfo = __starmapOverlay.getSectorInformation(sectorX, sectorY);
		if( locationInfo == null ) {
			return;
		}

		new StarmapSectorInfoPopup(__currentSystem, sectorX, sectorY, locationInfo, {
			request : __request
		});
	};

	// PUBLIC METHODS
	this.gotoLocation = gotoLocation;
	this.load = load;
	this.isReady = function() { return __ready };
};

/**
 * Erzeugt ein Popup das die Eingabe einer neuen Position erlaubt.
 * @param {function} callback Die Callbackmethode beim bestaetigen des Popups.
 * 			Erhaelt zwei Parameter, x und y.
 * @constructor
 */
function StarmapGotoLocationPopup(callback) {
	var __createPopup  = function()
	{
		if( $('#starmapGotoLocationPopup').size() == 0 )
		{
			var sectorview = '<div id="starmapGotoLocationPopup" class="gfxbox">'+
				'<form action="./ds" method="post" name="mapform">'+
				'<label>Position</label>'+
				'<input type="text" name="xstart" size="3" value="1" />/'+
				'<input type="text" name="ystart" size="3" value="1" />'+
				'<input type="submit" value="ok"/>'+
				'</form>'+
				'</div>';
			$('body').append(sectorview);
		}
	};

	var show = function() {
		__createPopup();

		var popup = $('#starmapGotoLocationPopup');
		popup.dialog({
			title: "Zur Position springen",
			height:100
		});
		var form = popup.find('form');
		form.off('submit')
			.on('submit', function(event) {
				event.preventDefault();

				popup.dialog('close');

				callback(form.find('input[name=xstart]').val(), form.find('input[name=ystart]').val());

				return false;
			});
	};

	show();
}

/**
 * Erzeugt ein neues Infopopup zu einem Sektor und zeigt dieses an.
 * @param {Object} system Das Sternensystem (Objekt)
 * @param {Number} x Die x-Koordinate des Sektors
 * @param {Number} y Die y-Koordinate des Sektors
 * @param {Object} locationInfo Die Informationen zum darzustellenden Sektor (Serverantwort)
 * @param {Object} options Die Optionalen Angaben zur Darstellung
 * @constructor
 */
function StarmapSectorInfoPopup(system, x, y, locationInfo, options) {
	var __createSectorView  = function()
	{
		if( $('#sectorview').size() == 0 )
		{
			var sectorview = "<div id='sectorview'>";
			//Text is inserted here - using javascript
			sectorview += "</div>";
			$('body').append(sectorview);
		}
	};

	var showSector = function (system, x, y, scanShip)
	{
		__createSectorView();

		$('#sectorview').dsBox('show', {
			center:true,
			width:400,
			draggable:true
		});

		$('#sectorview .content').html('Lade Sektor ' + system.id + ':' + x + '/' + y);
		var request = options.request;
		if( typeof request === 'undefined' ) {
			request = {};
		}
		request.sys = system.id;
		request.x = x;
		request.y = y;
		request.scanship = scanShip;
		request.module = 'map';
		request.action = 'sector';

		$.getJSON(DS.getUrl(),
			request,
			function(data)
			{
				openSector(system.id, x, y, data);
			});
	};

	var openSector = function (system, x, y, data)
	{
		var sector = $('#sectorview .content');
		var dialog = '<div class="header"><span>Sektor ' + system + ':' + x + '/' + y + '</span></div>';
		if( data.nebel ) {
			dialog += '<img class="nebel" src="data/objects/nebel'+data.nebel.type+'.png" alt="Nebel" />';
		}
		if( data.jumpnodes && data.jumpnodes.length > 0 ) {
			dialog += "<ul class='jumpnodes'>";
			$.each(data.jumpnodes, function() {
				dialog+= "<li><img src='./data/objects/node.png' /><div class='name'>"+this.name+(this.blocked ? ' - blockiert' : '')+"</div></li>";
			});
			dialog += "</ul>";
		}

		if( data.bases && data.bases.length > 0 ) {
			dialog += "<ul class='bases'>";
			$.each(data.bases, function() {
				dialog+= "<li><img src='./data/starmap/kolonie"+this.klasse+"_srs.png' />" +
					"<div class='name'>"+this.name+"</div>" +
					"<div class='typ'>"+this.typ+"</div>" +
					"<div class='owner'>"+this.username+"</div></li>";
			});
			dialog += "</ul>";
		}

		dialog += "<div class='ships'>"
		$.each(data.users, function()
		{
			var shipcount = 0;
			var shipclassId = this.id + '-shipclasses';
			var shiptypes = '<ul style="display:none" id="'+shipclassId+'" class="shipclasses">';
			$.each(this.shiptypes, function()
			{
				shiptypes += '<li><span>'+this.name+'</span><span style="float:right;">'+this.ships.length+'</span></li>';
				shipcount += this.ships.length;
			});
			shiptypes += '</ul>';
			dialog += '<span class="toggle" ds-shipclass="'+shipclassId+'"><span id="'+shipclassId+'Toggle">+</span> '+this.name+'</span><span style="float:right;">'+shipcount+'</span><br>';
			dialog += shiptypes;
		});
		dialog += '</div>';
		sector.html(dialog);
		sector.find('[ds-shipclass]').on('click', function() {
			toggleShowShipClasses($(this).attr('ds-shipclass'));
		});
	};

	var toggleShowShipClasses = function (shipclassId)
	{
		var data = $('#' + shipclassId);
		if( data.css('display') == 'none' ) {
			data.css('display', 'block');
		}
		else {
			data.css('display', 'none');
		}
		var node = $('#' + shipclassId+'Toggle');
		if( node.text() == '+' )
		{
			node.text('-');
		}
		else
		{
			node.text('+');
		}
	};

	if( locationInfo.scanner != null )
	{
		showSector(system, x, y, locationInfo.scanner);
	}
};
