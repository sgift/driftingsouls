var Schiff = {
	openImpObjects : function(system) {
		var el = jQuery("#impobjectsbox");
		el.css('display','block');
		
		var self = this;
		var url = getDsUrl();
		jQuery.getJSON(url,
			{'system': system, 'module': 'impobjects', 'action' : 'json'},
			function(data) {
				self.__renderImpObjects(data);
			});
	},
	fillMoveTo : function(x,y) {
		document.getElementsByName('navigation_ops[targetx]')[0].value=x;
		document.getElementsByName('navigation_ops[targety]')[0].value=y;
		this.closeImpObjects();
	},
	__renderImpObjects : function(data) {
		var content = jQuery('#impobjectsbox .content');
		content.children().remove();
		
		var text = "<h3>Wichtige Objekte in "+data.system.name+" ("+data.system.id+")</h3>";
		text += "<ul>";
		if( data.jumpnodes.length > 0 ) {
			text += "<li>Sprungpunkte<dl>";
			for( var i=0, length=data.jumpnodes.length; i < length; i++ ) {
				var jn = data.jumpnodes[i];
				text += "<dt><a title='Kurs festlegen' href='#' onclick='Schiff.fillMoveTo("+jn.x+","+jn.y+")'>"+jn.x+"/"+jn.y+"</a></dt>";
				text += "<dd>"+jn.name+"</dd>";
			}
			text += "</dl></li>";
		}
		if( data.posten.length > 0 ) {
			text += "<li>Handelsposten<dl>";
			for( var i=0, length=data.posten.length; i < length; i++ ) {
				var posten = data.posten[i];
				text += "<dt><a title='Kurs festlegen' href='#' onclick='Schiff.fillMoveTo("+posten.x+","+posten.y+")'>"+posten.x+"/"+posten.y+"</a></dt>";
				text += "<dd>"+posten.name+"</dd>";
			}
			text += "</dl></li>";
		}
		if( data.bases.length > 0 ) {
			text += "<li>Eigene Basen<dl>";
			for( var i=0, length=data.bases.length; i < length; i++ ) {
				var base = data.bases[i];
				text += "<dt><a title='Kurs festlegen' href='#' onclick='Schiff.fillMoveTo("+base.x+","+base.y+")'>"+base.x+"/"+base.y+"</a></dt>";
				text += "<dd>"+base.name+"</dd>";
			}
			text += "</dl></li>";
		}
		content.append(text);
	},
	closeImpObjects : function() {
		var el = jQuery("#impobjectsbox");
		el.css('display', 'none');
	}
};