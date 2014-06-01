package net.driftingsouls.ds2.server.modules.admin.editoren;

import javax.persistence.metamodel.SingularAttribute;

public class ColumnDefinition<E>
{
	private final String id;
	private final String label;
	private final Class<?> viewType;
	private final String formatter;
	private final SingularAttribute<E,?> dbColumn;

	public ColumnDefinition(String id, String label, Class<?> viewType)
	{
		this(id, label, viewType, null, null);
	}

	public ColumnDefinition(String id, String label, Class<?> viewType, SingularAttribute<E,?> dbColumn)
	{
		this(id, label, viewType, null, dbColumn);
	}

	public ColumnDefinition(String id, String label, Class<?> viewType, String formatter)
	{
		this(id,label,viewType,formatter,null);
	}

	public ColumnDefinition(String id, String label, Class<?> viewType, String formatter, SingularAttribute<E,?> dbColumn)
	{
		this.id = id;
		this.label = label;
		this.viewType = viewType;
		this.formatter = formatter;
		this.dbColumn = dbColumn;
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

	public SingularAttribute<E,?> getDbColumn()
	{
		return dbColumn;
	}
}
