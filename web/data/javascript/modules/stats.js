var Stats = {
	__currentCategory : 0,
	__currentStatistic : 0,
	setCurrentStatistic : function(category,statIdx) {
		this.__currentCategory = category;
		this.__currentStatistic = statIdx;
	},
	germanNumberFormatter : function(format,val) {
		return DS.ln(val);
	},
	chart : function(selector, datasets, options) {
		var xaxis = options.xaxis;
		if( typeof xaxis === 'undefined' ) {
			xaxis = {};
		}

		var yaxis = options.yaxis;
		if( typeof yaxis === 'undefined' ) {
			yaxis = {};
		}

		var multiSelect = options.selection !== 'single';

		var selectedDatasets = [];
		var dataRendererFunction = function() {
			var data = [];
			for(var key in selectedDatasets) {
		        if( !selectedDatasets.hasOwnProperty(key) ) {
		        	continue;
		        }

		    	var set = selectedDatasets[key];
		    	var setData = [];
		    	for( var i=0,length=set.length; i < length; i++ ) {
		    		var setEntry = set[i];
		    		setData.push([setEntry.index, setEntry.count]);
		    	}
		    	if( setData.length > 0 ) {
		    		data.push(setData);
		    	}
		    }
			if( data.length == 0 ) {
				return [[null]];
			}
		    return data;
		};

		if( typeof yaxis.tickOptions === 'undefined' ) {
			yaxis.tickOptions = {formatter:this.germanNumberFormatter};
		}
		if( typeof xaxis.tickOptions === 'undefined' ) {
			xaxis.tickOptions = {formatter:this.germanNumberFormatter};
		}

		var redraw = function() {
			$('#'+selector).empty();
			DS.plot(selector, [], {
				axes:{'xaxis':xaxis, 'yaxis':yaxis},
				seriesDefaults:{showMarker:false},
				dataRenderer:dataRendererFunction
			});
		};

		redraw();

		var el = $('#'+selector);
		var datasetsEl = this.__renderDatasets(el, multiSelect, datasets);

		var self = this;

		var showDataset = function(key) {
			DS.getJSON(
				{module:'stats',
				action:'ajax',
				show:self.__currentCategory,
				stat:self.__currentStatistic,
				key:key},
				function(result) {
					selectedDatasets[key] = result.data;
					redraw();
				});
		};
		var removeDataset = function(key) {
			delete selectedDatasets[key];
			redraw();
		};
		var removeAllDatasets = function() {
			selectedDatasets = [];
			redraw();
		};

		if( datasetsEl != null ) {
			datasetsEl.find('input').on('click', function(event) {
				var tar = $(event.target);
				var key = tar.attr('ds-stats-key');
				if( tar.prop('checked') && typeof selectedDatasets[key] === 'undefined' ) {
					if( !multiSelect ) {
						removeAllDatasets();
					}
					showDataset(key);
				}
				else if( !tar.prop('checked') && typeof selectedDatasets[key] !== 'undefined' ) {
					removeDataset(key);
				}
			});
		}

		if( datasets.length > 1 ) {
			var firstInput = datasetsEl
				.find('input')
				.first();

			firstInput.prop('checked', true);
			showDataset(firstInput.attr('ds-stats-key'));
		}
		else if( datasets.length == 1 ){
			showDataset(datasets[0].key)
		}
	},
	__renderDatasets : function(target,multiSelect,datasets) {
		if( datasets.length <= 0 ) {
			return null;
		}
		target.after("<div class='datasets'></div>");

		var groupName = target.attr('id')+'_selection';

		var datasetsEl = target.next();
		for( var i=0,length=datasets.length; i < length; i++ ) {
			var set = datasets[i];
			datasetsEl.append(
				"<div class='set'>"+
				"<input type='"+(multiSelect ? 'checkbox' : 'radio')+"' name='"+groupName+"' ds-stats-key='"+set.key+"' />"+set.label+
				"</div>"
			);
		}
		return datasetsEl;
	}
};