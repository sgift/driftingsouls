package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.ships.ShipFleet;

/**
 * Standard-ViewModel von Flotten ({@link net.driftingsouls.ds2.server.ships.ShipFleet}).
 */
@ViewModel
public class ShipFleetViewModel
{
	public int id;
	public String name;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static ShipFleetViewModel map(ShipFleet model)
	{
		ShipFleetViewModel viewModel = new ShipFleetViewModel();
		map(model, viewModel);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(ShipFleet model, ShipFleetViewModel viewModel)
	{
		viewModel.id = model.getId();
		viewModel.name = model.getName();
	}
}
