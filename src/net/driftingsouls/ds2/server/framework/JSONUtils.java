package net.driftingsouls.ds2.server.framework;

import net.sf.json.JSONObject;

/**
 * Hilfsklasse zur Konstruktion von JSON-Objekten.
 * @author Christopher Jung
 *
 */
public final class JSONUtils
{
	private JSONUtils()
	{
		// EMPTY
	}
	
	/**
	 * Konstruiert eine Erfolgsmeldung fuer eine Ajax-Aktion.
	 * @param message Der Text
	 * @return Das JSON-Objekt
	 */
	public static JSONObject success(String message)
	{
		JSONObject msgObj = new JSONObject()
			.accumulate("description", message)
			.accumulate("type", "success");
		
		return new JSONObject()
			.accumulate("message", msgObj);
	}
	
	/**
	 * Konstruiert eine Misserfolgsmeldung fuer eine Ajax-Aktion.
	 * Die Meldung dient nicht zur Uebermittlung echter Fehler.
	 * @param message Der Text
	 * @return Das JSON-Objekt
	 */
	public static JSONObject failure(String message)
	{
		JSONObject msgObj = new JSONObject()
			.accumulate("description", message)
			.accumulate("type", "failure");
		
		return new JSONObject()
			.accumulate("message", msgObj);
	}
	
	/**
	 * Konstruiert eine Fehlermeldung fuer eine Ajax-Aktion.
	 * Die Meldung dient ausschliesslich zur Uebermittlung echter Fehler,
	 * die dem Nutzer auch als solche angezeigt werden koennen.
	 * @param message Der Text
	 * @return Das JSON-Objekt
	 */
	public static JSONObject error(String message)
	{
		JSONObject msgObj = new JSONObject()
			.accumulate("description", message)
			.accumulate("type", "error");
		
		return new JSONObject()
			.accumulate("message", msgObj);
	}
}
