var Options = {
	erzeugeLeereNamenBeispieleBox : function() {
		var xtraForm = $('#options-xtra');
		if( xtraForm.find('.namenBeispiele').size() == 0 ) {
			xtraForm.prepend('<div class="gfxbox namenBeispiele" style="width:300px"></div>');
		}
		else {
			xtraForm.find('.namenBeispiele').empty();
		}
		return xtraForm.find('.namenBeispiele');
	},
	entferneNamenBeispieleBox : function() {
		var xtraForm = $('#options-xtra');
		xtraForm.find('.namenBeispiele').remove();
	},
	generierePersonenNamenBeispiele : function() {
		var xtraForm = $('#options-xtra');
		DS.getJSON(
			{
				module:'options',
				action:'generierePersonenNamenBeispiele',
				generator: xtraForm.find('select[name=personenNamenGenerator]').val()
			},
			function(result) {
				if( typeof(result.namen) === 'undefinied' || result.namen.length == 0 ) {
					Options.entferneNamenBeispieleBox();
					return;
				}

				var box = Options.erzeugeLeereNamenBeispieleBox();
				$(result.namen).each(function(index,value) {
					box.append(value+"<br />");
				});
			}
		);
	},
	generiereSchiffsNamenBeispiele : function() {
		var xtraForm = $('#options-xtra');
		DS.getJSON(
			{
				module:'options',
				action:'generiereSchiffsNamenBeispiele',
				schiffsKlassenNamenGenerator: xtraForm.find('select[name=schiffsKlassenNamenGenerator]').val(),
				schiffsNamenGenerator: xtraForm.find('select[name=schiffsNamenGenerator]').val()
			},
			function(result) {
				if( typeof(result.namen) === 'undefinied' || result.namen.length == 0 ) {
					Options.entferneNamenBeispieleBox();
					return;
				}

				var box = Options.erzeugeLeereNamenBeispieleBox();
				var table = "<table>";
				$(result.namen).each(function(index,value) {
					table += "<tr><td>"+value.klasse+"</td><td>"+value.name+"</td></tr>";
				});
				table += "</table>";
				box.append(table);
			}
		);
	}
};
