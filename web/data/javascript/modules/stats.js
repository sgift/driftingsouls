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
	chart : function(selector, xaxis, yaxis, datasets) {
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
		el.after("<div class='datasets'></div>");

		var datasetsEl = el.next();
		for( var i=0,length=datasets.length; i < length; i++ ) {
			var set = datasets[i];
			datasetsEl.append("<div class='set'><input type='checkbox' ds-stats-key='"+set.key+"' />"+set.label+"</div>");
		}

		var self = this;
		datasetsEl.find('input').on('click', function(event) {
			var key = $(event.target).attr('ds-stats-key');
			if( event.target.checked && typeof selectedDatasets[key] === 'undefined' ) {
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
			}
			else if( !event.target.checked && typeof selectedDatasets[key] !== 'undefined' ) {
				delete selectedDatasets[key];
				redraw();
			}
		});
	}
};