package net.driftingsouls.ds2.server.comm;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ParameterReader;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParamConverter;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParamConverterFor;

/**
 * Konvertiert einen URL-Parameter in einen Ordner mittels der Ordner-ID.
 */
@UrlParamConverterFor(Ordner.class)
public class OrdnerUrlParamConverter implements UrlParamConverter<Ordner>
{
	@Override
	public Ordner convert(ParameterReader parameterReader, String parameterName)
	{
		int id = (Integer) parameterReader.readParameterAsType(parameterName, Integer.TYPE);
		User user = (User) ContextMap.getContext().getActiveUser();
		if (user != null)
		{
			return Ordner.getOrdnerByID(id, user, ContextMap.getContext().getEM());
		}
		return Ordner.getOrdnerByID(id, ContextMap.getContext().getEM());
	}
}
