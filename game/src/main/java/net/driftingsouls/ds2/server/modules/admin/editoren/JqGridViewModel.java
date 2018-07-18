package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.ViewModel;

import java.util.ArrayList;
import java.util.List;

@ViewModel
public class JqGridViewModel
{

	public String url;
	public String datatype = "JSON";
	public String mtype = "GET";
	public List<String> colNames = new ArrayList<>();
	public List<JqGridColumnViewModel> colModel = new ArrayList<>();
	public String pager;
	public int rowNum = 20;
	public List<Integer> rowList = new ArrayList<>();
	public String sortname;
	public String sortorder;
	public boolean viewrecords = true;
	public boolean autoencode = true;
	public boolean gridview = true;
	public String caption;
	public String height = "auto";
	public boolean autowidth = true;
	public boolean shrinkToFit = false;
	public boolean forceFit = true;
}
