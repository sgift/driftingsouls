package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.db.batch.SingleUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

/**
 * Hebt die allgemeine Ticksperre auf.
 */
@Service
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TicksperreAufhebenTick extends TickController
{
	@Override
	protected void prepare()
	{
		// EMPTY
	}

	@Override
	protected void tick()
	{
		var db = getEM();
		new SingleUnitOfWork("Hebe Accountblock auf", db) {
			@Override
			public void doWork() {
				var db = getEM();
				ConfigValue value = new ConfigService(db).get(WellKnownConfigValue.TICK);
				value.setValue("0");
			}
		}
		.execute();
	}
}
