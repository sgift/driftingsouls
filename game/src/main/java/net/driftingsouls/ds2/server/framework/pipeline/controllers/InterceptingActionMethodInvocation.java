package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

/**
 * MethodInvocation mit einem {@link net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionMethodInterceptor} als
 * Interceptor um die eigentliche {@link org.aopalliance.intercept.MethodInvocation}.
 */
class InterceptingActionMethodInvocation implements MethodInvocation
{
	private ActionMethodInterceptor interceptor;
	private MethodInvocation inner;


	InterceptingActionMethodInvocation(ActionMethodInterceptor interceptor, MethodInvocation inner)
	{
		this.interceptor = interceptor;
		this.inner = inner;
	}

	@Override
	public Object proceed() throws Throwable
	{
		return this.interceptor.invoke(inner);
	}

	@Override
	public Object getThis()
	{
		return inner.getThis();
	}

	@Override
	public AccessibleObject getStaticPart()
	{
		return inner.getStaticPart();
	}

	@Override
	public Method getMethod()
	{
		return inner.getMethod();
	}

	@Override
	public Object[] getArguments()
	{
		return inner.getArguments();
	}
}
