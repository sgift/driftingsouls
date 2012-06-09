var Base = {
	highlightBuilding : function(buildingId) {
		$('#baseMap').addClass('fade');
		$('#baseMap .building'+buildingId).addClass('highlight');
	},
	noBuildingHighlight : function() {
		$('#baseMap').removeClass('fade');
		$('#baseMap .tile').removeClass('highlight');
	},
	changeName : function() {
		var el = $('#baseName');
		var name = el.text();
		el.empty();
		
		var cnt = "<form action='"+getDsUrl()+"' method='post' style='display:inline'>";
		cnt += "<input name='newname' type='text' size='15' maxlength='50' value='"+name+"' />";
		cnt += "<input name='col' type='hidden' value='"+$('#baseId').val()+"' />";
		cnt += "<input name='module' type='hidden' value='base' />";
		cnt += "<input name='action' type='hidden' value='changeName' />";
		cnt += "&nbsp;<input type='submit' value='umbenennen' />";
		cnt += "</form>";
		
		el.append(cnt);
	}
};