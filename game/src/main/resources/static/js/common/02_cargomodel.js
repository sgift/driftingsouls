/**
 * Ein Cargo-Modell analog zur Cargo-Klasse.
 * Der Konstruktor nimmt genau einen Parameter entgegen,
 * einen serialisierten Cargo-String im Item-Format.
 * @param {String} cargoStr Der serialisierte Cargo-String
 */
var CargoModel = function(cargoStr) {
	var changeEventListener = [];

	function parseCargo(cargoStr) {
		if( cargoStr.indexOf(',') > -1 ) {
			cargoStr = cargoStr.substr(cargoStr.lastIndexOf(',')+1);
		}
		var itemStrArray = cargoStr.split(';');
		var itemArray = [];
		for( var i=0; i < itemStrArray.length; i++ ) {
			var item = itemStrArray[i].split('|');
			if( item.length < 2 || item.length > 4 ) {
				continue;
			}
			for( var j=0; j < item.length; j++ ) {
				item[j] = parseInt(item[j]);
			}
			while( item.length < 4 ) {
				item.push(0);
			}

			itemArray.push(item)
		}
		return itemArray;
	}

	/**
	 * Gibt den gesamten Cargo als Array zurueck.
	 * @return {Array} Der Cargo-Inhalt
	 */
	this.getAll = function() {
		return cargo;
	};

	/**
	 * Iterator ueber alle Elemente des Cargos. Fuer jedes
	 * Element wird der angegebene Callback aufgerufen.
	 * Einziger Parameter ist ein Objekt mit den Daten
	 * des momentanen Eintrags.
	 */
	this.each = function(callback) {
		for( var i=0, length=cargo.length; i < length; i++ ) {
			callback({
				id:cargo[i][0],
				count:cargo[i][1],
				quest:cargo[i][2],
				uses:cargo[i][3]
			});
		}
	};

	/**
	 * Ermittelt die Menge des angegebenen Items im Cargo.
	 * Falls das Item nicht vorhanden ist wird 0 zurueckgegeben.
	 * Das Item kann entweder mittels ID angegeben werden oder
	 * als Objekt, dass eine id-Property besitzt.
	 */
	this.getResourceCount = function(item) {
		for( var i=0, length=cargo.length; i < length; i++ ) {
			if( typeof item === "object" ) {
				if( item.id === cargo[i][0] ) {
					return cargo[i][1];
				}
			}
			else if( item === cargo[i][0] ){
				return cargo[i][1];
			}
		}
		return 0;
	};

	/**
	 * Gibt zurueck, ob das angegebene Item mit einer Menge
	 * groesser 0 im Cargo vorkommt. Das Item kann entweder
	 * mittels ID angegeben werden oder
	 * als Objekt, dass eine id-Property besitzt.
	 */
	this.containsItem = function(item) {
		return this.getResourceCount(item) > 0;
	};

	/**
	 * Serialisiert den Cargo als Cargo-String im Item-Format.
	 * @return {String} Der serialisierte Cargo-String
	 */
	this.save = function() {
		var result = "";
		for( var i=0, length=cargo.length; i < length; i++ ) {
			if( result.length > 0 ) {
				result += ";";
			}
			result += cargo[i][0]+"|"+cargo[i][1]+"|"+cargo[i][2]+"|"+cargo[i][3];
		}
		return result;
	};

	/**
	 * Setzt, wie haeufig das angegebene Item im Cargo
	 * vorkommt. Die Anzahl 0 entfernt das Item.
	 * Das Item kann entweder mittels ID angegeben werden oder
	 * als Objekt, dass eine id-Property besitzt.
	 */
	this.setResourceCount = function(item, count) {
		if( typeof item !== "object" ) {
			item = {id:item};
		}
		for( var i=0, length=cargo.length; i < length; i++ ) {
			if( item.id === cargo[i][0] ) {
				if( count === 0 ) {
					cargo.splice(i,1);
					fireChangeEvent();
					return;
				}

				cargo[i][1] = count;

				fireChangeEvent();
				return;
			}
		}
		cargo.push([item.id, count, 0, 0]);
		fireChangeEvent();
	};

	function fireChangeEvent() {
		for( var i=0, length=changeEventListener.length; i < length; i++ ) {
			changeEventListener[i]();
		}
	}

	/**
	 * Registriert einen Listener im Cargo,
	 * der bei jeder Aenderung gefeuert wird.
	 * Dem Listener wird dabei kein Argument uebergeben.
	 */
	this.onchange = function(listener) {
		changeEventListener.push(listener);
	};

	var cargo = parseCargo(cargoStr);
};