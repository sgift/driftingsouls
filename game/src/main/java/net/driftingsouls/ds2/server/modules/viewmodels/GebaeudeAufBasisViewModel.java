package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel von Gebaeuden ({@link net.driftingsouls.ds2.server.bases.Building}).
 */
@ViewModel
public class GebaeudeAufBasisViewModel
{
	public int id;
	public String name;
	public String picture;
	public boolean active;
	public boolean deakable;
	public boolean kommandozentrale;
	public String type;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param base Die Basis auf der das Gebaeude steht
	 * @param field Das Feld auf der Basis auf der das Gebaeude steht
	 * @return Das ViewModel
	 */
	public static GebaeudeAufBasisViewModel map(Building model, Base base, int field)
	{
		GebaeudeAufBasisViewModel viewModel = new GebaeudeAufBasisViewModel();
		map(model, viewModel, base, field);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param base Die Basis auf der das Gebaeude steht
	 * @param field Das Feld auf der Basis auf der das Gebaeude steht
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(Building model, GebaeudeAufBasisViewModel viewModel, Base base, int field)
	{
		viewModel.id = model.getId();
		viewModel.name = Common._plaintitle(model.getName());
		viewModel.picture = model.getPictureForRace(base.getOwner().getRace());
		viewModel.active = model.isActive(base, base.getActive()[field], field);
		viewModel.deakable = model.isDeakAble();
		viewModel.kommandozentrale = model.getId() == Building.KOMMANDOZENTRALE;
		viewModel.type = model.getClass().getSimpleName();
	}
}
