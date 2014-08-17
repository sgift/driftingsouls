package net.driftingsouls.ds2.server.modules.admin.editoren;

import javax.persistence.metamodel.SingularAttribute;
import java.util.HashMap;
import java.util.Map;

public class ColumnDefinition<E>
{
	private final String id;
	private final String label;
	private final Class<?> viewType;
	private final String formatter;
	private final SingularAttribute<E,?> dbColumn;
	private boolean editable;
	private String edittype;
	private Map<String,String> editoptions = new HashMap<>();

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

	public boolean isEditable()
	{
		return editable;
	}

	public void setEditable(boolean editable)
	{
		this.editable = editable;
	}

	public String getEditType()
	{
		return edittype;
	}

	public void setEditType(String edittype)
	{
		this.edittype = edittype;
	}

	public void addEditOption(String key, String label) {
		editoptions.put(key, label);
	}

	public Map<String, String> getEditOptions()
	{
		return editoptions;
	}
}
