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
 * Setzt die allgemeine Ticksperre.
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class TicksperreSetzenTick extends TickController
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
				value.setValue("1");
			}
		}
		.execute();
	}
}
