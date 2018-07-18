/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.framework.templates;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import net.driftingsouls.ds2.server.framework.pipeline.ViewResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Das Template-Engine.
 * @author Christopher Jung
 * 
 */
public class TemplateEngine implements ViewResult
{
	private static final Log log = LogFactory.getLog(TemplateEngine.class);

	private Map<String,Template> file = new HashMap<>();

	private Map<String,Object> varvals = new HashMap<>();

	/*
	 *  whether every set_var-call should be recorded and every changed var should 
	 *  be resetted on clear_record or not 
	 */
	private boolean record = false;
	
	// the list of all recorded values
	private Set<String> recordvars = new HashSet<>();
	
	private Map<String,String> blocks = new HashMap<>();
	private Map<String,String[]> registeredBlocks = new HashMap<>();
	private Map<String,TemplateBlock> registeredBlockObj = new HashMap<>();
	
	private Map<String,String> varNameMap = new HashMap<>();
	private String masterTemplateId;
	private TemplateLoader templateLoader;

	/**
	 * Konstruktor.
	 */
	public TemplateEngine(String masterTemplateId, TemplateLoader templateLoader) {
		this.masterTemplateId = masterTemplateId;
		this.templateLoader = templateLoader;
	}

	/**
	 * Registriert eine Template-Datei im TemplateEngine unter einem bestimmten Namen.
	 * Die Template-Datei muss in kompilierter Form unter <code>net.driftingsouls.ds2.server.framework.templates</code>
	 * vorliegen.
	 * 
	 * @param handle Der Name unter dem das Template zu registrieren ist
	 * @param filename Der Name der Template-Datei
	 * @return true, falls das Template korrekt registriert werden konnte
	 *
	 */
	public boolean setFile( String handle, String filename) {
		if( filename.equals("") ) {
			error("set_file: Aufruf mit den Handle >"+handle+"< jedoch keinem Dateinamen");
			return false;
		}
		
		Template t = templateLoader.load(filename);
		if( t == null ) {
			log.debug("FAILED: Loading "+filename+" as "+handle);
			return false;
		}
		t.prepare(this, handle);
	
		this.file.put(handle, t);

		if( log.isDebugEnabled() ) {
			log.debug("Loaded class "+t.getClass().getName()+" as "+handle);
		}

		return true;
	}

	/**
	 * Registriert einen Template-Block unter einem festgelegten Namen im System.
	 * Der Block wird aus dem Template entfernt und residiert fortan im Speicher.
	 * An dessen Stelle wird ins Template eine Template-Variable gesetzt.
	 * 
	 * @param parent Der Name (Handle) des Elternblocks oder der Template-Datei
	 * @param handle Der Name des Blocks
	 * @param replace Der Name der Ersetzungsvariablen fuer den Block im Template.
	 */
	public void setBlock( String parent, String handle, String replace ) {	
		if( !registeredBlocks.containsKey(handle) ) {
			error("set_block: '"+handle+"' entspricht keinem registrierten Block");	
		}
		String name = replace;
		if( name.equals("") ) {
			name = handle;
		}

		varNameMap.put(handle, name);

		blocks.put(handle, handle); 
		
		if( !registeredBlocks.containsKey(handle) ) {
			error("set_block: Der Block '"+handle+"' ist unbekannt");
			
			return;	
		}
		
		TemplateBlock block = this.file.get(registeredBlocks.get(handle)[0]).getBlock(handle);
							
		if( block == null ) {
			error("set_block: Der Block '"+handle+"' konnte nicht im compilierten Template gefunden werden");
			
			return;	
		}
		registeredBlockObj.put(handle,block);
		
		if( log.isDebugEnabled() ) {
			log.debug("Defined block "+handle+" in "+parent+". Replacement: "+name);
		}

	}
	
	/**
	 * Registriert einen Template-Block unter einem festgelegten Namen im System.
	 * Der Block wird aus dem Template entfernt und residiert fortan im Speicher.
	 * An dessen Stelle wird ins Template eine Template-Variable mit dem Namen 
	 * des Blocks gesetzt.
	 * 
	 * @param parent Der Name (Handle) des Elternblocks oder der Template-Datei
	 * @param handle Der Name des Blocks
	 * @see #setBlock(String, String, String)
	 */
	public void setBlock( String parent, String handle ) {
		setBlock( parent, handle, "" );
	}

	/**
	 * Setzt eine Template-Variable auf einem Wert.
	 * @param varname Der Name der Template-Variablen
	 * @param value Der Wert der Template-Variablen
	 *
	 */
	public void setVar(String varname, Object value) {
		if( log.isTraceEnabled() ) {
			log.trace("set_var [single]: setting >"+varname+"< to >"+value+"<\n");
		}
		varvals.put(varname, value);
		if( record ) {
			recordvars.add(varname);
		}
	}
	
