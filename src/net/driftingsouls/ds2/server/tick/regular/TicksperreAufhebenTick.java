package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.tick.SingleUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * Hebt die allgemeine Ticksperre auf.
 */
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
		new SingleUnitOfWork("Hebe Accountblock auf") {
			@Override
			public void doWork() {
				ConfigValue value = new ConfigService().get(WellKnownConfigValue.TICKS);
				value.setValue("0");
			}
		}
		.execute();
	}
}
