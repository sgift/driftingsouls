var Base = {
	highlightBuilding : function(buildingId) {
		$('#baseMap').addClass('fade');
		$('#baseMap .building'+buildingId).addClass('highlight');
	},
	noBuildingHighlight : function() {
		$('#baseMap').removeClass('fade');
		$('#baseMap .tile').removeClass('highlight');
	}
};