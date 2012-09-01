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
					html : false
				})
				.blur(function() {
					scope[elemName] = element.val();
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
				html : true
			})
			.blur(function() {
				scope[elemName] = element.val();
			});
	};
})
.directive('dsPopup', ['PopupService', function(PopupService) {
	return {
		restrict : 'A',
		link : function(scope, element, attrs) {
			var popupOptions = {
				title : attrs.dsPopupTitle
			};
			element.bind("click", function() {
				PopupService.load("data/cltemplates/"+attrs.dsPopup, scope, popupOptions);
			});
		}
	};
}])
.factory('PopupService', ['$http', '$compile', function($http, $compile) {
	var popupService = {
		popupElement : null
	};

	popupService.getPopup = function(create) {
		if( popupService.popupElement == null && create) {
			popupService.popupElement = $('<div class="modal hide gfxbox"></div>');
			popupService.popupElement.appendTo('body');
		}

		return popupService.popupElement;
	}

	popupService.load = function(url, scope, options) {
		$http.get(url).success(
			function(data) {
				var popup = popupService.getPopup(true);
				popup.html(data);
				$compile(popup)(scope);
				$(popup).dialog(options);
			});
	}

	popupService.close = function() {
		var popup = popupService.getPopup(false);
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
}]);
