package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import org.aopalliance.intercept.MethodInvocation;

/**
 * Interceptor fuer Action-Methoden eines Controllers.
 */
public interface ActionMethodInterceptor
{
	/**
	 * Ruft den Interceptor auf. Uebergeben werden die Details zur aufzurufenden Methode des Controllers.
	 * Die Controllermethode ist mittels {@link org.aopalliance.intercept.MethodInvocation#proceed()} auszufuehren.
	 * @param methodInvocation Die Details zur aufzurufenden Controllermethode
	 * @return Das Ergebnis der Methode
	 * @throws Throwable
	 */
    Object invoke(MethodInvocation methodInvocation) throws Throwable;
}