	/**
	 * Setzt mehrere Template-Variablen.
	 * @param list Eine Liste von Werten, in der alle ungeraden Werte Variablennamen und alle Geraden Variablenwerte sind
	 *
	 */
	public void setVar( Object ... list  ) {
		if( list.length % 2 != 0 ) {
			throw new RuntimeException("Illegal var list");
		}
		for( int i=0; i < list.length; i+=2 ) {
			setVar(list[i].toString(), list[i+1]);
		}
	}

	/**
	 * Parsed den Inhalt eines Templates oder eines Block in eine Template-Variable.
	 * Dabei wird der bestehende Inhalt entweder ueberschrieben oder der neue Inhalt
	 * angehangen.
	 *	
	 * @param target Der Name der Zielvariablen
	 * @param handle Der Name (Handle) des Template-Blocks oder der Template-Datei
	 * @param append Soll angehangen (<code>true</code>) oder ueberschrieben (<code>false</code>) werden
	 * @return Der neue Inhalt der Template-Variablen 
	 */
	public String parse(String target, String handle, boolean append) {
		if( (this.file.get(handle) == null) && (blocks.get(handle) == null) ) {
			error("parse: '"+handle+"' ist kein gueltiges Datei- oder Blockhandle");
			return "";	
		}
		
		String str;
		
		if( registeredBlockObj.containsKey(handle) ) {
			TemplateBlock block = registeredBlockObj.get(handle);
			str = block.output(this);
		}
		else {
			Template file = this.file.get(handle);
			str = file.main(this);
		}
		
		boolean rec = record;
		record = false;
		
		if( append ) {
			Object obj = varvals.get(target);
			if( obj != null ) {
				if( !(obj instanceof StringBuffer) ) {
					obj = new StringBuffer(obj.toString());
				}
				((StringBuffer)obj).append(str);
				setVar(target, obj);
			}
			else {
				setVar(target, new StringBuffer(str));
			}
		} 
		else {
			setVar(target, str);
		}
		
		if( log.isDebugEnabled() ) {
			log.debug("parsed "+handle+" -> "+target+" (append: "+(append ? "true" : "false")+")");
		}
		
		record = rec;

		return str;
	}
	
	/**
	 * Parsed den Inhalt eines Templates oder eines Block in eine Template-Variable.
	 * Dabei wird der bestehende Inhalt ueberschrieben.
	 *	
	 * @param target Der Name der Zielvariablen
	 * @param handle Der Name (Handle) des Template-Blocks oder der Template-Datei
	 * @return Der neue Inhalt der Template-Variablen
	 * @see #parse(String, String, boolean) 
	 */
	public String parse(String target, String handle) {
		return parse(target, handle, false);
	}

	/**
	 * Gibt alle im TemplateEngine bekannten Variablen mit samt Wert zurueck.
	 * @return Alle Templatevariablen inklusive Wert
	 */
	public Map<String,Object> get_vars() {
		return varvals;
	}

	/**
	 * Gibt den Inhalt der Variablen zurueck, welche einen Block ersetzt hat.
	 * @param varname Der Name des Blocks
	 * @return Der Inhalt der Variablen
	 */
	public String getBlockReplacementVar( String varname ) {
		if( varNameMap.get(varname) != null ) {
			return getVar(varNameMap.get(varname));
		}
		return "";	
	}
	
	/**
	 * Prueft, ob der Inhalt einer Variablen wahr ist. Hierbei werden
	 * sowohl Boolean als auch Number und String unterstuetzt.
	 * @param varname Der Name der Variable
	 * @return <code>true</code>, falls der Inhalt wahr ist
	 */
	public boolean isVarTrue( String varname ) {
		Object val = getVarObject(varname);
		if( val != null ) {
			if( val instanceof Number ) {
				return ((Number) val).doubleValue() != 0;
			}
			else if( val instanceof Boolean ) {
				return (Boolean) val;
			}
			else if( val instanceof String ) {
				try {
					double d = Double.parseDouble(val.toString());
					if( d != 0 ) {
						return true;
					}
				}
				catch( NumberFormatException e ) {
					if( !val.toString().equals("")  ) {
						return true;
					}
				}
				return false;
			}
			else {
				return !val.toString().equals("");
			}
		}
		return false;
	}
	
	private Object getVarObject(String varname) {
		if( varvals.containsKey(varname) ) {
			return varvals.get(varname);
		}
		
		int index = -1;
		
		while( (index = varname.indexOf('.', index+1)) != -1 ) {
			Object obj = getVarObject(varname.substring(0, index));
			if( obj != null ) {
				return resolveBeanAttributePath(obj, varname.substring(index+1));
			}
		}
		
		return null;
	}

