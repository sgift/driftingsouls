package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.cargo.ItemData;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel von Items ({@link net.driftingsouls.ds2.server.config.items.Item}).
 */
@ViewModel
public class ItemViewModel
{
	public String picture;
	public int id;
	public String name;
	public ItemQualityViewModel quality;
	public String effectName;
	public long cargo;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static ItemViewModel map(ItemData model)
	{
		ItemViewModel viewModel = new ItemViewModel();
		map(model, viewModel);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(ItemData model, ItemViewModel viewModel)
	{
		viewModel.name = Common._plaintitle(model.getName());
		viewModel.picture = model.getPicture();
		viewModel.id = model.getId();
		viewModel.quality = ItemQualityViewModel.map(model.getQuality());
		viewModel.effectName = model.getType().getName();
		viewModel.cargo = model.getCargo();
	}
}
