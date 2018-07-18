package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;

/**
 * Standard-ViewModel von Einheiten-Eintraegen in einem Einheiten-Cargo ({@link net.driftingsouls.ds2.server.units.UnitCargoEntry}).
 */
@ViewModel
public class UnitCargoEntryViewModel
{
	public int id;
	public String name;
	public long count;
	public String picture;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static UnitCargoEntryViewModel map(UnitCargoEntry model)
	{
		UnitCargoEntryViewModel viewModel = new UnitCargoEntryViewModel();
		map(model, viewModel);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(UnitCargoEntry model, UnitCargoEntryViewModel viewModel)
	{
		viewModel.id = model.getUnitTypeId();
		viewModel.count = model.getAmount();
		viewModel.name = model.getUnitType().getName();
		viewModel.picture = model.getUnitType().getPicture();
	}
}
