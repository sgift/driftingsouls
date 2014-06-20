package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Eine Weiterleitung an eine bestimmte Action als Ergebnis einer anderen Action.
 */
public final class RedirectViewResult
{
	private final String targetAction;
	private final Map<String,Object> parameters = new HashMap<>();
	private String message;

	/**
	 * Konstruktor.
	 * @param targetAction Der Name der Action zu der weitergeleitet werden soll
	 */
	public RedirectViewResult(@Nonnull String targetAction)
	{
		this.targetAction = targetAction;
	}

	/**
	 * Gibt den Namen der Zielaction zurueck, zu der weitergeleitet werden soll.
	 * @return Der Name der Action
	 */
	public @Nonnull String getTargetAction()
	{
		return targetAction;
	}

	/**
	 * Setzt einen an die Zielaction zu uebermittelnden Parameter.
	 * @param key Der Name des Parameters
	 * @param value Der Wert des Parameters
	 * @return Die aktuelle Instanz
	 */
	public @Nonnull RedirectViewResult setParameter(@Nonnull String key, Object value)
	{
		this.parameters.put(key, value);
		return this;
	}

	/**
	 * Setzt einen Meldungstext der an die Zeilaction uebermittelt werden soll.
	 * @param message Der Text
	 * @return Die aktuelle Instanz
	 */
	public @Nonnull RedirectViewResult withMessage(String message)
	{
		this.message = message;
		return this;
	}

	/**
	 * Gibt alle gesetzten Parameter zurueck.
	 * @return Die Parameter
	 */
	public @Nonnull Map<String, Object> getParameters()
	{
		return parameters;
	}

	/**
	 * Gibt, sofern vorhanden, den Meldungstext zurueck.
	 * @return Der Text
	 */
	public @Nullable String getMessage()
	{
		return message;
	}
}
