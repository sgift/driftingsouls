package net.driftingsouls.ds2.server.tick;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.springframework.context.ApplicationContext;

/**
 * A tick specific context. Wie {@link BasicContext} loest sie den EntityManager bei jedem Zugriff
 * dynamisch ueber {@code HibernateUtil.getCurrentEntityManager()} auf (siehe {@link BasicContext#getEM()});
 * das Oeffnen und Schliessen der Datenbankverbindung obliegt weiterhin dem Aufrufer.
 */
public class TickContext extends BasicContext
{
	/**
	 * Initialisiert den Tick-Context.
	 *
	 * @param request Das Requestobjekt.
	 * @param response Das Responseobjekt.
	 * @param applicationContext Der zu verwendende Spring {@link ApplicationContext}.
	 */
	public TickContext(Request request, Response response, ApplicationContext applicationContext)
	{
		super(request, response, new EmptyPermissionResolver(), applicationContext);
	}
}