	private Object resolveBeanAttributePath(Object obj, String attrib)
	{
		try {
			while( attrib.indexOf('.') > -1 ) {
				obj = getBeanAttribute(obj, attrib.substring(0, attrib.indexOf('.')));
				if( obj == null ) {
					return null;
				}
				attrib = attrib.substring(attrib.indexOf('.')+1);
			}
			return getBeanAttribute(obj, attrib);
		}
		catch( IntrospectionException | ReflectiveOperationException | IllegalArgumentException e ) {
			log.error("BeanPath: "+attrib, e);
		}
		return null;
	}

	private Object getBeanAttribute(Object obj, String attrib) throws IntrospectionException,
			IllegalAccessException, InvocationTargetException
	{
		BeanInfo info = Introspector.getBeanInfo(obj.getClass());
		PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
		for (PropertyDescriptor descriptor : descriptors)
		{
			if (descriptor.getName().equalsIgnoreCase(attrib))
			{
				return descriptor.getReadMethod().invoke(obj);
			}
		}
		
		return null;
	}

	/**
	 * Liefert eine Variable zurueck.
	 * @param varname Name der Variablen
	 * @return Der Wert der Variablen als String
	 */
	public String getVar( String varname ) {
		if( varvals.get(varname) != null ) {						
      		return varvals.get(varname).toString();
		}
		
		if( varname.contains(".") ) {
			Object value = getVarObject(varname);
			if( value != null ) {
				return value.toString();
			}
		}
		
		return "";
	}
	
	/**
	 * Liefert eine Variable  zurueck.
	 * Sollte es sich bei der Variablen nicht um eine Zahl handelt, so wird
	 * <code>null</code> zurueckgegeben.
	 * @param varname Name der Variablen
	 * @return Der Wert der Variablen als String
	 */
	public Number getNumberVar( String varname ) {
		Object value = this.getVarObject(varname);
		if( value != null ) {						
      		if( value instanceof Number ) {
      			return (Number)value;
      		}
      		try {
      			return Double.parseDouble(value.toString());
      		}
      		catch( NumberFormatException e ) {
      			return (double) 0;
      		}
		}
		return (double) 0;
	}


	
	/**
	 * Registriert einen TemplateBlock.
	 * @param name Der Name
	 * @param filehandle Das Dateihandle
	 * @param parent Der Elternblock
	 */
	public void registerBlockItrnl( String name, String filehandle, String parent ) {
		log.debug("registered block: >"+name+"< >"+filehandle+"< >"+parent+"<");
		registeredBlocks.put(name, new String[] {filehandle, name, parent});
	}

	/**
	 * Startet die Aufzeichnung aller Variablensetzungen.
	 *
	 */
	public void start_record() {
		record = true;
		if( log.isTraceEnabled() ) {
			log.trace("** start record\n");
		}
  	}

	/**
	 * Beendet die Aufzeichnung aller Variablensetzungen.
	 *
	 */
	public void stop_record() {
		record = false;
		if( log.isTraceEnabled() ) {
			log.trace("** stop record\n");
		}
	}

	/**
	 * Entfernt alle Variablen, die innerhalb eines Aufzeichnungsblocks
	 * gesetzt wurden. Der Aufzeichnungspuffer wird anschliessend geleert.
	 *
	 */
	public void clear_record() {
		for( String key : recordvars ) {
			if( log.isTraceEnabled() ) {
				log.trace("clear_record: "+key);
			}
			varvals.remove(key);
		}
		
		recordvars.clear();
	}

	/**
	 * Meldet einen Fehler an den Kontext und gibt ihn im Log aus.
	 * @param msg send an error message to the controller
	 */
	private void error(String msg) {
		Context context = ContextMap.getContext();
		context.addError("Template: "+msg);
		log.error(msg);
 	}

	@Override
	public void writeToResponse(Response response) throws IOException
	{
		if( this.masterTemplateId == null )
		{
			throw new IllegalStateException("Keine MasterTemplateId vergeben");
		}
		this.parse("OUT", this.masterTemplateId);

		Object value = varvals.get("OUT");
		response.getWriter().append(value.toString());
	}

	/**
	 * Gibt den Inhalt einer Variablen in der Kontext-Antwort aus.
	 * @param varname Der Name der auszugebenden Variablen
	 * @throws IOException
	 */
	public void p( String varname ) throws IOException {
		log.debug("out: "+varname);

		Object value = varvals.get(varname);
		Context context = ContextMap.getContext();
		context.getResponse().getWriter().append(value.toString());
	}
}
