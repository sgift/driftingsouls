var Tradepost = {
	addSelectedItem : function() {
		var itemlist = $('#itemlist').get()[0];
		var list = $('#tradepost-waren table tbody');

		var option = itemlist.options[itemlist.selectedIndex];
		var paramid = option.value;
		var name = option.text;
		var picture = option.getAttribute("picture");
		var itemid = option.getAttribute("itemid");

		var isnpc = $('#isnpc').val() === "true";

		var content = "<tr>"+
				'<td><img src="'+picture+'" alt="" />'+name+'</td>'+
				'<td><input name="'+paramid+'buylimit" type="text" size="15" maxlength="15" value="0"/></td>'+
				"<td>"+
				'<input name="'+paramid+'buyprice" type="text" size="15" maxlength="15" value="0"/>'+
				'</td>';
		if( isnpc ) {
			content += '<td><input name="'+paramid+'buyrank" type="text" size="15" maxlength="15" value="0"/></td>';
		}
		content +=
			'<td><input name="'+paramid+'buybool" type="checkbox" value="1" checked="checked"/></td>'+
			'<td><input name="'+paramid+'saleslimit" type="text" size="15" maxlength="15" value="0"/></td>'+
			'<td><input name="'+paramid+'salesprice" type="text" size="15" maxlength="15" value="0"/></td>';
		if( isnpc ) {
			content += '<td><input name="'+paramid+'sellrank" type="text" size="15" maxlength="15" value="0"/></td>';
		}

		content += '<td><input name="'+paramid+'salebool" type="checkbox" value="1" checked="checked"/></td>';
		if( isnpc ) {
			content += '<td><input name="'+paramid+'fill" type="checkbox" value="1"/></td>';
		}
		content += '</tr>';

		list.append(content);
	}
};