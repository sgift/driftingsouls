package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.framework.NotLoggedInException;
import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Realisiert die notwendige Authentifizierung fuer Controller.
 */
@Component
public class UserAuthenticationMethodInterceptor implements ActionMethodInterceptor
{
	private AuthenticationManager manager;

	@Autowired
	public UserAuthenticationMethodInterceptor(AuthenticationManager manager)
	{
		this.manager = manager;
	}

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable
	{
		boolean authenticatedUser = manager.authenticateCurrentSession();

		if(authenticatedUser)
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
