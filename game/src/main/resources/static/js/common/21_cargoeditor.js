/**
 * Konstruiert einen grafischen Editor fuer Cargo-Strings.
 * Als einziger Parameter ist ein selektor anzugeben
 * zu einem Input-Feld, das den Cargo-String enthaelt.
 * Matchen mehrere Felder so wird nur das erste beruecksichtigt.
 * Es ist empfehlenswert ein hidden-Inputfeld zu verwenden.
 * @param selector Der Selektor
 * @returns {CargoEditor} Der CargoEditor oder null
 */
var CargoEditor = function(selector) {
	var CargoEditorView = function(target) {
		var cargoEditorView = null;

		function render(cargo, itemlist) {
			var tmpl = '<div class="cargoEditor"><table><tbody>'+
				'</tbody>'+
				'<tfoot>'+
				'<tr><td colspan="2"><select class="newEntry" size="1">'+
				'</select></td>'+
				'<td><input type="text" size="4" value="0" class="newEntryCount" /></td>'+
				'<td><button class="addEntry">+</button></td>'+
				'</tr>'+
				'</tfoot>'+
				'</table></div>';

			var html = DS.render(tmpl, {});

			target.after(html);

			cargoEditorView = target.next();

			cargoEditorView.find('.addEntry').on('click keypress', function(event) {
				if( event.type === 'keypress' ) {
					if( event.which != 32 && event.which != 13 ) {
						return;
					}
				}
				var itemId = parseInt(cargoEditorView.find('.newEntry').val());
				var count = parseInt(cargoEditorView.find('.newEntryCount').val());

				cargoEditorView.trigger('newEntry', {id:itemId, count:count});
				return false;
			});
		}

		this.updateNewEntrySection = function(itemlist) {
			var tmpl = '{{#itemlist}}'+
				'<option value="{{id}}">{{{name}}}</option>'+
				'{{/itemlist}}';
			var html = DS.render(tmpl, {itemlist:itemlist.toArray()});

			var newEntry = cargoEditorView.find(".newEntry");
			newEntry.empty();
			newEntry.append(html);
			cargoEditorView.find(".newEntryCount").val("0");
		};

		this.updateCargoEntry = function(entry) {
			var tableContent = cargoEditorView.find("tbody");
			tableContent.append(renderCargoEntry(entry));
			tableContent.find('.itemCount[item-id='+entry.id+']').on('change', function(event) {
				var itemId = parseInt($(this).attr('item-id'));
				var count = parseInt($(this).val());

				cargoEditorView.trigger("entryChanged", {id:itemId, count:count});
			});
		};

		function renderCargoEntry(entry) {
			var tmpl = '<tr>'+
				'<td class="picture"><img src="{{image}}" alt="" /></td>'+
				'<td>{{{name}}}</td>'+
				'<td><input class="itemCount" type="text" size="4" value="{{count1}}" item-id="{{id}}" /></td>'+
				'<td></td>'+
				'</tr>';
			return DS.render(tmpl, entry);
		}

		this.renderMain = function(cargo, itemlist) {
			this.updateNewEntrySection(itemlist);
			var tableContent = cargoEditorView.find("tbody");

			for( var i=0, length=cargo.length; i < length; i++ ) {
				var entry = cargo[i];
				tableContent.append(renderCargoEntry(entry));
			}

			tableContent.find('.itemCount').on('change', function(event) {
				var itemId = parseInt($(this).attr('item-id'));
				var count = parseInt($(this).val());

				cargoEditorView.trigger("entryChanged", {id:itemId, count:count});
			});
		};

		this.on = function(event, listener) {
			cargoEditorView.on(event, listener);
		};

		this.updateItemList = function(cargo) {
			var existingItems = [];
			cargoEditorView.find('.itemCount').each(function() {
				var itemId = parseInt($(this).attr('item-id'));
				var count = 0;
				for( var i=0; i < cargo.length; i++ ) {
					if( cargo[i].id == itemId ) {
						count = cargo[i].count1;
					}
				}

				if( count == 0 ) {
					$(this).parentsUntil('tbody', 'tr').remove();
				}
				else {
					$(this).val(count);
					existingItems[itemId] = itemId;
				}
			});

			for( var i=0, length=cargo.length; i < length; i++ ) {
				var entry = cargo[i];
				if( typeof existingItems[entry.id] !== "undefined" ) {
					continue;
				}

				this.updateCargoEntry(entry);
			}
		};

		render();
	};

	var CargoEditorController = function(target) {
		var view = new CargoEditorView(target);
		var cargo = new CargoModel(target.val());

		var itemlist = null;
		ItemListFactory.visibleItems(function(model) {
			itemlist = model;
			renderInitialCargoEditor();
		});

		function cargoEntryToViewModel(entry) {
			var item = itemlist.getById(entry.id);
			if( item == null ) {
				return null;
			}
			var cargoitem = {
				image: item.picture,
				name: item.name,
				count1 : entry.count,
				id: item.id
			};
			return cargoitem;
		}

		function transformCargoToViewModel() {
			var viewCargo = [];
			cargo.each(function(entry) {
				var cargoitem = cargoEntryToViewModel(entry);
				if( cargoitem == null ) {
					return;
				}

				viewCargo.push(cargoitem);
			});
			return viewCargo;
		}

		function renderInitialCargoEditor() {
			var viewCargo = transformCargoToViewModel();
			view.renderMain(viewCargo, itemlist.missingInCargo(cargo));

			cargo.onchange(function() {
				view.updateItemList(transformCargoToViewModel());
				view.updateNewEntrySection(itemlist.missingInCargo(cargo));
				target.val(cargo.save());
			});

			view.on('entryChanged', function(event, item) {
				cargo.setResourceCount(item.id, item.count);
			});
			view.on('newEntry', function(event, item) {
				if( item.count > 0 ) {
					cargo.setResourceCount(item.id, item.count);
				}
			});
		}
	};

	var target = $(selector).first();
	if( target.size() == 0 ) {
		return null;
	}
	new CargoEditorController(target);
};