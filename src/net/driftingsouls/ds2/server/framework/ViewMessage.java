package net.driftingsouls.ds2.server.framework;

/**
 * Nachrichtenklasse fuer Nachrichten an die GUI.
 */
@ViewModel
public class ViewMessage
{
	public static class ViewMessageDetails
	{
		public String description;
		public String type;
		public boolean redirect;
		public String cls;
	}

	public ViewMessageDetails message;

	/**
	 * Konstruiert eine Erfolgsmeldung fuer eine Ajax-Aktion.
	 *
	 * @param message Der Text
	 * @return Das Message-Objekt
	 */
	public static ViewMessage success(String message)
	{
		ViewMessageDetails details = new ViewMessageDetails();
		details.description = message;
		details.type = "success";

		ViewMessage result = new ViewMessage();
		result.message = details;

		return result;
	}

	/**
	 * Konstruiert eine Misserfolgsmeldung fuer eine Ajax-Aktion.
	 * Die Meldung dient nicht zur Uebermittlung echter Fehler.
	 *
	 * @param message Der Text
	 * @return Das Message-Objekt
	 */
	public static ViewMessage failure(String message)
	{
		ViewMessageDetails details = new ViewMessageDetails();
		details.description = message;
		details.type = "failure";

		ViewMessage result = new ViewMessage();
		result.message = details;

		return result;
	}

	/**
	 * Konstruiert eine Fehlermeldung fuer eine Ajax-Aktion.
	 * Die Meldung dient ausschliesslich zur Uebermittlung echter Fehler,
	 * die dem Nutzer auch als solche angezeigt werden koennen.
	 *
	 * @param message Der Text
	 * @return Das Message-Objekt
	 */
	public static ViewMessage error(String message)
	{
		ViewMessageDetails details = new ViewMessageDetails();
		details.description = message;
		details.type = "error";

		ViewMessage result = new ViewMessage();
		result.message = details;

		return result;
	}
}
