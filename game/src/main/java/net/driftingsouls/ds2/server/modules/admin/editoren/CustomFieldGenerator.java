package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;

/**
 * Standardinterface fuer Feldgeneratoren. Jede Instanz generiert genau
 * ein Feld fuer ein konkretes Form.
 */
public interface CustomFieldGenerator<E>
{
	/**
	 * Generiert den HTML-Code fuer das Eingabefeld.
	 * @param echo Der Writer in den der HTML-Code geschrieben werden soll
	 * @param entity Die Entity-Instanz zu der das Feld generiert werden soll
	 * @throws java.io.IOException Bei I/O-Fehlern
	 */
    void generate(StringBuilder echo, E entity) throws IOException;

	/**
	 * Liesst die Angaben zum Feld aus der Request und speichert sie an der
	 * angebenen Entity.
	 * @param request Die Request
	 * @param entity Die Entity
	 * @throws java.io.IOException Bei IO-Fehlern
	 */
    void applyRequestValues(Request request, E entity) throws IOException;

	/**
	 * Liefert die Spaltendefinition fuer diesen Editor fuer eine Darstellung in einem Grid.
	 * @return Die Spaltendefinition
	 * @param forEditing Falls Spaltendefinitionen zur Bearbeitung in einer Tabelle generiert werden sollen
	 */
    ColumnDefinition<E> getColumnDefinition(boolean forEditing);

	/**
	 * Konvertiert den von diesem Editor bearbeiteten Wert der Entity in einen fuer Benutzer lesbaren Anzeigestring.
	 * @param entity Die Entity-Instanz
	 * @return Der Anzeigestring
	 */
    String serializedValueOf(E entity);
}
