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
	public final int id;
	public final String name;
	public final String plainname;

	public AllyViewModel(int id, String name, String plainname) {
		this.id = id;
		this.name = Common._title(name);
		this.plainname = plainname;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static AllyViewModel map(Ally model)
	{
		return new AllyViewModel(model.getId(), model.getName(), model.getPlainname());
	}
}
