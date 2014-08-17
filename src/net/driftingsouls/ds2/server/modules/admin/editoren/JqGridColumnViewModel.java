package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.ViewModel;

import java.util.HashMap;
import java.util.Map;

@ViewModel
public class JqGridColumnViewModel
{
	public String name;
	public String index;
	public Integer width;
	public String align;
	public boolean sortable;
	public String formatter;
	public boolean search;
	public boolean editable;
	public String edittype;
	public Map<String,Object> editoptions = new HashMap<>();
	public boolean key;
	public boolean hidden;

	public JqGridColumnViewModel(String name, String formatter)
	{
		this.name = name;
		this.formatter = formatter;
	}
}
