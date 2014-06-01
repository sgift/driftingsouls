package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.AngularController;

/**
 * Standardmodul fuer den Angular-Client. Dient
 * als Einstiegsmodul in den Javascript-Client.
 * @author Christopher Jung
 *
 */
@Module(name="client")
public class ClientController extends AngularController
{
	/**
	 * Konstruktor.
	 */
	public ClientController() {
		super();
	}
}
