var Options = {
	generierePersonenNamenBeispiele : function() {
		var xtraForm = $('#options-xtra');
		DS.getJSON(
			{
				module:'options',
				action:'generierePersonenNamenBeispiele',
				generator: xtraForm.find('select[name=personenNamenGenerator]').val()
			},
			function(result) {
				if( result.length == 0 ) {
					xtraForm.find('.personenNamenBeispiele').remove();
					return;
				}
				if( xtraForm.find('.personenNamenBeispiele').size() == 0 ) {
					xtraForm.prepend('<div class="gfxbox personenNamenBeispiele" style="width:300px"></div>');
				}
				else {
					xtraForm.find('.personenNamenBeispiele').empty();
				}
				var box = xtraForm.find('.personenNamenBeispiele');
				$(result).each(function(index,value) {
					box.append(value+"<br />");
				});
			}
		);
	}
};
