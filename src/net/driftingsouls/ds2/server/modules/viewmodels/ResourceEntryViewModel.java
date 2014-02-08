package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel von Ressourceneintraegen ({@link net.driftingsouls.ds2.server.cargo.ResourceEntry}).
 */
@ViewModel
public class ResourceEntryViewModel
{
	public long count1;
	public String cargo1;
	public String image;
	public String name;
	public String plainname;
	public String id;

	public Long count2;
	public Long diff;
	public String cargo2;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static ResourceEntryViewModel map(ResourceEntry model)
	{
		ResourceEntryViewModel viewModel = new ResourceEntryViewModel();
		map(model, viewModel);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(ResourceEntry model, ResourceEntryViewModel viewModel)
	{
		viewModel.count1 = model.getCount1();
		viewModel.cargo1 = model.getCargo1();
		viewModel.image = model.getImage();
		viewModel.name = model.getName();
		viewModel.plainname = model.getPlainName();
		viewModel.id = model.getId().toString();

		if( model.getCargo2() != null )
		{
			viewModel.count2 = model.getCount2();
			viewModel.diff = model.getDiff();
			viewModel.cargo2 = model.getCargo2();
		}
	}
}
