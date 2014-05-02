package net.driftingsouls.ds2.server.modules.admin.editoren;

import java.util.LinkedHashMap;
import java.util.Map;

public class SelectViewModel extends InputViewModel
{
	public boolean disabled;
	public String nullOption;
	public Map<String,String> options = new LinkedHashMap<>();
	public String selected;

	public SelectViewModel(String name)
	{
		super("select", name);
	}
}
