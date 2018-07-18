package net.driftingsouls.ds2.server.framework.pipeline.controllers.jsstubs;

import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class ControllerAnalyser
{
	private Class<?> controllerCls;

	public ControllerAnalyser(Class<?> controllerCls)
	{
		this.controllerCls = controllerCls;
	}

	public List<Method> findActionMethods()
	{
		List<Method> actionMethods = new ArrayList<>();
		for( Method method : controllerCls.getMethods() ) {
			if( !method.isAnnotationPresent(Action.class) )
			{
				continue;
			}
			Action action = method.getAnnotation(Action.class);
			if( action.value() != ActionType.AJAX )
			{
				continue;
			}
			actionMethods.add(method);
		}
		return actionMethods;
	}

	public String getStubName()
	{
		return controllerCls.getSimpleName()+"Stub";
	}

	public String getFullName()
	{
		return controllerCls.getCanonicalName();
	}

	public String getModuleName()
	{
		return controllerCls.getAnnotation(Module.class).name();
	}
}
