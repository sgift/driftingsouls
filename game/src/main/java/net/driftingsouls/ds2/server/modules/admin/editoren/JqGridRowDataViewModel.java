package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.ViewModel;

import java.util.List;

@ViewModel
public class JqGridRowDataViewModel
{
	public final String id;
	public final List<String> cell;

	public JqGridRowDataViewModel(String id, List<String> cell)
	{
		this.id = id;
		this.cell = cell;
	}
}
