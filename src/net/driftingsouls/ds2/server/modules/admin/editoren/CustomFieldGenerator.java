package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;
import java.io.Writer;

/**
 * Standardinterface fuer Feldgeneratoren. Jede Instanz generiert genau
 * ein Feld fuer ein konkretes Form.
 */
public interface CustomFieldGenerator<V>
{
	/**
	 * Generiert den HTML-Code fuer das Eingabefeld.
	 * @param echo Der Writer in den der HTML-Code geschrieben werden soll
	 * @param entity Die Entity-Instanz zu der das Feld generiert werden soll
	 * @throws java.io.IOException Bei I/O-Fehlern
	 */
	public void generate(Writer echo, V entity) throws IOException;

	/**
	 * Liesst die Angaben zum Feld aus der Request und speichert sie an der
	 * angebenen Entity.
	 * @param request Die Request
	 * @param entity Die Entity
	 * @throws java.io.IOException Bei IO-Fehlern
	 */
	public void applyRequestValues(Request request, V entity) throws IOException;
}
