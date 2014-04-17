package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.ViewModel;

import java.util.ArrayList;
import java.util.List;

@ViewModel
public class JqGridTableDataViewModel
{
	public int page;
	public int total;
	public int records;
	public List<JqGridRowDataViewModel> rows = new ArrayList<>();
}
