package net.driftingsouls.ds2.server.modules.admin.editoren;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interface fuer Klassen zur Erstellung von Formelementen zu einer Entity.
 * @param <E> Der Typ der Entity
 */
public interface FormElementCreator<E>
{
	/**
	 * Fuegt einen Generator fuer ein Eingabefeld zum Form hinzu.
	 * @param generator Der Generator
	 * @param <T> Der Typ des Generators
	 * @return Der Generator
	 */
	<T extends CustomFieldGenerator<E>> T custom(T generator);

	/**
	 * Generiert ein Eingabefeld (Editor) fuer ein via {@link net.driftingsouls.ds2.server.framework.DynamicContent}
	 * gemanagetes Bild.
	 * @param label Der Label fuer das Eingabefeld
	 * @param getter Der getter für das Feld
	 * @return Der erzeugte Generator
	 */
	DynamicContentFieldGenerator<E> dynamicContentField(String label, Function<E, String> getter, BiConsumer<E, String> setter);

	/**
	 * Erzeugt ein Eingabefeld (Editor) in Form eines nicht editierbaren Werts.
	 * @param label Der Label zum Wert
	 * @param getter Der getter des Werts
	 */
	<T> LabelGenerator<E,T> label(String label, Function<E, T> getter);

	/**
	 * Erzeugt ein Editor ohne Aenderungsfunktionen fuer ein Bild.
	 * @param label Der Label zum Wert
	 * @param getter Der getter des Werts
	 */
	PictureGenerator<E> picture(String label, Function<E, String> getter);

	/**
	 * Erzeugt ein Eingabefeld (Editor) in Form einer Textarea.
	 * @param label Der Label
	 * @param getter Der Getter fuer den momentanen Wert
	 * @param setter Der Setter fuer den momentanen Wert
	 */
	TextAreaGenerator<E> textArea(String label, Function<E, String> getter, BiConsumer<E, String> setter);

	/**
	 * Erzeugt ein Eingabefeld (Editor) fuer einen bestimmten Datentyp. Das konkret erzeugte Eingabefeld
	 * kann von Datentyp zu Datentyp unterschiedlich sein.
	 * @param label Das Anzeigelabel
	 * @param viewType Der Datentyp des Views
	 * @param dataType Der Datentyp des Models
	 * @param getter Der getter fuer den momentanen Wert
     * @param setter Der setter fuer den momentanen Wert
	 */
	<T> FieldGenerator<E,T> field(String label, Class<?> viewType, Class<T> dataType, Function<E, T> getter, BiConsumer<E, T> setter);

	/**
	 * Erzeugt ein Eingabefeld (Editor) fuer einen bestimmten Datentyp. Das konkret erzeugte Eingabefeld
	 * kann von Datentyp zu Datentyp unterschiedlich sein.
	 * @param label Das Anzeigelabel
	 * @param type Der Datentyp des Views und des Models
	 * @param getter Der getter fuer den momentanen Wert
	 * @param setter Der setter fuer den momentanen Wert
	 */
	<T> FieldGenerator<E,T> field(String label, Class<T> type, Function<E, T> getter, BiConsumer<E, T> setter);

	/**
	 * Erzeugt ein Auswahlfeld mit einer Mehrfachauswahl (Editor) fuer einen bestimmten Datentyp.
	 * @param label Das Anzeigelabel
	 * @param type Der Datentyp des Views und des Models
	 * @param getter Der getter fuer den momentanen Wert
	 * @param setter Der setter fuer den momentanen Wert
	 */
	<T> MultiSelectionGenerator<E,T> multiSelection(String label, Class<T> type, Function<E, Set<T>> getter, BiConsumer<E, Set<T>> setter);


	<T,V extends Collection<T>> CollectionGenerator<E,T,V> collection(String label, Class<T> type, Function<E,V> getter, BiConsumer<E,V> setter, Consumer<FormElementCreator<T>> subFormGenerator);

	<KT,VT,V extends Map<KT,VT>> MapGenerator<E,KT,VT,V> map(String label, Class<KT> keyType, Class<VT> valueType, Function<E,V> getter, BiConsumer<E,V> setter, Consumer<FormElementCreator<MapEntryRef<KT,VT>>> subFormGenerator);

	FormElementCreator<E> ifAdding();

	FormElementCreator<E> ifUpdating();
}
