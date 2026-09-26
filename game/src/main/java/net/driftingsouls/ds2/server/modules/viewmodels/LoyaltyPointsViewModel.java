package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.entities.Loyalitaetspunkte;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel von Loyalitaetspunkten ({@link net.driftingsouls.ds2.server.entities.Loyalitaetspunkte}).
 */
@ViewModel
public class LoyaltyPointsViewModel
{
	public int id;
	public String grund;
	public String anmerkungen;
	public int anzahlPunkte;
	public long zeitpunkt;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static LoyaltyPointsViewModel map(Loyalitaetspunkte model)
	{
		LoyaltyPointsViewModel viewModel = new LoyaltyPointsViewModel();
		map(model, viewModel);
		return viewModel;
	}

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @param viewModel Die Zielinstanz des ViewModels
	 */
	public static void map(Loyalitaetspunkte model, LoyaltyPointsViewModel viewModel)
	{
		viewModel.id = model.getId();
		viewModel.grund = model.getGrund();
		viewModel.anmerkungen = model.getAnmerkungen();
		viewModel.anzahlPunkte = model.getAnzahlPunkte();
		viewModel.zeitpunkt = model.getZeitpunkt().getTime();
	}
}
