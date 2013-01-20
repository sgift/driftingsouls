var MapController = {
	__starmap : null,
	init : function() {
		var self = this;
		$(document).ready(function() {
			self.__createJumpnodePopup();
			$('#systemauswahl').dsBox('show', {
				width:400,
				center:true,
				draggable:true,
				closeButton:false
			});
			self.__starmap = new Starmap($('#mapcontent'));
		});
	},
	showSystemSelection : function() {
		$('#systemauswahl').dsBox('show');
	},
	__createJumpnodePopup : function() {
		if( $('#jumpnodebox').size() == 0 ) {
			$('body').append('<div id="jumpnodebox"><h3>Sprungpunkte</h3><div id="jumpnodelist"><span>Kein System ausgewählt.</span></div></div>');
			$('#jumpnodebox').dsBox({
				center:true,
				width:400,
				draggable:true
			});
		}
	},
	showJumpnodeList : function() {
		$('#jumpnodebox').dsBox('show');
	},
	load : function() {
		var sys = document.mapform.sys.value;
		var x = document.mapform.xstart.value;
		var y = document.mapform.ystart.value;
		var adminSicht = 0;
		if( document.mapform.adminSicht && document.mapform.adminSicht.checked) {
			adminSicht = 1;
		}

		$('#systemauswahl').dsBox('hide');

		var self = this;

		this.__starmap.load(sys,x,y, {
			request : {
				admin : adminSicht
			},
			loadCallback : function(data) {
				self.__updateJumpnodeList(data);
			}
		});
	},
	__updateJumpnodeList : function(data)
	{
		var listEl = $('#jumpnodelist');
		listEl.children().remove();

		var jns = '<table class="datatable">';

		for( var i=0; i < data.jumpnodes.length; i++ )
		{
			var jn = data.jumpnodes[i];

			jns += '<tr>';
			jns += '<td><span class="nobr">'+jn.x+'/'+jn.y+'</span></td>';
			jns += '<td>nach</td>';
			jns += '<td><span class="nobr">'+jn.name+' ('+jn.systemout+')'+jn.blocked+'</span></td>';
			jns += '</tr>';
		}
		jns += '</table>';

		listEl.append(jns);
	}
};