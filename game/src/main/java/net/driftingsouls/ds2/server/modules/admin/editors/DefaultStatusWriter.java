package net.driftingsouls.ds2.server.modules.admin.editors;

class DefaultStatusWriter implements StatusWriter
{
	private final StringBuilder echo;

	public DefaultStatusWriter(StringBuilder echo)
	{
		this.echo = echo;
	}

	@Override
	public StatusWriter append(String text)
	{
		this.echo.append(text);
		return this;
	}
}
