package net.driftingsouls.ds2.server.modules.viewmodels;

import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionAktionsMeldung;
import net.driftingsouls.ds2.server.framework.ViewModel;

/**
 * Standard-ViewModel fuer Fraktionsaktionsmeldungen ({@link net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionAktionsMeldung}).
 */
@ViewModel
public class FraktionAktionsMeldungViewModel
{
	public Long id;
	public UserViewModel von;
	public long am;
	public UserViewModel fraktion;
	public String meldungstext;
	public Long bearbeitetAm;

	/**
	 * Mappt eine Entity zu einer Instanz dieses ViewModels.
	 * @param model Die zu mappende Entity
	 * @return Das ViewModel
	 */
	public static FraktionAktionsMeldungViewModel map(FraktionAktionsMeldung model)
	{
		FraktionAktionsMeldungViewModel viewModel = new FraktionAktionsMeldungViewModel();
		viewModel.id = model.getId();
		viewModel.von = UserViewModel.map(model.getGemeldetVon());
		viewModel.am = model.getGemeldetAm().getTime();
		viewModel.fraktion = UserViewModel.map(model.getFraktion());
		viewModel.meldungstext = model.getMeldungstext();
		viewModel.bearbeitetAm = model.getBearbeitetAm() != null ? model.getBearbeitetAm().getTime() : null;

		return viewModel;
	}
}
