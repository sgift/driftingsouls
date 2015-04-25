package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.db.batch.SingleUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Hebt die allgemeine Ticksperre auf.
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
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
				ConfigValue value = new ConfigService().get(WellKnownConfigValue.TICK);
				value.setValue("0");
			}
		}
		.execute();
	}
}
