package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.ViewModel;

import java.util.ArrayList;
import java.util.List;

@ViewModel
public class JqGridRowDataViewModel
{
	public String id;
	public List<String> cell = new ArrayList<>();

	public JqGridRowDataViewModel(String id, List<String> cell)
	{
		this.id = id;
		this.cell = cell;
	}
}
