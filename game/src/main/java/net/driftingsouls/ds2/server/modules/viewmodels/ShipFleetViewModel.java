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

	public ShipFleetViewModel(int id, String name) {
		this.id = id;
		this.name = name;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static ShipFleetViewModel map(ShipFleet model)
	{
		return new ShipFleetViewModel(model.getId(), model.getName());
	}
}
