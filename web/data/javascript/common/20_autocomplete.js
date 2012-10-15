var DsAutoComplete = {
	users : function(pattern, response) {
		var url = DS.getUrl();
		var params = {
				module:'search',
				action:'search',
				search:pattern.term,
				only:'users',
				max:5
		};

		jQuery.getJSON( url, params, function(result) {
			var data = [];
			for( var i=0; i < result.users.length; i++ ) {
				var user = result.users[i];
				data.push({label: user.name+" ("+user.id+")", value:user.plainname});
			}
			response(data);
		});
	}
}

$(document).ready(function() {
	DsTooltip.create();
	DsTooltip.update($("body"));
});