package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.authentication.TickInProgressException;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

/**
 * Realisiert die Ticksperre fuer Controller.
 */
@Component
public class TickMethodInterceptor implements ActionMethodInterceptor
{
	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable
	{
		int tickState = new ConfigService().getValue(WellKnownConfigValue.TICK);
		boolean isTick = tickState == 1;
		if(isTick)
		{

			if(!methodInvocation.getThis().getClass().isAnnotationPresent(KeineTicksperre.class) &&
					!methodInvocation.getMethod().isAnnotationPresent(KeineTicksperre.class))
			{
				throw new TickInProgressException();
			}
		}
		return methodInvocation.proceed();
	}
}
