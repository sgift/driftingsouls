package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Basisklasse fuer Objekte zur Ausgabe von Header, Footer und Fehlern.
 *
 */
public abstract class OutputHandler
{
	private Context context = null;
	private Map<String,Object> attributes = new HashMap<>();

	/**
	 * Konstruktor.
	 *
	 */
	public OutputHandler() {
		context = ContextMap.getContext();
	}

	/**
	 * Gibt den Header aus.
	 * @throws java.io.IOException
	 *
	 */
	public abstract void printHeader() throws IOException;
	/**
	 * Gibt den Footer aus.
	 * @throws java.io.IOException
	 *
	 */
	public abstract void printFooter() throws IOException;
	/**
	 * Gibt die Fehlerliste aus.
	 * @throws java.io.IOException
	 *
	 */
	public abstract void printErrorList() throws IOException;

	/**
	 * Setzt ein Attribut.
	 * @param key Der Schluessel
	 * @param value Der Wert
	 */
	public final void setAttribute(String key, Object value) {
		this.attributes.put(key, value);
	}

	/**
	 * Gibt das Attribut mit dem angegebenen Schluessel zurueck.
	 * @param key Der Schluessel
	 * @return Der Wert oder <code>null</code>
	 */
	public final Object getAttribute(String key) {
		return this.attributes.get(key);
	}

	/**
	 * Gibt das Attribut mit dem angegebenen Schluessel als String zurueck.
	 * Wenn das Attribut nicht gesetzt ist (null ist) wird ein leerer String zurueckgegeben.
	 * @param key Der Schluessel
	 * @return Der Wert
	 */
	public final String getAttributeString(String key)
	{
		Object val = this.attributes.get(key);
		if( val == null ) {
			return "";
		}
		return val.toString();
	}

	/**
	 * Gibt den aktuellen Kontext zurueck.
	 * @return Der Kontext
	 */
	protected final Context getContext() {
		return this.context;
	}
}
