'use strict';

angular.module('jquery-ui.directives', [])
.directive('jqueryUiAutocomplete', function() {
	return function(scope, element, attrs) {
		var elemName = attrs.name;
		var expression = attrs.jqueryUiAutocomplete;
		scope.$watch(expression, function(value) {
			var items = this.$eval(expression);
			element
				.autocomplete({
					source : items,
					html : false,
					select : function( event, ui ) {
						element.trigger('input');
					},
					change : function( event, ui ) {
						element.trigger('input');
					}
				})
				.blur(function() {
					element.trigger('input');
				});
		});
	};
});

angular.module('ds.directives', [])
.directive('dsAutocomplete', function() {
	return function(scope, element, attrs) {
		var elemName = attrs.name;
		var items = null;
		if( attrs.dsAutocomplete == "users" ) {
			items = DsAutoComplete.users;
		}

		element
			.autocomplete({
				source : items,
				html : true,
				select : function( event, ui ) {
					element.trigger('input');
				},
				change : function( event, ui ) {
					element.trigger('input');
				}
			})
			.blur(function() {
				element.trigger('input');
			});
	};
})
.directive('dsBindHtmlUnsafe', [function() {
	return function(scope, element, attr) {
		element.addClass('ds-binding').data('$binding', attr.dsBindHtmlUnsafe);
		scope.$watch(attr.dsBindHtmlUnsafe, function(value) {
			element.html(value || '');
			DsTooltip.update(element);
		});
	};
}])
.directive('dsPopupControlOpen', ['PopupService', function(PopupService) {
	return {
		restrict : 'A',
		link : function(scope, element, attrs) {
			element.bind("click", function() {
				PopupService.open(attrs.dsPopupControlOpen, {element:element});
			});
		}
	};
}])
.directive('dsPopupControlClose', ['PopupService', function(PopupService) {
	return {
		restrict : 'A',
		link : function(scope, element, attrs) {
			element.bind("click", function() {
				PopupService.close(attrs.dsPopupControlClose, {element:element});
			});
		}
	};
}])
.directive('dsPopup', ['PopupService', function(PopupService) {
	return {
		scope:true,
		restrict:'A',
		link : function(scope, element, attrs) {
			var popupOptions = {
				title : attrs.dsPopupTitle,
				autoOpen : attrs.dsPopupAutoOpen ? true : false
			};
			if( attrs.dsPopupWidth ) {
				popupOptions.width = attrs.dsPopupWidth;
			}
			if( attrs.dsPopupHeight ) {
				popupOptions.height = attrs.dsPopupHeight;
			}

			var dialogEl = $(element.get(0));
			PopupService.create(attrs.dsPopup, dialogEl, popupOptions, scope);

			if( element.get(0).nodeName.toUpperCase() == "A" ) {
				element.bind("click", function() {
					PopupService.open(attrs.dsPopup);
				});
			}
		}
	};
}])
.factory('PopupService', [function() {
	var popupService = {
	};

	var embeddedPopupRegistry = [];
	var queuedActions = [];

	popupService.create = function(name, dialogEl, popupOptions, scope) {
		dialogEl.dialog(popupOptions);
		dialogEl.addClass('gfxbox');
		scope.dsPopupName = name;
		embeddedPopupRegistry[name] = {element:dialogEl, scope:scope};

		if( queuedActions[name] != null ) {
			if( queuedActions[name] ) {
				dialogEl.dialog('open');
			}
			else {
				dialogEl.dialog('close');
			}
			queuedActions[name] = null;
		}
	}

	function resolvePopupName(name, element) {
		if( name.indexOf('@') !== 0 ) {
			return name;
		}
		if( name == "@this" ) {
			return element.parents().filter('[ds-popup]').first().attr('ds-popup');
		}
		throw "Unbekannter Popupname: "+name;
	}

	popupService.open = function(name, options) {
		if(options == null ) {
			options = {};
		}
		name = resolvePopupName(name, options.element);
		if( !embeddedPopupRegistry[name] ) {
			queuedActions[name] = true;
			return;
		}
		embeddedPopupRegistry[name].element.dialog('open');
		embeddedPopupRegistry[name].scope.$broadcast('dsPopupOpen', name, options.parameters);
	}

	popupService.close = function(name, options) {
		if(options == null ) {
			options = {};
		}
		name = resolvePopupName(name, options.element);
		if( !embeddedPopupRegistry[name] ) {
			queuedActions[name] = false;
			return;
		}
		embeddedPopupRegistry[name].element.dialog('close');
	}

	return popupService;
}])
.directive('dsExternalPopup', ['ExternalPopupService', function(PopupService) {
	return {
		link : function(scope, element, attrs) {
			var popupOptions = {
				title : attrs.dsPopupTitle
			};
			if( element.get(0).nodeName.toUpperCase() == "A" ) {
				element.bind("click", function() {
					PopupService.load("data/cltemplates/"+attrs.dsPopup, scope, popupOptions);
				});
			}
		}
	};
}])
.factory('ExternalPopupService', ['$http', '$compile', function($http, $compile) {
	var popupService = {
		popupElement : null
	};

	popupService.getExternalPopup = function(create) {
		if( popupService.popupElement == null && create) {
			popupService.popupElement = $('<div class="modal hide gfxbox"></div>');
			popupService.popupElement.appendTo('body');
		}

		return popupService.popupElement;
	}

	popupService.load = function(url, scope, options) {
		$http.get(url).success(
			function(data) {
				var popup = popupService.getExternalPopup(true);
				popup.html(data);
				$compile(popup)(scope);
				$(popup).dialog(options);
			});
	}

	popupService.close = function() {
		var popup = popupService.getExternalPopup(false);
		if( popup != null ) {
			$(popup).dialog('hide');
		}
	}

	return popupService;
}])
.factory('dsChartSupport', function() {
	return {
		__chartId : 1,
		generateChartId : function() {
			return "_dsChart"+this.__chartId++;
		},
		parseAxis : function(element, axisName, target) {
			var axisElem = element.find(axisName);
			if( axisElem.size() > 0 ) {
				if( axisElem.attr('label') ) {
					target.label = axisElem.attr('label');
				}
				if( axisElem.attr('pad') ) {
					target.pad = axisElem.attr('pad');
				}
				if( axisElem.attr('tick-interval') ) {
					target.tickInterval = axisElem.attr('tick-interval');
				}
			}
		},
		parseLegend : function(element, target) {
			var legendElem = element.find('legend');
			if( legendElem.size() > 0 ) {
				target.show = true;
				if( legendElem.attr('location') ) {
					target.location = legendElem.attr('location');
				}
			}
		},
		generateDataFromScopeValue : function(options, evalValue) {
			var data = [];
			angular.forEach(evalValue, function(entry) {
				if( typeof entry === 'object' && !(entry instanceof Array)) {
					var seriesEntry = {};
                    seriesEntry.label = entry.label;
					data.push(entry.data);
					options.series.push(seriesEntry);
				}
				else {
					data.push(entry);
				}
			});
			return data;
		}
	};
})
.directive('dsLineChart', ['dsChartSupport', function(dsChartSupport) {
	return {
		restrict : 'A',
		link : function(scope, element, attrs) {
			var currentId = dsChartSupport.generateChartId();
			element.attr('id', currentId);
			
			attrs.$observe('dsLineChart', function(value) {
				scope.$watch(value, function(evalValue) {
					if( evalValue == null || !evalValue ) {
						evalValue = [null];
					}

					var options = {
						axes : {
							xaxis : {},
							yaxis : {}
						},
						legend: {},
						highlighter:{show:true, sizeAdjust: 7.5},
						cursor:{show:false},
						series: []
					};
					if( attrs.title ) {
						options.title = attrs.title;
					}

					dsChartSupport.parseAxis(element, 'x-axis', options.axes.xaxis);
					dsChartSupport.parseAxis(element, 'y-axis', options.axes.yaxis);
					dsChartSupport.parseLegend(element, options.legend);

					var data = dsChartSupport.generateDataFromScopeValue(options, evalValue);

					$('#'+currentId).find('canvas,div').remove();
					DS.plot(currentId, data, options);
				});
			});
		}
	};
}])
	.directive('dsLog', function() {
		return {
			restrict : 'E',
			transclude: true,
			scope: {
				logentries: '=list',
				entry : '=entry'
			},
			replace: true,
			template:
				'<div class="log">' +
					'<div class="logcontainer">' +
						'<div class="logentries">' +
						'<div class="logentry" ng-repeat="entry in logentries" ng-transclude />'+
						'</div>'+
					'</div>'+
					'<span class="toggleSize" title="aufklappen" />'+
				'</div>',
			link : function(scope, element, attrs) {
				var container = element.find('.logcontainer');
				var oldHeight = 0;

				element.find('.toggleSize').on('click', function(event) {
					element.toggleClass('expanded');
					$(event.target).attr('title', element.hasClass('expanded') ? 'zuklappen' : 'aufklappen');
					oldHeight = element.find('.logentries').height();
					container.scrollTop(oldHeight);
				});
				scope.$watch('logentries', function(){
					setTimeout(function() {
						var height;
						if( !element.hasClass('expanded') || container.scrollTop()+container.height() >= oldHeight ) {
							container.stop(true);
							height = element.find('.logentries').height();
							container.animate({ scrollTop: height }, 1000);
						}
						else {
							height = element.find('.logentries').height();
						}
						oldHeight = height;
					}, 100);
				}, true);
			}
		};
	})
