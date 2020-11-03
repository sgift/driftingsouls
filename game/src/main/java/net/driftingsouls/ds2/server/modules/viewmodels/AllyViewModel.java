package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;

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
	public static AllyViewModel map(BBCodeParser bbCodeParser, Ally model)
	{
		AllyViewModel viewModel = new AllyViewModel();
		map(bbCodeParser, model, viewModel);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(BBCodeParser bbCodeParser, Ally model, AllyViewModel viewModel)
	{
		viewModel.id = model.getId();
		viewModel.name = Common._title(bbCodeParser, model.getName());
		viewModel.plainname = model.getPlainname();
	}
}
