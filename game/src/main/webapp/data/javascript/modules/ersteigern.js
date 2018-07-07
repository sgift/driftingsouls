$(document).ready(function() {
	"use strict";

	$("#ueberweisen_to").autocomplete({
		source : DsAutoComplete.users,
		html:true
	});
});