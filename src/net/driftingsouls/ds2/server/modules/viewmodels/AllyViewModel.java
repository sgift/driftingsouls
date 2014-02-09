package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel von Allianzen ({@link net.driftingsouls.ds2.server.entities.ally.Ally}).
 */
@ViewModel
public class AllyViewModel
{
	public int id;
	public String name;
	public String plainname;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static AllyViewModel map(Ally model)
	{
		AllyViewModel viewModel = new AllyViewModel();
		map(model, viewModel);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(Ally model, AllyViewModel viewModel)
	{
		viewModel.id = model.getId();
		viewModel.name = Common._title(model.getName());
		viewModel.plainname = model.getPlainname();
	}
}
