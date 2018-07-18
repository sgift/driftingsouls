package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

/**
 * {@link org.aopalliance.intercept.MethodInvocation} fuer eine konkrete Methode eines Controllers.
 */
class ActionMethodInvocation implements MethodInvocation
{
	private Method method;
	private Object[] arguments;
	private Object controller;

	ActionMethodInvocation(Object controller, Method method, Object[] arguments)
	{
		this.method = method;
		this.arguments = arguments;
		this.controller = controller;
	}

	@Override
	public Method getMethod()
	{
		return method;
	}

	@Override
	public Object[] getArguments()
	{
		return arguments;
	}

	@Override
	public Object proceed() throws Throwable
	{
		return method.invoke(controller, arguments);
	}

	@Override
	public Object getThis()
	{
		return controller;
	}

	@Override
	public AccessibleObject getStaticPart()
	{
		return method;
	}
}
