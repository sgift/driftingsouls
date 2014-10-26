var Admin = {};

Admin.CreateObjectsFromImage = {
	objectTypeChanged : function(select) {
		var jq = $(select);
		jq.parents().filter('td:first').find('input[type=text]').val('');

		var form = jq.parents().filter("form:first");
		form.find('input[name=doupdate]').prop('checked', false);

		form.find('input[type=submit]').click();
	}
};

Admin.initMenu = function() {
	var list = $('#admin .treemenu>ul');
	list.find('p').click(function (event) {
			$(this).parent().toggleClass('expanded');
			$(this).parent().children('ul').toggle('fast');
		});
	list.find('li:has(ul):not(.expanded)').addClass('collapsed')
		.children('ul').hide();
};

Admin._createInput = function(inputModel) {
	if( inputModel.typ === "select" ) {
		var select = $('<select size="1" name="'+inputModel.name+'"></select>');
		if( inputModel.nullOption != null ) {
			select.append($('<option></option>').html(v))
		}
		$.each(inputModel.options, function(k, v) {
			select.append($('<option></option>').val(k).html(v));
		});
		select.val(inputModel.selected);
		select.prop('disabled', inputModel.disabled);
		return select;
	}
	else if( inputModel.typ === "textfield" ) {
		var input = $('<input type="text" name="'+inputModel.name+'" id="'+inputModel.id+'" />');
		if( inputModel.autoNumeric ) {
			input.autoNumeric('init', inputModel.autoNumeric);
		}
		input.val(input.value);
		input.prop('disabled', inputModel.disabled);
		return input;
	}
	return "Unbekanntes Eingabeelement";
};

Admin.openEntityEditor = function(namedplugin) {
	function beginSelectionBox(jq) {
		jq.append('<form action="./ds" method="post">' +
			'<input type="hidden" name="namedplugin" value="'+namedplugin+'" />' +
			'<input type="hidden" name="module" value="admin" />'+
			'<input type="submit" name="choose" value="Ok" /></form>');
	}

	function addForm(jq) {
		jq.append('<form action="./ds" method="post">'+
			'<input type="hidden" name="namedplugin" value="'+namedplugin+'" />' +
			'<input type="hidden" name="module" value="admin" />' +
			'<input type="submit" name="add" value="+" />' +
			'</form>');
	}

	DS.getJSON({module:'admin', action:'entityPluginOverviewAction', namedplugin:namedplugin}, function(data) {
		var adminplugin = $('#adminplugin');
		adminplugin.empty().append('<div class="gfxbox adminSelection" style="width:390px"></div>' +
			'<div id="entityListWrapper"><table id="entityList"><tr><td></td></tr></table><div id="pager"></div></div>');

		var selection = adminplugin.find('.adminSelection');
		if( data.entitySelection.allowSelection ) {
			beginSelectionBox(selection);
			var form = selection.find('form');

			form.prepend(Admin._createInput(data.entitySelection.input));
		}
		if( data.entitySelection.allowAdd ){
			addForm(selection);
		}

		Admin.createEntityTable(data.table);

		var admin = $('#admin');
		admin.find('.treemenu a').removeClass("active");
		admin.find('.treemenu a[data-namedplugin="'+namedplugin+'"]').addClass("active");
	});
};

Admin.createEntityTable = function(params) {
	var items = null;
	ItemListFactory.visibleItems(function(itemmodel) {
		items = itemmodel;
		updateCargos();
	});

	var updateCargos = function() {
		$("#admin .gridcargo:not(.done)").each(function(idx, el) {
			if( items == null ) {
				return;
			}

			el = $(el);
			var cargoModel = new CargoModel(el.text());
			el.empty();
			cargoModel.each(function(entry) {
				var item = items.getById(entry.id);
				if( item == null ) {
					return null;
				}
				el.append('<img src="'+item.picture+'" title="'+item.name+'" /> '+entry.count+" ");
			});

			el.addClass("done");
		});
	};

	$(document).ready(function() {
		params.onSelectRow = function (id) {
			var entityField = $('#admin .adminSelection *[name=entityId]');
			entityField.val(id);
		};
		params.gridComplete = function() {
			updateCargos();
		};
		$('#entityList').jqGrid(params);
		var searchColumn = false;
		$.each(params.colModel, function(idx, val) {
			searchColumn |= val.search || false;
		});
		if( searchColumn ) {
			$('#entityList').jqGrid('filterToolbar', {});
		}
	});
};

Admin.createEditTable = function(name, model) {
	function postprocessRowData(rowData) {
		for( var i=0; i < rowData.length; i++ ) {
			var row = rowData[i];

			for( var j=0; j < model.colModel.length; j++ ) {
				var colModel = model.colModel[j];

				if( colModel.edittype === 'select' ) {
					// value statt label uebertragen
					// (standardmaessig gibt jqgrid hier das label zurueck!)
					var optionsList = colModel.editoptions.value.split(';');

					for( var k=0; k < optionsList.length; k++ ) {
						var option = optionsList[k].split(':');
						if( row[colModel.name] === option[1] ) {
							row[colModel.name] = option[0];
							break;
						}
					}
				}
			}
		}
	}

	function saveRowData($table) {
		var rowData = $table.jqGrid('getRowData');
		postprocessRowData(rowData);
		$('#'+name+'_data').val(JSON.stringify(rowData))
	}

	var lastsel2 = null;

	var $table = $('#' + name);
	model.datatype = "local";
	model.rowList = [];
	model.pgbuttons = false;
	model.pgtext = null;
	model.viewrecords = true;

	model.onSelectRow = function(id){
		if(id && id!==lastsel2){
			$table.restoreRow(lastsel2);
			$table.editRow(id,true);
			lastsel2=id;
		}
	};
	$table.jqGrid(model);
	$table.jqGrid('navGrid','#gridpager',{refresh:false,search:false,edit:false,add:false,del:false},{},{},{}, {});

	$table.jqGrid('navButtonAdd','#gridpager',{caption:'Hinzufügen',
		buttonicon:"ui-icon-plus",
		onClickButton: function(){
			$table.jqGrid('addRowData', $table.getGridParam('reccount')+1, {}, 'last');
			saveRowData($table);
		},
		position:"last"});

	$table.jqGrid('navButtonAdd','#gridpager',{caption:'Entfernen',
		buttonicon:"ui-icon-trash",
		onClickButton: function(){
			if( lastsel2 != null ) {
				$table.jqGrid('delRowData', lastsel2);
				saveRowData($table);
			}
		},
		position:"last"});

	$table.on('jqGridInlineAfterSaveRow', function(evt){
		lastsel2=null;
		saveRowData($table);
	});
};

jQuery.extend($.fn.fmatter , {
	picture : function(cellvalue, options, rowdata) {
		if( cellvalue != null && cellvalue != '' ) {
			return '<img class="gridpicture" src="' + cellvalue + '" />';
		}
		else {
			return '';
		}
	},
	textarea : function(cellvalue, options, rowdata) {
		return '<div class="gridtextarea">'+cellvalue+'</div>';
	},
	cargo : function(cellvalue, options, rowdata) {
		var out = '<div class="gridcargo">';
		out += cellvalue;
		out +='</div>';
		return out;
	}
});