package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel eines Benutzers.
 */
@ViewModel
public class UserViewModel
{
	public final int race;
	public final int id;
	public final String name;
	public final String plainname;

	public UserViewModel(int race, int id, String name, String plainname) {
		this.race = race;
		this.id = id;
		this.name = Common._title(name);
		this.plainname = plainname;
	}

	/**
	 * Mappt eine User-Entity zu einer Instanz dieses ViewModels.
	 * @param user Die zu mappende User-Entity
	 * @return Das ViewModel
	 */
	public static UserViewModel map(User user)
	{
		return new UserViewModel(user.getRace(), user.getId(), user.getName(), user.getPlainname());
	}
}
