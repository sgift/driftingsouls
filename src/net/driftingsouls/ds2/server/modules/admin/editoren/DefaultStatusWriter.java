package net.driftingsouls.ds2.server.modules.admin.editoren;

import java.io.IOException;
import java.io.Writer;

class DefaultStatusWriter implements StatusWriter
{
	private Writer echo;

	public DefaultStatusWriter(Writer echo)
	{
		this.echo = echo;
	}

	@Override
	public StatusWriter append(String text)
	{
		try
		{
			this.echo.append(text);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
		return this;
	}
}
