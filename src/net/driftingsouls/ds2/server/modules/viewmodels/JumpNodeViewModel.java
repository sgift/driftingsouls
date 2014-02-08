package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel von Sprungpunkten ({@link net.driftingsouls.ds2.server.entities.JumpNode}).
 */
@ViewModel
public class JumpNodeViewModel
{
	public int system;
	public int x;
	public int y;
	public String name;
	public int systemout;
	public boolean blocked;
	public boolean hidden;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static JumpNodeViewModel map(JumpNode model)
	{
		JumpNodeViewModel viewModel = new JumpNodeViewModel();
		map(model, viewModel);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(JumpNode model, JumpNodeViewModel viewModel)
	{
		viewModel.system = model.getSystem();
		viewModel.x = model.getX();
		viewModel.y = model.getY();
		viewModel.name = model.getName();
		viewModel.systemout = model.getSystemOut();
		viewModel.blocked = model.isGcpColonistBlock();
		viewModel.hidden = model.isHidden();
	}
}
