/**
 * Repraesentation einer Liste von Items. Dem Konstruktor
 * kann dazu eine vom Server gelieferte Liste uebergeben werden.
 * Wird kein Parameter angegeben so ist die Liste leer.
 * @param {Array} [itemlist] (optional) Die Liste der Items
 * @returns Die Instanz
 */
var ItemListModel = function(itemlist) {
	var __itemlist = typeof itemlist === "undefined" ? [] : itemlist;

	/**
	 * Ermittelt, sofern vorhanden, zur angegebenen Item-ID das
	 * zugehoerige Item.
	 * @param {Number} itemid Die ID des Items
	 */
	this.getById = function(itemid) {
		for( var i=0, length=__itemlist.length; i < length; i++ ) {
			if( __itemlist[i].id == itemid ) {
				return __itemlist[i];
			}
		}
		return null;
	};

	/**
	 * Konvertiert die Liste in ein Array von Items.
	 * @returns {Array} Das Array mit allen Itemdefinitionen
	 */
	this.toArray = function() {
		return __itemlist;
	};

	/**
	 * Erzeugt eine gefilterte Itemliste, aus der alle
	 * Items entfernt wurden, die im angegebenen Cargo-Modell
	 * vorhanden sind.
	 * @param {CargoModel} cargo Das Cargo-Modell dessen Items nicht ins Ergebnis kommen sollen
	 * @returns {ItemListModel} Das entsprechend gefilterte Item-Modell
	 */
	this.missingInCargo = function(cargo) {
		var filteredList = [];
		for( var i=0, length=__itemlist.length; i < length; i++ ) {
			if( !cargo.containsItem(__itemlist[i]) ) {
				filteredList.push(__itemlist[i]);
			}
		}
		return new ItemListModel(filteredList);
	};
};

var ItemListFactory = {
	/**
	 * Fuellt die Itemliste mit der Liste aller fuer
	 * den Benutzer sichtbaren Items. Die bestehende Liste
	 * wird dabei ueberschrieben. Der Ladevorgang
	 * erfolgt asynchron. Sobald er beendet wurde
	 * wird der angegebene Listener aufgerufen.
	 * @param {ItemListFactory~visibleItemsCallback} callback Die Callback-Funktion
	 */
	visibleItems : function(callback) {
		DS.getJSON(
			{module: 'iteminfo', action: 'ajax'},
			function (result) {
				var model = new ItemListModel(result.items);
				if (typeof callback !== "undefined") {
					callback(model);
				}
			});
	}

	/**
	 * This callback is displayed as part of the Requester class.
	 * @callback ItemListFactory~visibleItemsCallback
	 * @param {ItemListModel} Das vollstaendige ItemListModel
	 */
};