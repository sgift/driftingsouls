package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.ViewModel;

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

	public JqGridColumnViewModel(String name, String formatter)
	{
		this.name = name;
		this.formatter = formatter;
	}
}
