package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.config.items.Quality;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel von Qualitaetsleveln von Items ({@link net.driftingsouls.ds2.server.config.items.Quality}).
 */
@ViewModel
public class ItemQualityViewModel
{
	public String name;
	public String color;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static ItemQualityViewModel map(Quality model)
	{
		ItemQualityViewModel viewModel = new ItemQualityViewModel();
		map(model, viewModel);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(Quality model, ItemQualityViewModel viewModel)
	{
		viewModel.name = model.name();
		viewModel.color = model.color();
	}
}
