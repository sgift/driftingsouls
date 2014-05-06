package net.driftingsouls.ds2.server.framework.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Hilfsklasse zum Konvertieren von Strings in eine Reihe von bekannten Datentypen.
 */
public final class StringToTypeConverter
{
	private StringToTypeConverter()
	{
		// EMPTY
	}

	/**
	 * Konvertiert einen String in den angegebenen Zieldatentyp, sofern eine Konvertierung moeglich ist.
	 * @param type Der Zieldatentyp
	 * @param value Der Stringwert
	 * @param <T> Der Zieldatentyp
	 * @return Der konvertierte Wert
	 * @throws java.lang.IllegalArgumentException Falls der konkrete Wert nicht in den Zieldatentyp konvertiert werden kann
	 * @throws java.lang.UnsupportedOperationException Falls fuer den Datentyp keine Konvertierung unterstuetzt wird
	 */
	@SuppressWarnings("unchecked")
	public static <T> T convert(Class<T> type, String value) throws UnsupportedOperationException, IllegalArgumentException
	{
		if( value == null )
		{
			// Null kann immer konvertiert werden
			return null;
		}
		if( type == String.class )
		{
			return (T)value;
		}
		if( type == BigDecimal.class )
		{
			return (T)new BigDecimal(value);
		}
		else if( type == BigInteger.class )
		{
			return (T)new BigInteger(value);
		}

		try
		{
			Method valueOfMethod = type.getMethod("valueOf", String.class);
			if( (valueOfMethod.getModifiers() & Modifier.STATIC) != 0 )
			{
				valueOfMethod.setAccessible(true);
				return (T)valueOfMethod.invoke(null, value);
			}
		}
		catch (IllegalAccessException e)
		{
			throw new UnsupportedOperationException("Der Datentyp "+type+" besitzt zwar eine valueOf-Methode, diese kann aber nicht aufgerufen werden", e);
		}
		catch (InvocationTargetException e)
		{
			throw new IllegalArgumentException("Der Wert '"+value+"' kann nicht in den Datentyp "+type+" konvertiert werden", e);
		}
		catch (NoSuchMethodException e)
		{
			// fall through
		}

		throw new UnsupportedOperationException("Der Zieltyp "+type.getName()+" wird nicht unterstuetzt");
	}
}