.directive('dsPieChart', ['dsChartSupport', function(dsChartSupport) {
	return {
		restrict : 'A',
		link : function(scope, element, attrs) {
			var currentId = dsChartSupport.generateChartId();
			element.attr('id', currentId);

			attrs.$observe('dsPieChart', function(value) {
				scope.$watch(value, function(evalValue) {
					if( evalValue == null || !evalValue ) {
						evalValue = [null];
					}

					var options = {
						seriesDefaults : {
							renderer:$.jqplot.PieRenderer,
							rendererOptions:{
								showDataLabels:true,
								dataLabelThreshold:0
							}
						},
						legend: {},
						highlighter:{
							show:false
						},
						cursor:{show:false},
						series: []
					};
					if( attrs.title ) {
						options.title = attrs.title;
					}
					if( attrs.labelThreshold ) {
						options.seriesDefaults.rendererOptions.dataLabelThreshold =
							attrs.labelThreshold;
					}
					if( !attrs.percentValues ) {
						options.seriesDefaults.rendererOptions.dataLabels='value';
					}

					dsChartSupport.parseLegend(element, options.legend);

					var data = dsChartSupport.generateDataFromScopeValue(options, evalValue);

					$('#'+currentId).find('canvas,div').remove();
					DS.plot(currentId, data, options);
				});
			});
		}
	};
}])
.factory('dsGraphSupport', function() {
	/* Basiert auf Dracula Graph Library */
	var Graph = {};
	Graph.Layout = {};
	Graph.Layout.Spring = function(graph) {
		this.graph = graph;
		this.iterations = 500;
		this.maxRepulsiveForceDistance = 5;
		this.minAttractiveForceDistance = 3;
		this.k = 2;
		this.c = 0.01;
		//this.c = 0.8;
		this.maxVertexMovement = 4;
		this.minVertexMovement = 0;
		this.round = false;
		this.layout();
	};
	Graph.Layout.Spring.prototype = {
		layout: function() {
			this.layoutPrepare();
			for (var i = 0; i < this.iterations; i++) {
				this.layoutIteration();
			}
			this.layoutCalcBounds();
		},

		layoutPrepare: function() {
			for( var i= 0, length=this.graph.nodes.length; i < length; i++ ) {
				var node = this.graph.nodes[i];
				node.layoutPosX = 0;
				node.layoutPosY = 0;
				node.layoutForceX = 0;
				node.layoutForceY = 0;
			}

		},

		layoutCalcBounds: function() {
			var minx = Infinity, maxx = -Infinity, miny = Infinity, maxy = -Infinity;

			for( var i= 0, length=this.graph.nodes.length; i < length; i++ ) {
				var node = this.graph.nodes[i];
				var x = node.layoutPosX;
				var y = node.layoutPosY;

				if(x > maxx) maxx = x;
				if(x < minx) minx = x;
				if(y > maxy) maxy = y;
				if(y < miny) miny = y;
			}

			this.graph.layoutMinX = minx;
			this.graph.layoutMaxX = maxx;
			this.graph.layoutMinY = miny;
			this.graph.layoutMaxY = maxy;
		},

		layoutIteration: function() {
			// Forces on nodes due to node-node repulsions

			var prev = new Array();
			for( var c= 0, length=this.graph.nodes.length; c < length; c++ ) {
				var node1 = this.graph.nodes[c];
				for( var d= 0, length2=prev.length; d < length2; d++ ) {
					var node2 = this.graph.nodes[prev[d]];
					this.layoutRepulsive(node1, node2);

				}
				prev.push(c);
			}

			// Forces on nodes due to edge attractions
			for (var i = 0; i < this.graph.edges.length; i++) {
				var edge = this.graph.edges[i];
				this.layoutAttractive(edge);
			}

			// Move by the given force
			for( var i= 0, length=this.graph.nodes.length; i < length; i++ ) {
				var node = this.graph.nodes[i];
				var xmove = this.c * node.layoutForceX;
				var ymove = this.c * node.layoutForceY;
				if( this.round ) {
					xmove = Math.round(xmove);
					ymove = Math.round(ymove);
				}

				var max = this.maxVertexMovement;
				var min = this.minVertexMovement;
				if(xmove > max) xmove = max;
				else if(xmove < -max) xmove = -max;
				else if(xmove <= min && xmove >= -min ) xmove = 0;

				if(ymove > max) ymove = max;
				else if(ymove < -max) ymove = -max;
				else if(ymove <= min && ymove >= -min ) ymove = 0;

				node.layoutPosX += xmove;
				node.layoutPosY += ymove;
				node.layoutForceX = 0;
				node.layoutForceY = 0;
			}
		},

		layoutRepulsive: function(node1, node2) {
			if (typeof node1 == 'undefined' || typeof node2 == 'undefined')
				return;
			var dx = node2.layoutPosX - node1.layoutPosX;
			var dy = node2.layoutPosY - node1.layoutPosY;
			var d2 = dx * dx + dy * dy;
			if(d2 < 0.01) {
				dx = 0.1 * Math.random() + 0.1;
				dy = 0.1 * Math.random() + 0.1;
				d2 = dx * dx + dy * dy;
			}
			var d = Math.sqrt(d2);
			if(d < this.maxRepulsiveForceDistance) {
				var repulsiveForce = this.k * this.k / d;
				node2.layoutForceX += repulsiveForce * dx / d;
				node2.layoutForceY += repulsiveForce * dy / d;
				node1.layoutForceX -= repulsiveForce * dx / d;
				node1.layoutForceY -= repulsiveForce * dy / d;
			}
		},

		layoutAttractive: function(edge) {
			var node1 = edge.source;
			var node2 = edge.target;

			var dx = node2.layoutPosX - node1.layoutPosX;
			var dy = node2.layoutPosY - node1.layoutPosY;
			var d2 = dx * dx + dy * dy;
			if(d2 < 0.01) {
				dx = 0.1 * Math.random() + 0.1;
				dy = 0.1 * Math.random() + 0.1;
				d2 = dx * dx + dy * dy;
			}
			var d = Math.sqrt(d2);
			if(d > this.maxRepulsiveForceDistance) {
				d = this.maxRepulsiveForceDistance;
				d2 = d * d;
			}
			else if( d < this.minAttractiveForceDistance ) {
				d = this.minAttractiveForceDistance;
				d2 = d * d;
			}
			var attractiveForce = (d2 - this.k * this.k) / this.k;
			if(edge.attraction == undefined) edge.attraction = 1;
			attractiveForce *= Math.log(edge.attraction) * 0.5 + 1;

			node2.layoutForceX -= attractiveForce * dx / d;
			node2.layoutForceY -= attractiveForce * dy / d;
			node1.layoutForceX += attractiveForce * dx / d;
			node1.layoutForceY += attractiveForce * dy / d;
		}
	};
	return Graph;
})
.directive('dsGraph', ['dsGraphSupport',function(dsGraphSupport) {
	var uid               = ['0', '0', '0'];
	/**
	 * A consistent way of creating unique IDs in angular. The ID is a sequence of alpha numeric
	 * characters such as '012ABC'. The reason why we are not using simply a number counter is that
	 * the number string gets longer over time, and it can also overflow, where as the the nextId
	 * will grow much slower, it is a string, and it will never overflow.
	 *
	 * @returns an unique alpha-numeric string
	 *
	 * Basierend auf angular.nextUid (interne API)
	 */
	function nextUid() {
		var index = uid.length;
		var digit;

		while(index) {
			index--;
			digit = uid[index].charCodeAt(0);
			if (digit == 57 /*'9'*/) {
				uid[index] = 'A';
				return uid.join('');
			}
			if (digit == 90  /*'Z'*/) {
				uid[index] = '0';
			} else {
				uid[index] = String.fromCharCode(digit + 1);
				return uid.join('');
			}
		}
		uid.unshift('0');
		return uid.join('');
	}
	/**
	 * Computes a hash of an 'obj'.
	 * Hash of a:
	 *  string is string
	 *  number is number as string
	 *  object is either result of calling $$hashKey function on the object or uniquely generated id,
	 *         that is also assigned to the $$hashKey property of the object.
	 *
	 * @param obj
	 * @returns {string} hash string such that the same input will have the same hash string.
	 *         The resulting string key is in 'type:hashKey' format.
	 *  Basierend auf angular.hashKey (interne API)
	 */
	function hashKey(obj) {
		var objType = typeof obj,
			key;

		if (objType == 'object' && obj !== null) {
			if (typeof (key = obj.$$hashKey) == 'function') {
				// must invoke on object to keep the right this
				key = obj.$$hashKey();
			}
			else if( typeof obj.id !== 'undefinied' && obj.id != null ) {
				key = obj.$$hashKey = obj.id;
			}
			else if (key === undefined) {
				key = obj.$$hashKey = nextUid();
			}
		} else {
			key = obj;
		}

		return objType + ':' + key;
	}
	/**
	 * A map where multiple values can be added to the same key such that they form a queue.
	 * @returns {HashQueueMap}
	 * Basierend auf angular.HashQueueMap (interne API)
	 */
	function HashQueueMap() {}
	HashQueueMap.prototype = {
		push: function(key, value) {
			var array = this[key = hashKey(key)];
			if (!array) {
				this[key] = [value];
			} else {
				array.push(value);
			}
		},

		shift: function(key) {
			var array = this[key = hashKey(key)];
			if (array) {
				if (array.length == 1) {
					delete this[key];
					return array[0];
				} else {
					return array.shift();
				}
			}
		},
		get: function(key) {
			var array = this[key = hashKey(key)];
			return array[0];
		}
	};

	function prepareGraphEdges(graph) {
		for( var i= 0,length=graph.edges.length;i < length; i++) {
			var edge = graph.edges[i];
			if( angular.isObject(edge.source) ) {
				continue;
			}
			// Die Referenz auf die Nodes ist als ID kodiert
			// - nun durch die entsprechenden Nodes ersetzen
			for( var j= 0, length2=graph.nodes.length; j < length2; j++ ) {
				var node = graph.nodes[j];
				if( node.id === edge.source ) {
					edge.source = node;
					if( angular.isObject(edge.target) ) {
						break;
					}
				}
				if( node.id === edge.target ) {
					edge.target = node;
					if( angular.isObject(edge.source) ) {
						break;
					}
				}
			}
			// Falls eine ID nicht aufgeloesst werden konnte
			// die entsprechende Node entfernen
			if( !angular.isObject(edge.source) || !angular.isObject(edge.target) ) {
				graph.edges.splice(i, 1);
				i--;
				length--;
			}
		}
	}

	function computeVoronoiDiagramBBox(graph, coordTransformer) {
		var bbox = {
			xl:0,
			xr:0,
			yt:0,
			yb:0
		};

		for( var i=0; i < graph.nodes.length; i++ ) {
			var node = graph.nodes[i];

			var x = node.posX || coordTransformer.transformX(node.layoutPosX);
			var y = node.posY || coordTransformer.transformY(node.layoutPosY);

			if( bbox.xr < x+coordTransformer.getNodeWidth()+10 ) {
				bbox.xr = x+coordTransformer.getNodeWidth()+10;
			}
			if( bbox.yb < y+coordTransformer.getNodeHeight()+10 ) {
				bbox.yb = y+coordTransformer.getNodeHeight()+10;
			}
		}
		return bbox
	}

	function computeVoronoiDiagram(graph, voronoiBbox, coordTransformer) {
		var voronoi = new Voronoi();
		var vertices = [];

		for( var i=0; i < graph.nodes.length; i++ ) {
			var node = graph.nodes[i];

			var x = node.posX || coordTransformer.transformX(node.layoutPosX);
			var y = node.posY || coordTransformer.transformY(node.layoutPosY);

			vertices.push({
				x: x+coordTransformer.getNodeWidth()/2,
				y: y+coordTransformer.getNodeHeight()/2,
				nodeId: i,
				group: node.group
			});
		}
		// a 'vertex' is an object exhibiting 'x' and 'y' properties. The
		// Voronoi object will add a unique 'voronoiId' property to all
		// vertices. The 'voronoiId' can be used as a key to lookup the
		// associated cell in 'diagram.cells'.
		return voronoi.compute(vertices, voronoiBbox);
	}

	function renderVoronoiDiagram(targetEl, diagram, voronoiBbox) {
		if( diagram.edges.length == 0 ) {
			return;
		}

		var parent = targetEl.get(0);

		var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
		svg.setAttribute("version", "1.1");
		svg.setAttribute("preserveAspectRatio", "none");
		svg.setAttribute("class", "voronoi graphBackground");

		var groupMap = [];
		var groupCounter = 0;

		var cells = diagram.cells,
			cellLength = cells.length;
		for( var i=0; i < cellLength; i++ ) {
			var cell = cells[i];
			var halfedges = cell.halfedges;
			var group = cell.site.group;
			var halfedgeLength = halfedges.length;
			if( halfedgeLength == 0 ) {
				continue;
			}

			var cls = groupMap[group.id];
			if( cls == null ) {
				cls = "group"+(groupCounter++);
				groupMap[group.id] = cls;
			}

			var points = "";
			for( var j=0; j < halfedgeLength; j++ ) {
				var halfedge = halfedges[j];
				if( j > 0 ) {
					points += " ";
				}
				var startPoint = halfedge.getStartpoint();
				points += startPoint.x+","+startPoint.y;
				var endPoint = halfedge.getEndpoint();
				points += " "+endPoint.x+","+endPoint.y;
			}

			var poly = document.createElementNS("http://www.w3.org/2000/svg", "polygon");
			poly.setAttribute("points", points);
			poly.setAttribute("class", "group "+cls+" "+group.styleClass);
			svg.appendChild(poly);
		}

		var maxX = 0;
		var maxY = 0;
		var edges = diagram.edges,
			iEdge = edges.length,
			edge, v, v2;
		while (iEdge--) {
			edge = edges[iEdge];

			var rGroup = edge.rSite != null ? edge.rSite.group : null;
			var lGroup = edge.lSite != null ? edge.lSite.group : null;

			if( rGroup == null || lGroup == null || rGroup.id == lGroup.id ) {
				continue;
			}

			v = edge.va;
			v2 = edge.vb;
			var line = document.createElementNS("http://www.w3.org/2000/svg", "line");
			line.setAttribute("x1", v.x);
			line.setAttribute("x2", v2.x);
			line.setAttribute("y1", v.y);
			line.setAttribute("y2", v2.y);
			line.setAttribute("class", "border");
			svg.appendChild(line);

			if(v.x > maxX ) {
				maxX = v.x;
			}
			if(v2.x > maxX ) {
				maxX = v2.x;
			}
			if(v.y > maxY ) {
				maxY = v.y;
			}
			if(v2.y > maxY ) {
				maxY = v2.y;
			}
		}

		if( maxX < voronoiBbox.xr ) {
			maxX = voronoiBbox.xr
		}

		if( maxY < voronoiBbox.yb ) {
			maxY = voronoiBbox.yb
		}

		svg.setAttribute("style", "position:absolute;left:0px;top:0px;display:block;width:"+maxX+"px;height:"+maxY+"px");

		parent.insertBefore(svg, parent.firstChild);
	}

	function WorldToScreenTransformer(worldMinX,worldMinY, nodeWidth, nodeHeight) {
		return {
			transformX : function(x) {
				return Math.floor(nodeWidth/2+(x-worldMinX)*nodeWidth);
			},
			transformY : function(y) {
				return Math.floor(nodeHeight/2+(y-worldMinY)*nodeHeight);
			},
			getNodeHeight : function() {
				return nodeHeight;
			},
			getNodeWidth : function() {
				return nodeWidth;
			}
		};
	}

	var graphId = 1;
	// Basierend auf ng-repeat
	return {
		transclude: 'element',
		priority: 1000,
		terminal: true,
		compile: function(element, attr, linker) {
			return function(scope, iterStartElement, attr){
				iterStartElement.wrap("<div class='graph'><div class='graphcontainer'/></div>");

				var curGraphId = graphId++;

				var lastOrder = new HashQueueMap();
				var jsPlumbInstance = jsPlumb.getInstance();
				jsPlumbInstance.importDefaults({
					Connector : [ "Bezier", { curviness:30 } ],
					DragOptions : { cursor: "pointer", zIndex:2000 },
					PaintStyle : { strokeStyle:'darkgreen', lineWidth:2 },
					EndpointStyle : { radius:3, fillStyle:'darkgreen' },
					HoverPaintStyle : {strokeStyle:"#ec9f2e" },
					EndpointHoverStyle : {fillStyle:"#ec9f2e" }
				});
				var uniqueEdges = {};
				var nodeId = 0;

				scope.$watch(attr.dsGraph, function(dsGraph){
					var coordTransformer;

					iterStartElement.parent().parent().find('.graphBackground').remove();

					var graph = $.extend(true, dsGraph, {});
					if( graph.edges == null || graph.nodes == null ) {
						return;
					}
					prepareGraphEdges(graph);
					var layout = new dsGraphSupport.Layout.Spring(graph);

					var index, length,
						collection = layout.graph.nodes,
						collectionLength = collection.length,
						childScope,
						nextOrder = new HashQueueMap(),
						key, value,
						array = collection,
						last,
						cursor = iterStartElement;


					function updateElementPosition(value, element) {
						if( !coordTransformer ) {
							var nodeWidth = attr.nodeWidth ? parseInt(attr.nodeWidth) : element.outerWidth()*1.1;
							var nodeHeight = attr.nodeHeight ? parseInt(attr.nodeHeight) : element.outerHeight()*1.1;

							coordTransformer = new WorldToScreenTransformer(
								layout.graph.layoutMinX,
								layout.graph.layoutMinY,
								nodeWidth,
								nodeHeight
							);
						}

						var x, y;
						if( value.posX || value.posY ) {
							x = value.posX;
							y = value.posY;
						}
						else {
							x = coordTransformer.transformX(value.layoutPosX);
							y = coordTransformer.transformY(value.layoutPosY);
						}
						element.css({
							'left': x+"px",
							'top': y+"px"
						});
					}

					// we are not using forEach for perf reasons (trying to avoid #call)
					for (index = 0, length = array.length; index < length; index++) {
						key = (collection === array) ? index : array[index];
						value = collection[key];
						last = lastOrder.shift(value);
						if (last) {
							// if we have already seen this object, then we need to reuse the
							// associated scope/element
							childScope = last.scope;
							nextOrder.push(value, last);

							if (index === last.index) {
								// do nothing
								cursor = last.element;
							} else {
								// existing item which got moved
								last.index = index;
								// This may be a noop, if the element is next, but I don't know of a good way to
								// figure this out,  since it would require extra DOM access, so let's just hope that
								// the browsers realizes that it is noop, and treats it as such.
								cursor.after(last.element);
								cursor = last.element;
							}

							updateElementPosition(value, last.element);

							jsPlumbInstance.detachAllConnections(last.element);

							uniqueEdges[hashKey(value)] = {};
						}
						else {
							// new item which we don't know about
							childScope = scope.$new();
						}

						childScope.node = value;
						childScope.$index = index;

						childScope.$first = (index === 0);
						childScope.$last = (index === (collectionLength - 1));
						childScope.$middle = !(childScope.$first || childScope.$last);

						if (!last) {
							uniqueEdges[hashKey(value)] = {};

							linker(childScope, function(clone){
								var id = "_dsGraph"+curGraphId+"i"+(nodeId++);
								clone.attr("id", id);
								cursor.after(clone);

								updateElementPosition(value, clone);

								if( attr.draggable ) {
                                	jsPlumbInstance.draggable(clone);
									clone.on('dragstop', function(event,ui) {
										scope.$apply(function(scope) {
											var graph = scope.$eval(attr.dsGraph);
											$.each(lastOrder, function(key, value) {
												if( typeof value !== 'object' ) {
													return;
												}
												if( value[0].elementId == $(event.target).attr('id') ) {
													graph.nodes[value[0].index].posX = ui.position.left;
													graph.nodes[value[0].index].posY = ui.position.top;
													graph.nodes[value[0].index].moved = true;
												}
											});
										});

									});
								}

								last = {
									scope: childScope,
									element: (cursor = clone),
									index: index,
									elementId : id
								};
								nextOrder.push(value, last);
							});
						}
					}

					for (var indexEdge = 0, lengthEdge = layout.graph.edges.length; indexEdge < lengthEdge; indexEdge++) {
						var edge = layout.graph.edges[indexEdge];
						var node1 = nextOrder.get(edge.source);
						var node2 = nextOrder.get(edge.target);
						if( hashKey(edge.source) === hashKey(edge.target) ) {
							continue;
						}
						if( !uniqueEdges[hashKey(edge.source)][hashKey(edge.target)] ) {
							jsPlumbInstance.connect({
								source:node1.element,
								target:node2.element,
								anchor:"Continuous"
								//overlays:overlays
							});
							uniqueEdges[hashKey(edge.source)][hashKey(edge.target)] = true;
							uniqueEdges[hashKey(edge.target)][hashKey(edge.source)] = true;
						}
					}

					//shrink children
					for (key in lastOrder) {
						if (lastOrder.hasOwnProperty(key)) {
							array = lastOrder[key];
							while(array.length) {
								value = array.pop();
								jsPlumbInstance.detachAllConnections(value.element);
								value.element.remove();
								value.scope.$destroy();
								delete uniqueEdges[key];
							}
						}
					}

					if( attr.groupNodes ) {
						var voronoiBbox = computeVoronoiDiagramBBox(graph, coordTransformer);
						var voronoi = computeVoronoiDiagram(graph, voronoiBbox, coordTransformer);
						renderVoronoiDiagram(iterStartElement.parent(), voronoi, voronoiBbox);
					}
					lastOrder = nextOrder;
				});
			};
		}
	};
}]);
