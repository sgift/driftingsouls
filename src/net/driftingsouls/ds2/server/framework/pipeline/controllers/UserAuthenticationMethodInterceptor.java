package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.NotLoggedInException;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

/**
 * Realisiert die notwendige Authentifizierung fuer Controller.
 */
@Component
public class UserAuthenticationMethodInterceptor implements ActionMethodInterceptor
{
	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable
	{
		if(ContextMap.getContext().getActiveUser() != null)
		{
			return methodInvocation.proceed();
		}
		if(!methodInvocation.getThis().getClass().isAnnotationPresent(KeinLoginNotwendig.class) &&
				!methodInvocation.getMethod().isAnnotationPresent(KeinLoginNotwendig.class))
		{
			throw new NotLoggedInException();
		}

		return methodInvocation.proceed();
	}
}
