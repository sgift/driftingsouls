package net.driftingsouls.ds2.server.modules.admin.editoren;

class DefaultStatusWriter implements StatusWriter
{
	private StringBuilder echo;

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
