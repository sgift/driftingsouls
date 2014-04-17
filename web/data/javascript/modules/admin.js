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

Admin.initMenu = function() {
	var list = $('#admin .treemenu>ul');
	list.find('p').click(function (event) {
			$(this).parent().toggleClass('expanded');
			$(this).parent().children('ul').toggle('fast');
		});
	list.find('li:has(ul):not(.expanded)').addClass('collapsed')
		.children('ul').hide();
};

Admin.createEntityTable = function(params) {
	var items = new ItemListModel([]);
	var updateCargos = function() {
		$("#admin .gridcargo:not(.done)").each(function(idx, el) {
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

	items.fillWithVisibleItems(updateCargos);

	$(document).ready(function() {
		params.onSelectRow = function (id) {
			var entityField = $('#admin .adminSelection *[name=entityId]');
			entityField.val(id);
		};
		params.gridComplete = function() {
			updateCargos();
		};
		$('#entityList').jqGrid(params);
	});
};

jQuery.extend($.fn.fmatter , {
	picture : function(cellvalue, options, rowdata) {
		return '<img class="gridpicture" src="'+cellvalue+'" />';
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