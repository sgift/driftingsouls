package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel einer Rangs ({@link net.driftingsouls.ds2.server.config.Rang}).
 */
@ViewModel
public class RankViewModel
{
	public int id;
	public String name;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static RankViewModel map(Rang model)
	{
		RankViewModel viewModel = new RankViewModel();
		viewModel.name = model.getName();
		viewModel.id = model.getId();

		if( model.getId() == 0 ) {
			viewModel.name = "-";
		}

		return viewModel;
	}
}
