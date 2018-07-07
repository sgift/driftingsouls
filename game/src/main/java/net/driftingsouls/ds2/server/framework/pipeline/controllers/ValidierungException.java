package net.driftingsouls.ds2.server.framework.pipeline.controllers;

/**
 * Spezieller Fehler zum Anzeigen von Validierungsfehlern.
 */
public class ValidierungException extends RuntimeException
{
	private String url;

	public ValidierungException(String message)
	{
		this(message,null);
	}

	public ValidierungException(String message, String url)
	{
		super(message);
		this.url = url;
	}

	public String getUrl()
	{
		return url;
	}
}
