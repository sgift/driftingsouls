package net.driftingsouls.ds2.server.modules.admin.editoren;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

final class EntityUtils
{
	private EntityUtils() {
		// EMPTY
	}

	public static <T> T createEntity(Class<T> clazz)
	{
		try
		{
			Constructor<? extends T> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		}
		catch (NoSuchMethodException e)
		{
			throw new AssertionError("Kein default-Konstruktor fuer Entity '"+clazz.getName()+"' vorhanden");
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException e)
		{
			throw new IllegalStateException("Konnte Entity '"+clazz.getName()+"' nicht instantiieren");
		}
	}
}
