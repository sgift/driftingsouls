package net.driftingsouls.ds2.server.comm;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ParameterReader;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParamKonverter;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParamKonverterFuer;
import net.driftingsouls.ds2.server.services.FolderService;
import org.springframework.stereotype.Component;

/**
 * Konvertiert einen URL-Parameter in einen Ordner mittels der Ordner-ID.
 */
@UrlParamKonverterFuer(Ordner.class)
@Component
public class OrdnerUrlParamKonverter implements UrlParamKonverter<Ordner>
{
	private final FolderService folderService;

	public OrdnerUrlParamKonverter(FolderService folderService) {
		this.folderService = folderService;
	}

	@Override
	public Ordner konvertiere(ParameterReader parameterReader, String parameterName)
	{
		int id = (Integer) parameterReader.readParameterAsType(parameterName, Integer.TYPE);
		User user = (User) ContextMap.getContext().getActiveUser();
		if (user != null)
		{
			return folderService.getOrdnerByID(id, user);
		}
		return folderService.getOrdnerByID(id);
	}
}
