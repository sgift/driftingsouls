package net.driftingsouls.ds2.server.framework.pipeline.generators;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.apache.commons.lang.math.NumberUtils;
import org.hibernate.Session;

import javax.persistence.Entity;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Klasse zum Parsen und Konvertieren von Requestparametern.
 */
public class ParameterReader
{
	private Request request;
	private String subParameter;
	private Map<String,Object> parameter = new HashMap<>();
	private org.hibernate.Session session;

	public ParameterReader(Request request, Session session)
	{
		this.request = request;
		this.session = session;
		this.subParameter = "";
	}

	/**
	 * Entfernt einen Parameter. Bei einer anschliessenden
	 * Registrierung des Parameters, ist der Wert leer.
	 * @param parameter Der Parametername
	 */
	public void unsetParameter( String parameter ) {
		if( !subParameter.equals("") ) {
			parameter = subParameter+"["+parameter+"]";
		}
		this.request.setParameter(parameter, null);
		this.parameter.remove(parameter);
	}

	protected void parseSubParameter( String subparam ) {
		subParameter = subparam;
	}

	/**
	 * Registriert einen Parameter im System als Zahl. Der Parameter
	 * kann anschliessend ueber entsprechende Funktionen erfragt werden.
	 * @param parameter Der Name des Parameters
	 */
	public void parameterNumber( String parameter ) {
		if( !subParameter.equals("") ) {
			parameter = subParameter+"["+parameter+"]";
		}
		if( (this.request.getParameter(parameter) != null) && !"".equals(this.request.getParameter(parameter)) ) {
			String val = this.request.getParameter(parameter);
			try {
				this.parameter.put(parameter, Common.getNumberFormat().parse(val.trim()));
			}
			catch( NumberFormatException | ParseException e ) {
				this.parameter.put(parameter, 0d);
			}
		}
		else {
			this.parameter.put(parameter, 0d);
		}
	}

	/**
	 * Registriert einen Parameter im System als String. Der Parameter
	 * kann anschliessend ueber entsprechende Funktionen erfragt werden.
	 * @param parameter Der Name des Parameters
	 */
	public void parameterString( String parameter ) {
		if( !subParameter.equals("") ) {
			parameter = subParameter+"["+parameter+"]";
		}
		if( this.request.getParameter(parameter) != null ) {
			this.parameter.put(parameter, this.request.getParameter(parameter));
		}
		else {
			this.parameter.put(parameter,"");
		}
	}

	private Object getParameter( String parameter ) {
		if( subParameter.equals("") ) {
			return this.parameter.get(parameter);
		}
		return this.parameter.get(subParameter+"["+parameter+"]");
	}

	/**
	 * Gibt einen als Zahl registrierten Parameter in Form eines
	 * <code>int</code> zurueck.
	 * @param parameter Der Parametername
	 * @return Der Wert
	 */
	public int getInteger(String parameter) {
		return ((Number)getParameter(parameter)).intValue();
	}

	/**
	 * Gibt einen als Zahl registrierten Parameter in Form eines
	 * <code>double</code> zurueck.
	 * @param parameter Der Parametername
	 * @return Der Wert
	 */
	public double getDouble(String parameter) {
		return ((Number)getParameter(parameter)).doubleValue();
	}

	/**
	 * Gibt einen als String registrierten parameter zurueck.
	 * @param parameter Der Name des Parameters
	 * @return Der Wert
	 */
	public String getString(String parameter) {
		return (String)getParameter(parameter);
	}

	/**
	 * Registriert den Parameter im System unter dem angegebenen (oder einem kompatiblen)
	 * Typ und gibt den Parameter als ein in eine Instanz dieses Typs konvertiertes Objekt zurueck.
	 * Diese Methode liefert immer einen Wert zurueck auch wenn sich der Parameter nicht konvertieren
	 * laesst. In einen solchen Fall wird dann der Defaultwert (z.B. <code>0</code>, <code>null</code>)
	 * zurueckgegeben.
	 * @param paramName Der Name des Parameters
	 * @param type Der Typ des Parameters
	 * @return Ein Objekt vom angegebenen Typ das den Wert des Parameters enthaelt
	 * @throws IllegalArgumentException Falls der angegebene Typ nicht unterstuetzt wird
	 */
	public Object readParameterAsType(String paramName, Class<?> type) throws IllegalArgumentException
	{
		if( Number.class.isAssignableFrom(type) || type == Boolean.TYPE || type == Integer.TYPE || type == Double.TYPE )
		{
			parameterNumber(paramName);
		}
		else
		{
			parameterString(paramName);
		}

		Object paramValue = getParameter(paramName);
		if( type == Boolean.TYPE ) {
			return paramValue != null && ((Number)paramValue).intValue() == 1;
		}
		else if( type == Integer.class )
		{
			return paramValue != null ? ((Number)paramValue).intValue() : null;
		}
		else if( type == Integer.TYPE )
		{
			return paramValue != null ? ((Number)paramValue).intValue() : 0;
		}
		else if( type == Double.class )
		{
			return paramValue != null ? ((Number)paramValue).doubleValue() : null;
		}
		else if( type == Double.TYPE )
		{
			return paramValue != null ? ((Number)paramValue).doubleValue() : 0d;
		}
		else if( Enum.class.isAssignableFrom(type) )
		{
			String strValue = (String)paramValue;
			try
			{
				if(NumberUtils.isDigits(strValue) )
				{
					return type.getEnumConstants()[Integer.parseInt(strValue)];
				}
				return Enum.valueOf(type.asSubclass(Enum.class), (String)paramValue);
			}
			catch( IllegalArgumentException | ArrayIndexOutOfBoundsException e )
			{
				return null;
			}
		}
		else if( type == String.class )
		{
			return paramValue;
		}
		else if( type.isAnnotationPresent(Entity.class) )
		{
			// TODO: Ids erkennen, deren Datentyp nicht Integer ist
			Integer id = (Integer)readParameterAsType(paramName, Integer.class);
			if( id == null )
			{
				return null;
			}
			return this.session.get(type, id);
		}
		throw new IllegalArgumentException(type.getName()+" ist kein gueltiger Parametertyp fuer eine Action-Methode");
	}
}
