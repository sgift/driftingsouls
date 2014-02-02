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
	public int race;
	public int id;
	public String name;
	public String plainname;

	/**
	 * Mappt eine User-Entity zu einer Instanz dieses ViewModels.
	 * @param user Die zu mappende User-Entity
	 * @return Das ViewModel
	 */
	public static UserViewModel map(User user)
	{
		UserViewModel viewModel = new UserViewModel();
		viewModel.race = user.getRace();
		viewModel.id = user.getId();
		viewModel.name = Common._title(user.getName());
		viewModel.plainname = user.getPlainname();

		return viewModel;
	}
}
