var Admin = {};

Admin.CreateObjectsFromImage = {
	objectTypeChanged : function(select) {
		var jq = $(select);
		jq.parents().filter('td:first').find('input[type=text]').val('');

		var form = jq.parents().filter("form:first");
		form.find('input[name=doupdate]').attr('checked', false);

		form.find('input[type=submit]').click();
	}
};