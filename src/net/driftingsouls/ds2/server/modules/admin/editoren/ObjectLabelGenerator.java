package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Generator fuer Labels zu Objekten.
 */
public class ObjectLabelGenerator
{
	/**
	 * Generiert ein Label fuer eine Kombination von ID und Objektinstanz.
	 * @param identifier Die ID des Objekts
	 * @param entity Die Objektinstanz
	 * @return Das generierte Label
	 */
	public String generateFor(@Nullable Serializable identifier, @Nonnull Object entity)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		if (identifier == null && entity.getClass().isAnnotationPresent(Entity.class))
		{
			identifier = db.getIdentifier(entity);
		}

		if( entity instanceof User)
		{
			return (((User) entity).getPlainname())+" ("+identifier+")";
		}

		Class<?> type = entity.getClass();

		Method labelMethod = null;
		for (String m : Arrays.asList("getName", "getNickname", "toString"))
		{
			try
			{
				labelMethod = type.getMethod(m);
				break;
			}
			catch (NoSuchMethodException e)
			{
				// Ignore
			}
		}

		if( labelMethod == null )
		{
			throw new AssertionError("No toString");
		}

		try
		{
			return labelMethod.invoke(entity).toString() + (identifier != null ? " (" + identifier + ")" : "");
		}
		catch (IllegalAccessException | InvocationTargetException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
