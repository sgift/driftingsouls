package net.driftingsouls.ds2.server.framework.pipeline.generators;

import java.util.HashMap;
import java.util.Map;

public class RedirectViewResult
{
	private final String targetAction;
	private final Map<String,Object> parameters = new HashMap<>();
	private String message;

	public RedirectViewResult(String targetAction)
	{
		this.targetAction = targetAction;
	}

	public String getTargetAction()
	{
		return targetAction;
	}

	public RedirectViewResult setParameter(String key, Object value)
	{
		this.parameters.put(key, value);
		return this;
	}

	public RedirectViewResult setMessage(String message)
	{
		this.message = message;
		return this;
	}

	public Map<String, Object> getParameters()
	{
		return parameters;
	}

	public String getMessage()
	{
		return message;
	}
}
