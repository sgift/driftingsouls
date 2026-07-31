package net.driftingsouls.ds2.server.framework.pipeline.controllers;

/**
 * Special error for displaying validation failures.
 */
public class ValidationException extends RuntimeException
{
	private final String url;

	public ValidationException(String message)
	{
		this(message,null);
	}

	public ValidationException(String message, String url)
	{
		super(message);
		this.url = url;
	}

	public String getUrl()
	{
		return url;
	}
}
