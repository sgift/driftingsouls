package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.AngularGenerator;

/**
 * Standardmodul fuer den Angular-Client. Dient
 * als Einstiegsmodul in den Javascript-Client.
 * @author Christopher Jung
 *
 */
@Module(name="client")
public class ClientController extends AngularGenerator {
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public ClientController(Context context) {
		super(context);
	}

	@Override
	protected boolean validateAndPrepare() {
		return true;
	}
}
