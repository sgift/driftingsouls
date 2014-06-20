package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

/**
 * Realisiert die Sperre von Acccounts im Vacation-Modus.
 */
@Component
public class AccountVacationMethodInterceptor implements ActionMethodInterceptor
{
	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable
	{
		if(!methodInvocation.getThis().getClass().isAnnotationPresent(KeinLoginNotwendig.class) &&
				!methodInvocation.getMethod().isAnnotationPresent(KeinLoginNotwendig.class))
		{
			User user = (User) ContextMap.getContext().getActiveUser();
			if (user != null && !user.isAdmin() && user.isInVacation())
			{
				throw new AccountInVacationModeException(user.getVacationCount());
			}
		}
		return methodInvocation.proceed();
	}
}
