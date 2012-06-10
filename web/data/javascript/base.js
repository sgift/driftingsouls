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
		cnt += "<input name='col' type='hidden' value='"+this.__getBaseId()+"' />";
		cnt += "<input name='module' type='hidden' value='base' />";
		cnt += "<input name='action' type='hidden' value='changeName' />";
		cnt += "&nbsp;<input type='submit' value='umbenennen' />";
		cnt += "</form>";

		el.append(cnt);
	},
	showBuilding : function(tileId) {
		var self = this;

		if( $('#buildingBox').size() == 0 ) {
			$('body').append('<div id="buildingBox" />');
			$('#buildingBox').dsBox({
				draggable:true,
				center:true
			});
		}
		var buildingBox = $('#buildingBox');
		buildingBox.dsBox('show');
		var content = buildingBox.find('.content');
		content.empty();
		content.append('Lade...');

		DS.getJSON({
			module:'building',
			action:'ajax',
			col:this.__getBaseId(),
			field:tileId
		}, function(response) {
			self.__parseBuildingResponse(response)
		});
	},
	__parseBuildingResponse : function(resp) {
		var content = $('#buildingBox .content');
		content.empty();

		if( resp.noJsonSupport ) {
			document.location.href=DS.getUrl()+'?module=building&action=default&col='+resp.col+'&field='+resp.field;
			return;
		}

		var out = '<div class="head">';
		out += '<img src="./'+resp.building.picture+'" alt="" /> '+resp.building.name;
		out += '</div>';
		out += '<div class="message" />'
		out += '<div class="status">Status: ';
		if( resp.building.active ) {
			out += '<span class="aktiv">Aktiv</span>';
		}
		else {
			out += '<span class="inaktiv">Inaktiv</span>';
		}

		out += '</div>';

		// BUILDING
		if( resp.building.type === 'DefaultBuilding' ) {
			out += this.__defaultBuildingHandler.generateOutput(resp);
		}
		else {
			out += "Unbekannter Gebäudetyp: "+resp.building.type;
		}

		out += '<ul class="aktionen">Aktionen: ';

		if( resp.building.deakable ) {
			if( resp.building.active ) {
				out += '<li><a id="startStopBuilding" href="'+DS.getUrl()+'?module=building&action=shutdown&col='+resp.col+'&field='+resp.field+'">deaktivieren</a></li>';
			}
			else {
				out += '<li><a id="startStopBuilding" href="'+DS.getUrl()+'?module=building&action=start&col='+resp.col+'&field='+resp.field+'">aktivieren</a></li>';
			}
		}

		if( !resp.building.kommandozentrale ) {
			out += '<li><a class="error" href="'+DS.getUrl()+'?module=building&action=demo&col='+resp.col+'&field='+resp.field+'">abreissen</a></li>';
		}
		else {
			out += '<li><a class="error" href="DS.ask(\'Wollen sie den Asteroiden wirklich aufgeben?\',\''+DS.getUrl()+'?module=building&action=demo&col='+resp.col+'&field='+resp.field+'\')">Asteroid aufgeben</a></li>';
		}

		out += "</ul>";

		content.append(out);
		DsTooltip.update(content);

		this.__bindBuildingStartStop(resp.col, resp.field, resp.building.active);
	},
	__bindBuildingStartStop : function(col, field, active) {
		var self = this;
		$('#startStopBuilding').unbind('click.startStop');

		if( active ) {
			$('#startStopBuilding').bind('click.startStop', function() {
				DS.getJSON(
					{module:'building', action:'shutdownAjax', 'col':col, 'field':field},
					function(resp) {self.__parseBuildingShutdown(resp)}
				);
				return false;
			});
		}
		else {
			$('#startStopBuilding').bind('click.startStop', function() {
				DS.getJSON(
					{module:'building', action:'startAjax', 'col':col, 'field':field},
					function(resp) {self.__parseBuildingStart(resp)}
				);
				return false;
			});
		}
	},
	__parseBuildingShutdown : function(resp) {
		if( !resp.success ) {
			$('#buildingBox .message')
				.empty()
				.append(resp.message);

			return;
		}

		var link = $('#baseMap .p'+resp.field).html();

		$('#baseMap').append('<div class="o'+resp.field+' overlay">'+link+'</div>');
		$('#baseMap .o'+resp.field+' img').attr('src','./data/buildings/overlay_offline.png');

		$('#startStopBuilding').text('aktivieren');
		$('#buildingBox .status .aktiv').remove();
		$('#buildingBox .status').append('<span class="inaktiv">Inaktiv</span>');

		this.__bindBuildingStartStop(resp.col, resp.field, false);

		this.__refreshCargoAndActions();
	},
	__parseBuildingStart : function(resp) {
		if( !resp.success ) {
			$('#buildingBox .message')
				.empty()
				.append(resp.message);

			return;
		}

		$('#baseMap .o'+resp.field).remove();

		$('#startStopBuilding').text('deaktivieren');
		$('#buildingBox .status .inaktiv').remove();
		$('#buildingBox .status').append('<span class="aktiv">Aktiv</span>');

		this.__bindBuildingStartStop(resp.col, resp.field, true);

		this.__refreshCargoAndActions();
	},
	__refreshCargoAndActions : function() {
		var self = this;
		DS.get(
				{module:'base', col:this.__getBaseId()},
				function(resp) {self.__parseRefreshCargoAndActions(resp)}
		);
	},
	__parseRefreshCargoAndActions : function(resp) {
		var newCargoBox = $(resp).find('#cargoBox');
		$('#cargoBox').replaceWith(newCargoBox);

		var newActionBox = $(resp).find('.buildingActions');
		$('.buildingActions').replaceWith(newActionBox);

		var newStatsBox = $(resp).find('#statsBox');
		$('#statsBox').replaceWith(newStatsBox);
	},
	__getBaseId : function() {
		return $('#baseId').val();
	},
	__defaultBuildingHandler : {
		generateOutput : function(resp) {
			var ui = resp.buildingUI;

			out = '<dl class="defaultBuilding"><dt>Verbraucht:</dt>';
			out += "<dd>";

			var entry = false;
			for( var i=0; i < ui.consumes.cargo.length; i++ ) {
				var res = ui.consumes.cargo[i];
				out += '<img src="'+res.image+'" alt="" title="'+res.plainname+'" />'+res.cargo1+' ';
				entry = true;
			}

			if( ui.consumes.e > 0 ) {
				out += '<img src="./data/interface/energie.gif" alt="" title="Energie" />'+ui.consumes.e+' ';
				entry = true;
			}
			if( !entry ) {
				out += '-';
			}

			out += "</dd>";

			out += "<dt>Produziert:</dt>";
			out += "<dd>";

			entry = false;
			for( var i=0; i < ui.produces.cargo.length; i++ ) {
				var res = ui.produces.cargo[i];
				out += '<img src="'+res.image+'" alt="" title="'+res.plainname+'" />'+res.cargo1+' ';
				entry = true;
			}

			if( ui.produces.e > 0 ) {
				out += '<img src="./data/interface/energie.gif" alt="" title="Energie" />'+ui.consumes.e+' ';
				entry = true;
			}
			if( !entry ) {
				out += '-';
			}

			out += "</dd></dl>";

			return out;
		}
	}
};