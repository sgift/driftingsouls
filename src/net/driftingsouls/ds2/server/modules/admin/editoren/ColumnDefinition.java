package net.driftingsouls.ds2.server.modules.admin.editoren;

public class ColumnDefinition
{
	private final String id;
	private final String label;
	private final Class<?> viewType;
	private final String formatter;

	public ColumnDefinition(String id, String label, Class<?> viewType)
	{
		this(id, label, viewType, null);
	}

	public ColumnDefinition(String id, String label, Class<?> viewType, String formatter)
	{
		this.id = id;
		this.label = label;
		this.viewType = viewType;
		this.formatter = formatter;
	}

	public String getId()
	{
		return id;
	}

	public String getLabel()
	{
		return label;
	}

	public Class<?> getViewType()
	{
		return viewType;
	}

	public String getFormatter()
	{
		return formatter;
	}
}
