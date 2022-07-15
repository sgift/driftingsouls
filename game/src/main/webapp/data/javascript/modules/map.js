var MapController = {
	__starmap : null,
	init : function() {
		var self = this;
		$(document).ready(function() {
			self.__createJumpnodePopup();
			self.showSystemSelection();
			self.__starmap = new Starmap($('#mapcontent'));
		});
	},
	showSystemSelection : function() {
		var self = this;
		var systemauswahl = $('#systemauswahl');
		systemauswahl.dsBox('show', {
			width:400,
			center:true,
			draggable:true,
			closeButton:false
		});
		systemauswahl.find('form')
			.off('submit')
			.on('submit', function(event) {
				event.preventDefault();
				self.load();
				return false;
			})
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
	showJumpToPosition : function() {
		if( !this.__starmap.isReady() ) {
			return;
		}
		var self = this;
		new StarmapGotoLocationPopup(function(x, y) {
			if( !x || !y ) {
				return;
			}
			x = parseInt(x);
			y = parseInt(y);
			self.__starmap.gotoLocation(x,y);
		});
	},
	load : function() {
		var mapform = $('#systemauswahl form');
		var sys = mapform.find('select[name=sys]').val();
		var x = mapform.find('input[name=xstart]').val();
		var y = mapform.find('input[name=ystart]').val();
		var adminSicht = mapform.find('input[name=adminSicht]:checked').val();

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