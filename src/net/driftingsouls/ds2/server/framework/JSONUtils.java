package net.driftingsouls.ds2.server.framework;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
	public static JsonElement success(String message)
	{
		JsonObject msgObj = new JsonObject();
		msgObj.addProperty("description", message);
		msgObj.addProperty("type", "success");

		JsonObject result = new JsonObject();
		result.add("message", msgObj);
		return result;
	}
	
	/**
	 * Konstruiert eine Misserfolgsmeldung fuer eine Ajax-Aktion.
	 * Die Meldung dient nicht zur Uebermittlung echter Fehler.
	 * @param message Der Text
	 * @return Das JSON-Objekt
	 */
	public static JsonElement failure(String message)
	{
		JsonObject msgObj = new JsonObject();
		msgObj.addProperty("description", message);
		msgObj.addProperty("type", "failure");

		JsonObject result = new JsonObject();
		result.add("message", msgObj);
		return result;
	}
	
	/**
	 * Konstruiert eine Fehlermeldung fuer eine Ajax-Aktion.
	 * Die Meldung dient ausschliesslich zur Uebermittlung echter Fehler,
	 * die dem Nutzer auch als solche angezeigt werden koennen.
	 * @param message Der Text
	 * @return Das JSON-Objekt
	 */
	public static JsonElement error(String message)
	{
		JsonObject msgObj = new JsonObject();
		msgObj.addProperty("description", message);
		msgObj.addProperty("type", "error");

		JsonObject result = new JsonObject();
		result.add("message", msgObj);
		return result;
	}
}
