'use strict';

angular.module('ds.starmap', ['ds.service.ds'])
	.factory('Starmap', ['dsMsg', function(dsMsg) {
/**
 * Erzeugt eine neue Sternenkarte an der Stelle des uebergebenen JQuery-Objekts.
 * @param jqElement Das JQuery-Objekt
 * @name Starmap
 * @constructor
 */
return function(jqElement) {
	/**
	 * Die Groesse eines Sektors der Sternenkarte in Pixel.
	 * @const
	 * @type {number}
	 */
	var SECTOR_IMAGE_SIZE = 25;
	var TILE_SIZE = 20;

	/**
	 * @name JsonSystemResponse
	 * @class JsonSystemResponse
	 * Die Antwort des Servers.
	 *
	 * @property {number} width Die Breite des Sternensystems in Sektoren.
	 * @property {number} height Die Höhe des Sternensystems in Sektoren.
	 */

	/**
	 * Hilfsklasse zur Verwaltung des momentanen Anzeigebereichs (Kartengroesse).
	 * Die Groesse ist dabei durch den Anzeigebereich bestimmt, jedoch beschraenkt auf
	 * die maximale Groesse des dargestellten Sternensystems.
	 * @constructor
	 * @param {String} selector Der JQuery-Selektor zur Ermittlung des als Anzeigebereich verwendeten Elements
	 * @param {JsonSystemResponse}system Das Sternensystem-Objekt mit den Breiten- und Hoehenangaben des Sternensystems
	 */
	var StarmapScreen = function(selector, system) {
		var __screen = [0,0];
		var __currentSystem = system;
		var __max = [__currentSystem.width*SECTOR_IMAGE_SIZE, __currentSystem.height*SECTOR_IMAGE_SIZE];
		var __selector = selector;

		/**
		 * Gibt die Breite des dargestellten Bereichs der Sternenkarte zurueck.
		 * @name StarmapScreen.width
		 * @return {Number} Die Breite in px
		 */
		this.width = function() {
			return Math.min(__screen[0], __max[0]);
		};
		/**
		 * Gibt die Hoehe des dargestellten Bereichs der Sternenkarte zurueck.
		 * @name StarmapScreen.height
		 * @return {Number} Die Hoehe in px
		 */
		this.height = function() {
			return Math.min(__screen[1], __max[1]);
		};
		/**
		 * Gibt die Breite des dargestellten Bereichs der Sternenkarte in Sektoren zurueck.
		 * Teilweise dargestellte Sektoren zaehlen vollstaendig mit.
		 * @name StarmapScreen.widthInSectors
		 * @return {Number} Die Anzahl der Sektoren
		 */
		this.widthInSectors = function() {
			return Math.min(Math.ceil(__screen[0] / SECTOR_IMAGE_SIZE), __currentSystem.width);
		};
		/**
		 * Gibt die Hoehe des dargestellten Bereichs der Sternenkarte in Sektoren zurueck.
		 * Teilweise dargestellte Sektoren zaehlen vollstaendig mit.
		 * @name StarmapScreen.heightInSectors
		 * @return {Number} Die Anzahl der Sektoren
		 */
		this.heightInSectors = function() {
			return Math.min(Math.ceil(__screen[1] / SECTOR_IMAGE_SIZE), __currentSystem.height);
		};
		/**
		 * Gibt die Anzahl der zur Fuellung des dargestellten Bereichs der Sternenkarte in der Breite noch fehlenden
		 * Sektoren zurueck. Dazu wird ein ueber offset sowie Start- und Zielsektor (x) zu ueberpruefender
		 * Bereich gegen die tatsaechliche Breite der Darstellung geprueft. Es wird nicht ueberprueft
		 * ob durch das offset selbst eine Luecke in der Darstellung entsteht (offset > 0).
		 * @name StarmapScreen.widthGap
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
		 * @name StarmapScreen.heightGap
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
		 * @name StarmapScreen.update
		 */
		this.update = function() {
			var el = $(__selector);
			__screen = [el.width(),el.height()];
		};

		this.update();
	};
	/**
	 * Das Aktionsoverlay der Sternenkarte. Nimmt Aktionen (Clicks, Drags) usw
	 * entgegen und berechnet entsprechende Events fuer die Sternenkarte.
	 * Die Eventhandler muessen durch die Sternenkarte ueberschrieben werden.
	 * @constructor
	 */
	var ActionOverlay = function(options){
		this.__lastDrag = [];
		var dragDistance = 0;

		this.create = function()
		{
			var overlay = "<div id='actionOverlay' />";

			$('#mapview').append(overlay);

			var self = this;
			var actionOverlay = $('#actionOverlay');
			/*actionOverlay.bind('click', function(e)
			{
				document.body.focus();
				document.onselectstart = function () { return false; };

				var offset = $(e.target).offset();
				var x = e.pageX - offset.left;
				var y = e.pageY - offset.top;

				e.stopPropagation();

				self.onClick(x, y);
			});*/

			actionOverlay.bind('mousedown', function(e) {
				if( e.which != 1 ) {
					return;
				}
				e.stopPropagation();
				self.__lastDrag = [e.pageX, e.pageY];

				e.target.ondragstart = function() { return false; };
				document.body.focus();
				document.onselectstart = function () { return false; };
				dragDistance = 0;

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

				if( dragDistance < 3 && e.type != 'mouseout') {
					var offset = $(e.target).offset();
					var x = e.pageX - offset.left;
					var y = e.pageY - offset.top;

					self.onClick(x, y);
				}
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

				dragDistance += Math.abs(moveX)+Math.abs(moveY);

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
	 * @constructor
	 */
	var Legend = function(screen, mapSize) {
		var __screen = screen;
		var __currentShiftOffset = [0, 0];
		var __size = {minx:0, miny:0, maxx:0, maxy:0};

		function __renderLegend(mapSize)
		{
			__size = {minx:mapSize.minx, maxx:mapSize.maxx, miny:mapSize.miny, maxy:mapSize.maxy};
			var legend = "<div id=\"legend\">";
			legend += "<div class='top'>";
			legend += "<div class='corner'>x/y</div>";
			legend += __printLegend("hbar", __size.minx, __size.maxx);
			legend += "</div>";

			legend += "<div class='left'>";
			legend += __printLegend("vbar", __size.miny, __size.maxy);
			legend += "</div>";

			legend += "<div class='right'>";
			legend += __printLegend("vbar", __size.miny, __size.maxy);
			legend += "</div>";

			legend += "<div class='bottom'>";
			legend += "<div class='corner'>x/y</div>";
			legend += __printLegend("hbar", __size.minx, __size.maxx);
			legend += "</div></div>";
			return legend;
		}

		function __printLegend(cls, start, end)
		{
			var legend = "<div class=\""+cls+"\"><div class=\"scroll\">";
			for(var x = start; x <= end; x++)
			{
				legend += "<div>"+x+"</div>";
			}
			legend += "</div></div>";
			return legend;
		}

		/**
		 * Bereitet eine Verschiebeoperation vor.
		 * @name Legend.prepareShift
		 * @param moveX {number} Der Offset in X-Richtung in Pixel
		 * @param moveY {number} Der Offset in Y-Richtung in Pixel
		 */
		this.prepareShift = function(moveX, moveY) {
			var $legend = $('#legend');
			var hbar = $legend.find('.hbar .scroll');
			var vbar = $legend.find('.vbar .scroll');

			__currentShiftOffset = [__currentShiftOffset[0]-moveX, __currentShiftOffset[1]-moveY];

			if( __currentShiftOffset[0] >= SECTOR_IMAGE_SIZE ) {
				__currentShiftOffset[0] -= SECTOR_IMAGE_SIZE;

				hbar.prepend("<div>"+(__size.minx-1)+"</div>");

				__size.minx -= 1;
			}
			else if( __screen.widthGap(__currentShiftOffset[0], __size.minx,__size.maxx) > 0 ) {
				var cntw = __screen.widthGap(__currentShiftOffset[0], __size.minx,__size.maxx);
				var contentw = "";
				for( var w=1; w <= cntw; ++w ) {
					contentw += "<div>"+(__size.maxx+w)+"</div>";
				}
				hbar.append(contentw);
				__size.maxx += cntw;
			}

			if( __currentShiftOffset[1] >= SECTOR_IMAGE_SIZE ) {
				__currentShiftOffset[1] -= SECTOR_IMAGE_SIZE;

				vbar.prepend("<div>"+(__size.miny-1)+"</div>");
				__size.miny -= 1;
			}
			else if( __screen.heightGap(__currentShiftOffset[1], __size.miny,__size.maxy) > 0 ) {
				var cnth = __screen.heightGap(__currentShiftOffset[1], __size.miny,__size.maxy);
				var contenth = "";
				for( var h=1; h <= cnth; ++h ) {
					contenth += "<div>"+(__size.maxy+h)+"</div>";
				}
				vbar.append(contenth);
				__size.maxy += cnth;
			}
		};
		/**
		 * Speichert die Verschiebeoperation und aktualisiert die Darstellung.
		 * @name Legend.storeShift
		 */
		this.storeShift = function() {
			var $legend = $('#legend');
			var hbar = $legend.find('.hbar .scroll');
			var vbar = $legend.find('.vbar .scroll');

			hbar.css('left' , (__currentShiftOffset[0])+'px');
			vbar.css('top' , (__currentShiftOffset[1])+'px');
		};

		jqElement.prepend(__renderLegend(mapSize));
	};
	/**
	 * Der Hintergrund (Tiles) der Sternenkarte. Dieser besteht aus einzelnen Kacheln.
	 * Die Identifikation der Kacheln geschieht allein clientseitig.
	 * @constructor
	 * @param {Number} systemId Die ID des darzustellenden Sternensystems
	 * @param {StarmapScreen} screenSize
	 * @param {Object} mapSize
	 */
	var Tiles = function(systemId, screenSize, mapSize) {
		this.__screen = screenSize;
		this.__currentShiftOffset = [0, 0];
		this.__size = {minx:mapSize.minx, maxx:mapSize.maxx, miny:mapSize.miny, maxy:mapSize.maxy};
		this.__systemId = systemId;
		this.__renderedTiles = [];
		this.__startPos = [Math.floor((mapSize.minx-1)/TILE_SIZE), Math.floor((mapSize.miny-1)/TILE_SIZE)];
		this.__startSectorPos = [mapSize.minx, mapSize.miny];

		this.__renderTiles = function(mapSize)
		{
			var url = DS.getUrl();
			var tiles = '<div id="tiles">';
			var startTileY = Math.floor((mapSize.miny-1)/TILE_SIZE);
			var startTileX = Math.floor((mapSize.minx-1)/TILE_SIZE);
			for( var y=startTileY; y <= Math.floor((mapSize.maxy-1)/TILE_SIZE); y++ )
			{
				for( var x=startTileX; x <= Math.floor((mapSize.maxx-1)/TILE_SIZE); x++ )
				{
					var xOffset = ((x-startTileX)*TILE_SIZE-(mapSize.minx-1)%TILE_SIZE)*SECTOR_IMAGE_SIZE;
					var yOffset = ((y-startTileY)*TILE_SIZE-(mapSize.miny-1)%TILE_SIZE)*SECTOR_IMAGE_SIZE;
					tiles += "<img style=\"left:"+xOffset+"px;top:"+yOffset+"px;\" src=\""+url+"?module=map&action=tile&sys="+this.__systemId+"&tileX="+x+"&tileY="+y+"\" />";
					this.__renderedTiles[x+"/"+y] = true;
				}
			}
			tiles += "</div>";
			return tiles;
		};

		/**
		 * Bereitet eine Verschiebeoperation vor.
		 * @name Tiles.prepareShift
		 * @param moveX {number} Der Offset in X-Richtung in Pixel
		 * @param moveY {number} Der Offset in Y-Richtung in Pixel
		 */
		this.prepareShift = function(moveX, moveY) {
			var tiles = $('#tiles');
			if( isNaN(moveX) || isNaN(this.__currentShiftOffset[0]) ) {
				throw "NaN";
			}

			this.__currentShiftOffset = [this.__currentShiftOffset[0]-moveX, this.__currentShiftOffset[1]-moveY];

			var realSize = {minx:this.__size.minx, maxx:this.__size.maxx, miny:this.__size.miny, maxy:this.__size.maxy};

			var mod = false;
			if( this.__currentShiftOffset[0] >= SECTOR_IMAGE_SIZE ) {
				var cntw = Math.ceil(this.__currentShiftOffset[0] / SECTOR_IMAGE_SIZE);
				realSize.minx = this.__startSectorPos[0]-cntw;
				mod = true;
			}
			else if( this.__currentShiftOffset[0] + (this.__size.maxx-this.__size.minx)*TILE_SIZE < this.__screen.width() ) {
				var gapw = this.__screen.width() - (this.__currentShiftOffset[0] + (this.__size.maxx-this.__size.minx)*TILE_SIZE);
				realSize.maxx += Math.ceil(gapw / SECTOR_IMAGE_SIZE);
				mod = true;
			}

			if( this.__currentShiftOffset[1] >= SECTOR_IMAGE_SIZE ) {
				var cnth = Math.ceil(this.__currentShiftOffset[1] / SECTOR_IMAGE_SIZE);
				//this.__currentShiftOffset[1] -= cnt*SECTOR_IMAGE_SIZE;
				realSize.miny = this.__startSectorPos[1]-cnth;
				mod = true;
			}
			else if( this.__currentShiftOffset[1] + (this.__size.maxy-this.__size.miny)*TILE_SIZE < this.__screen.height() ) {
				var gaph = this.__screen.height() - (this.__currentShiftOffset[1] + (this.__size.maxy-this.__size.miny)*TILE_SIZE);
				realSize.maxy += Math.ceil(gaph / SECTOR_IMAGE_SIZE);
				mod = true;
			}

			if( mod ) {
				var newTiles = "";
				var url = DS.getUrl();
				var startTileY = Math.floor((realSize.miny-1)/TILE_SIZE);
				var startTileX = Math.floor((realSize.minx-1)/TILE_SIZE);
				if( startTileX < 0 ) {
					throw "OutOfRange";
				}
				for( var y=startTileY; y <= Math.floor((realSize.maxy-1)/TILE_SIZE); y++ )
				{
					for( var x=startTileX; x <= Math.floor((realSize.maxx-1)/TILE_SIZE); x++ )
					{
						if( typeof this.__renderedTiles[x+"/"+y] !== "undefined" ) {
							continue;
						}
						var xOffset = ((x-this.__startPos[0])*TILE_SIZE-(this.__startSectorPos[0]-1)%TILE_SIZE)*SECTOR_IMAGE_SIZE;
						var yOffset = ((y-this.__startPos[1])*TILE_SIZE-(this.__startSectorPos[1]-1)%TILE_SIZE)*SECTOR_IMAGE_SIZE;
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
	 * @constructor
	 * @param {Object} data Die Serverantwort
	 * @param {StarmapScreen} screen Die Informationen zum Darstellungsbereich
	 * @param {Object} request Die fuer Requests zu verwendenden Daten
	 */
	var Overlay = function(data, screen, request) {
		var __currentShiftOffset = [0,0];
		var __currentSize = data.size;
		var __screen = screen;
		var __reloadTriggered = false;
		var __currentSystem = data.system;
		var __currentLocations = data.locations;
		var __request = request;
		if( typeof __request === 'undefined' ) {
			__request = {};
		}

		function doServerRequest(realSize) {
			var request = __request;
			request.FORMAT = 'JSON';
			request.sys = __currentSystem.id;
			request.xstart = realSize.minx;
			request.xend = realSize.maxx;
			request.ystart = realSize.miny;
			request.yend = realSize.maxy;
			request.loadmap = 1;
			request.module = 'map';
			request.action = 'map';
			$.getJSON(DS.getUrl(),
				request,
				function (data) {
					dsMsg(data);
					__updateShiftOffset(data.size);
					__currentLocations = data.locations;
					__currentSize = data.size;
					var overlay = $(__renderOverlay(data));
					var oldOverlay = $('#tileOverlay');

					oldOverlay.find('.highlight').each(function () {
						var highlight = $(this);
						var sectorX = parseInt(highlight.attr('data-highlight-x'));
						var sectorY = parseInt(highlight.attr('data-highlight-y'));
						var posx = (sectorX - __currentSize.minx) * SECTOR_IMAGE_SIZE;
						var posy = (sectorY - __currentSize.miny) * SECTOR_IMAGE_SIZE;
						highlight.css({
							left: posx + "px",
							top: posy + "px"
						});

						overlay.append(highlight);
					});
					oldOverlay.replaceWith(overlay);
					__reloadTriggered = false;
				});
		}

		var __reloadOverlay = function()
		{
			var realSize = {
				minx:__currentSize.minx,
				maxx:__currentSize.maxx,
				miny:__currentSize.miny,
				maxy:__currentSize.maxy};


			// Fehlende Bereiche identifieren und Ausschnitt vergroessern
			var mod = false;
			if( __currentShiftOffset[0] >= SECTOR_IMAGE_SIZE ) {
				realSize.minx -= Math.ceil(__currentShiftOffset[0] / SECTOR_IMAGE_SIZE);
				realSize.maxx = realSize.minx + __screen.widthInSectors() + 1;
				mod = true;
			}
			else if( __screen.widthGap(__currentShiftOffset[0], __currentSize.minx, __currentSize.maxx) > 0 ) {
				realSize.maxx += __screen.widthGap(__currentShiftOffset[0], __currentSize.minx, __currentSize.maxx);
				realSize.minx = Math.max(1, realSize.maxx - __screen.widthInSectors() - 1);
				mod = true;
			}

			if( __currentShiftOffset[1] >= SECTOR_IMAGE_SIZE ) {
				realSize.miny -= Math.ceil(__currentShiftOffset[1] / SECTOR_IMAGE_SIZE);
				realSize.maxy = realSize.miny + __screen.heightInSectors() + 1;
				mod = true;
			}
			else if( __screen.heightGap(__currentShiftOffset[1], __currentSize.miny, __currentSize.maxy) > 0 ) {
				realSize.maxy += __screen.heightGap(__currentShiftOffset[1], __currentSize.miny, __currentSize.maxy);
				realSize.miny = Math.max(1, realSize.maxy - __screen.heightInSectors() - 1);
				mod = true;
			}

			if( mod ) {
				doServerRequest(realSize);
			}

			__reloadTriggered = false;
		};

		var __updateShiftOffset = function(newSize) {
			__currentShiftOffset[0] -= (__currentSize.minx-newSize.minx)*SECTOR_IMAGE_SIZE;
			__currentShiftOffset[1] -= (__currentSize.miny-newSize.miny)*SECTOR_IMAGE_SIZE;
		};

		var __renderOverlay = function(data)
		{
			function tile(loc) {
				return Math.floor((loc-1)/TILE_SIZE);
			}

			var overlay = "<div id='tileOverlay' style='left:"+(__currentShiftOffset[0])+"px;top:"+(__currentShiftOffset[1])+"px'>";

			var slice = (function() {
				var url = DS.getUrl();
				var locy = null;
				var locx = null;
				var tilex = null;
				var tiley = null;
				var width = 0;

				function flush() {
					var posx = (locx-__currentSize.minx)*SECTOR_IMAGE_SIZE;
					var posy = (locy-__currentSize.miny)*SECTOR_IMAGE_SIZE;
					return "<div style=\"top:"+posy+"px;left:"+posx+"px;width:"+(width*SECTOR_IMAGE_SIZE)+"px;background-image:url('"+url+"?module=map&action=tile&sys="+__currentSystem.id+"&tileX="+tilex+"&tileY="+tiley+"');background-position:-"+((locx-1)%TILE_SIZE)*SECTOR_IMAGE_SIZE+"px -"+((locy-1)%TILE_SIZE)*SECTOR_IMAGE_SIZE+"px\" ></div>";
				}

				var complete = function() {
					if( locx == null ) {
						return "";
					}
					var overlay = flush();
					locx = null;
					locy = null;
					width = 0;
					return overlay;
				};
				var extend = function(x, y) {
					var overlay = "";
					if( locx !== null && (tilex !== tile(x) || locy !== y || locx+width !== x) ) {
						overlay += flush();
						locx = null;
					}
					if( locx === null ) {
						locx = x;
						locy = y;
						width = 0;
						tilex = tile(x);
						tiley = tile(y);
					}
					width++;
					return overlay;
				};

				return {complete:complete, extend:extend};
			})();

			for( var i=0; i < data.locations.length; i++ ) {
				var loc = data.locations[i];

				if( loc.bg != null && loc.bg.image == null && loc.fg == null ) {
					overlay += slice.extend(loc.x, loc.y);
					continue;
				}

				overlay += slice.complete();

				var posx = (loc.x-__currentSize.minx)*SECTOR_IMAGE_SIZE;
				var posy = (loc.y-__currentSize.miny)*SECTOR_IMAGE_SIZE;

				if( loc.bg != null ) {
					if( loc.bg.image == null )
					{
						var url = DS.getUrl();
						var tilex = tile(loc.x);
						var tiley = tile(loc.y);
						overlay += "<div style=\"top:"+posy+"px;left:"+posx+"px;background-image:url('"+url+"?module=map&action=tile&sys="+__currentSystem.id+"&tileX="+tilex+"&tileY="+tiley+"');background-position:-"+((loc.x-1)%TILE_SIZE)*SECTOR_IMAGE_SIZE+"px -"+((loc.y-1)%TILE_SIZE)*SECTOR_IMAGE_SIZE+"px\" >";
					}
					else {
						overlay += "<div style=\"top:"+posy+"px;left:"+posx+"px;background-image:url('"+loc.bg.image+"');background-position:"+loc.bg.x*SECTOR_IMAGE_SIZE+"px "+loc.bg.y*SECTOR_IMAGE_SIZE+"px\" >";
					}
				}
				else if( loc.fg != null ) {
					overlay += "<div style=\"top:"+posy+"px;left:"+posx+"px\" >";
				}

				if( loc.fg != null ) {
					var classes = "fg";
					if( loc.battle ) {
						classes += " battle";
					}
					else if( loc.roterAlarm ) {
						classes += " roter-alarm";
					}
					overlay += '<img class="'+classes+'" src="'+loc.fg+'" alt="'+loc.x+'/'+loc.y+'" />';
				}

				if( loc.fg != null || loc.bg != null ) {
					overlay += "</div>";
				}
			}
			overlay += slice.complete();
			overlay += "</div>";

			return overlay;
		};

		/**
		 * Gibt die zum angegebenen Sektor vorhandenen Informationen zurueck.
		 * @name Overlay.getSectorInformation
		 * @param {Number} sectorX Die x-Koordinate des Sektors
		 * @param {Number} sectorY Die Y-Koordinate des Sektors
		 * @return {Object} Die Sektorinformationen oder null
		 */
		this.getSectorInformation = function(sectorX,sectorY) {
			for( var i=0; i < __currentLocations.length; i++ )
			{
				var loc = __currentLocations[i];
				if( loc.x == sectorX && loc.y == sectorY )
				{
					return loc;
				}
			}
			return null;
		};

		/**
		 * Highlightet einen Sektor.
		 * @name Overlay.highlight
		 * @param shape {{x:number,y:number,size:?number}} Die x-Koordinate des Sektors
		 * @param highlightGroup {string=} Die Gruppe zu der das highlight gehört.
		 * Falls nicht angegeben gehoert es zur Defaultgruppe.
		 * @param cssClass {string=} Zusätzliche an das Highlight zu schreibende CSS-Klassen
		 */
		this.highlight = function(shape, highlightGroup, cssClass) {
			if( highlightGroup == null ) {
				highlightGroup = "DEFAULT";
			}
			if( shape.x < 1 || shape.x > __currentSystem.width ) {
				return;
			}
			if( shape.y < 1 || shape.y > __currentSystem.height ) {
				return;
			}
			var size = shape.size || 0;

			var highlight = "";
			for( var i=shape.x-size; i <= shape.x+size; i++ ) {
				if( i < 1 ) {
					continue;
				}
				var height = 0;
				var minY = Number.MAX_VALUE;
				for( var j=shape.y-size; j <= shape.y+size; j++ ) {
					if( j < 1 ) {
						continue;
					}

					if( Math.floor(Math.sqrt(Math.pow(j-shape.y,2)+Math.pow(i-shape.x,2))) <= size ) {
						height += 1;
						if( minY > j ) {
							minY = j;
						}
					}
				}
				if( height > 0 ) {
					var posx = (i-__currentSize.minx)*SECTOR_IMAGE_SIZE;
					var posy = (minY-__currentSize.miny)*SECTOR_IMAGE_SIZE;
					height *= SECTOR_IMAGE_SIZE;

					highlight += "<div style=\"top:"+posy+"px;left:"+posx+"px;height:"+height+"px\" class='highlight "+cssClass+"' " +
						"data-highlight-group='"+highlightGroup+"' data-highlight-x='"+shape.x+"' data-highlight-y='"+shape.y+"' />";
				}
			}
			$('#tileOverlay').append(highlight)
		};

		/**
		 * Entfernt das Highlight einer bestimmten Gruppe von einem Sektor.
		 * @name Overlay.unhighlight
		 * @param sectorX {number} Die x-Koordinate des Sektors
		 * @param sectorY {number} Die y-Koordinate des Sektors
		 * @param highlightGroup {string=} Die Highlight-Gruppe.
		 * Falls nicht angegeben wird die Defaultgruppe genommen.
		 */
		this.unhighlight = function(sectorX, sectorY, highlightGroup) {
			if( highlightGroup == null ) {
				highlightGroup = "DEFAULT";
			}
			$('#tileOverlay').find('.highlight[data-highlight-x='+sectorX+'][data-highlight-y='+sectorY+'][data-highlight-group='+highlightGroup+']').remove();
		};

		/**
		 * Entfernt alle Highlights die zu einer Gruppe gehoeren.
		 * @name Overlay.unhighlightGroup
		 * @param highlightGroup {string=} Die Highlight-Gruppe.
		 * Falls nicht angegeben wird die Defaultgruppe genommen.
		 */
		this.unhighlightGroup = function(highlightGroup) {
			if( highlightGroup == null ) {
				highlightGroup = "DEFAULT";
			}
			$('#tileOverlay').find('.highlight[data-highlight-group='+highlightGroup+']').remove();
		};

		/**
		 * Bereitet eine Verschiebeoperation vor.
		 * @name Overlay.prepareShift
		 * @param moveX {number} Der Offset in X-Richtung in Pixel
		 * @param moveY {number} Der Offset in Y-Richtung in Pixel
		 */
		this.prepareShift = function(moveX, moveY) {
			__currentShiftOffset = [__currentShiftOffset[0]-moveX, __currentShiftOffset[1]-moveY];

			if( !__reloadTriggered ) {
				__reloadTriggered = true;
				setTimeout(function() {__reloadOverlay();}, 500);
			}
		};

		this.storeShift = function(cl) {
			var overlay = cl.find('#tileOverlay');
			overlay.css({'left' : (__currentShiftOffset[0])+'px', 'top' : (__currentShiftOffset[1])+'px'});
		};

		/**
		 * Aktualisiert die momentane Anzeige mit neuen vom Server abgeholten Daten.
		 */
		this.reload = function() {
			doServerRequest(__currentSize);
		};

		var content = __renderOverlay(data);
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
		 * @name LoaderPopup.show
		 */
		this.show = function() {
			$('#starmaploader .content').html("Verbinde mit interplanetarem Überwachungsnetzwerk...<br /><img src='./data/interface/ajax-loader.gif' alt='Lade' />");
			$('#starmaploader').dsBox('show')
		};
		/**
		 * Verbirgt das Loader-Popup wieder.
		 * @name LoaderPopup.hide
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
	/**
	 * @type {Legend}
	 * @private
	 */
	var __starmapLegend = null;
	/**
	 * @type {Tiles}
	 * @private
	 */
	var __starmapTiles = null;
	/**
	 * @type {Overlay}
	 * @private
	 */
	var __starmapOverlay = null;
	var __request = null;
	var __ready = false;
	/**
	 * @param {JsonSystemResponse} currentSystem
	 * @param {Number} sectorX
	 * @param {Number} sectorY
	 * @param {Object} locationInfo
	 * @private
	 */
	var __onSectorClicked = function(currentSystem,sectorX,sectorY,locationInfo) {};

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

		request.FORMAT = 'JSON';
		request.sys = sys;
		request.xstart = xstart;
		request.xend = xstart+width;
		request.ystart = ystart;
		request.yend = ystart+height;
		request.loadmap = 1;
		request.module = 'map';
		request.action = 'map';

		if( options.onSectorClicked ) {
			__onSectorClicked = options.onSectorClicked;
		}

		$.getJSON(DS.getUrl(),
			request,
			function(data) {
				dsMsg(data);

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
	}

	/**
	 * Springt zum angegebenen Sektor. Die Karte wird (sofern die Grenzen
	 * es erlauben) direkt auf dem Sektor zentriert. Falls eine der Koordinatenangaben ein
	 * oder mehrere x-Zeichen enthaelt wird eine grobe Positionierung durchgefuehrt.
	 * @param x {object} Die X-Koordinate des Sektors
	 * @param y {object} Die Y-Koordinate des Sektors
	 */
	function gotoLocation(x, y) {
		var mapview = $('#mapview');
		var width = Math.floor((mapview.width())/SECTOR_IMAGE_SIZE);
		var height = Math.floor((mapview.height())/SECTOR_IMAGE_SIZE);

		var tmpx = x;
		var tmpy = y;
		if( typeof(tmpx) === 'string' ) {
			if( tmpx.indexOf('x') === 0 ) {
				tmpx = __currentSystem.width/2
			}
			else {
				tmpx = parseInt(tmpx.replace('x', 5))
			}
		}

		if( typeof(tmpy) === 'string' ) {
			if( tmpy.indexOf('x') === 0 ) {
				tmpy = __currentSystem.height/2
			}
			else {
				tmpy = parseInt(tmpy.replace('x', 5))
			}
		}

		var targetX = Math.max(1, tmpx-Math.floor(width/2));
		var targetY = Math.max(1, tmpy-Math.floor(height/2));

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
	}

	function clearMap()
	{
		$('#legend').remove();
		$('#tileOverlay').remove();
		$('#tiles').remove();
		$('#actionOverlay').remove();
		__currentShiftOffset = [0,0];
	}
	function renderMap(data, options)
	{
		__currentSize = data.size;
		__currentSystem = data.system;
		__screen = new StarmapScreen('#mapview', __currentSystem);

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
	}
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
	}

	var __loaderPopup = null;
	function onSystemLoad()
	{
		if( __loaderPopup == null ) {
			__loaderPopup = new LoaderPopup();
		}
		__loaderPopup.show();
	}
	function onSystemLoaded()
	{
		__loaderPopup.hide();
		__ready = true;
	}
	function onClick(x,y) {
		x -= __currentShiftOffset[0];
		y -= __currentShiftOffset[1];
		var sectorX = __currentSize.minx+Math.floor(x / SECTOR_IMAGE_SIZE);
		var sectorY = __currentSize.miny+Math.floor(y / SECTOR_IMAGE_SIZE);
		var locationInfo = __starmapOverlay.getSectorInformation(sectorX, sectorY);

		__onSectorClicked(__currentSystem, sectorX, sectorY, locationInfo);
	}

	// PUBLIC METHODS
	this.gotoLocation = gotoLocation;
	/**
	 * Highlightet einen Sektor.
	 * @param shape {{x:number,y:number,size:?number}} Die x-Koordinate des Sektors
	 * @param highlightGroup {string=} Die Gruppe zu der das highlight gehört.
	 * Falls nicht angegeben gehoert es zur Defaultgruppe.
	 * @param cssClass {string=} Zusätzliche an das Highlight zu schreibende CSS-Klassen
	 * @name Starmap.highlight
	 */
	this.highlight = function(shape,highlightGroup,cssClass) {
		if( __starmapOverlay != null ) {
			__starmapOverlay.highlight(shape,highlightGroup,cssClass);
		}
	};
	/**
	 * Entfernt das Highlight einer bestimmten Gruppe von einem Sektor.
	 * @param sectorX {number} Die x-Koordinate des Sektors
	 * @param sectorY {number} Die y-Koordinate des Sektors
	 * @param highlightGroup {string=} Die Highlight-Gruppe.
	 * Falls nicht angegeben wird die Defaultgruppe genommen.
	 * @name Starmap.unhighlight
	 */
	this.unhighlight = function(sectorX,sectorY,highlightGroup) {
		if( __starmapOverlay != null ) {
			__starmapOverlay.unhighlight(sectorX,sectorY,highlightGroup);
		}
	};
	/**
	 * Entfernt alle Highlights die zu einer Gruppe gehoeren.
	 * @param highlightGroup {string=} Die Highlight-Gruppe.
	 * Falls nicht angegeben wird die Defaultgruppe genommen.
	 * @name Starmap.unhighlightGroup
	 */
	this.unhighlightGroup = function(highlightGroup) {
		if( __starmapOverlay != null ) {
			__starmapOverlay.unhighlightGroup(highlightGroup);
		}
	};
	/**
	 * Laedt ein Sternensystem in der Sternenkarte und positioniert die Ansicht ueber der angegebenen
	 * Position.
	 * @param sys {number} Das zu ladende System (ID)
	 * @param x {number} Die X-Position auf der positioniert werden soll
	 * @param y {number} Die Y-Position auf der positioniert werden soll
	 * @param options {object}
	 * @name Starmap.load
	 */
	this.load = load;
	/**
	 * Gibt zurueck, ob die Sternenkarte erfolgreich geladen wurde, d.h.
	 * momentan ein Sternensystem dargestellt wird.
	 * @returns {boolean} true, falls dem so ist
	 * @name Starmap.isReady
	 */
	this.isReady = function() { return __ready; };
	/**
	 * Gibt die ID des momentan dargestellten Sternensystems zurueck.
	 * Das Ergebnis der Methode ist nicht definiert, falls momentan kein
	 * Sternensystem geladen ist.
	 * @returns {number} Die ID
	 * @name Starmap.getSystemId
	 */
	this.getSystemId = function() { return __currentSystem.id; };
	/**
	 * Aktualisiert die Ansicht der Sternenkarte. Dabei werden aktuelle Daten
	 * vom Server abgeholt und angezeigt. Die Anzeigeposition aendert sich nicht.
	 */
	this.refresh = function() {
		if( __starmapOverlay != null ) {
			__starmapOverlay.reload();
		}
	};
};
	}]);