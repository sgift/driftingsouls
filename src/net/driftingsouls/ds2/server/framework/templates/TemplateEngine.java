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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;

// TODO: Compiler
// TODO: Runtime-functions

/**
 * Das Template-Engine
 * @author Christopher Jung
 * 
 */
public class TemplateEngine implements Loggable {
	private static final String PACKAGE = "net.driftingsouls.ds2.server.templates";

	private Map<String,Template> m_file = new HashMap<String,Template>();
	
	private Context m_controller = null;

	/* 
	 * relative filenames are relative to this pathname 
	 * overlay is checked first - then base 
	 */
	private String overlay = null;

	private Map<String,Object> varvals = new HashMap<String,Object>();

	/*
	 *  whether every set_var-call should be recorded and every changed var should 
	 *  be resetted on clear_record or not 
	 */
	private boolean record = false;
	
	// the list of all recorded values
	private Set<String> recordvars = new HashSet<String>();
	
	private Map<String,String> m_blocks = new HashMap<String,String>();
	private Map<String,String[]> m_registeredBlocks = new HashMap<String,String[]>();
	private Map<String,TemplateBlock> registeredBlockObj = new HashMap<String,TemplateBlock>();
	
	private Map<String,String> m_varNameMap = new HashMap<String,String>();
	
	public TemplateEngine(Context controller) {
		m_controller = controller;
	}
	
	public Context getController() {
		return m_controller;
	}

	/**
	 * Setzt das Overlay-Verzeichnis fest. Das Overlay-Verzeichnis
	 * muss ein direktes Unterverzeichnis des angegeben Template-Verzeichnisses sein.
	 * Ferner duerfen keine / oder . im Namen vorkommen.
	 * Templates werden fortan zuerst im Overlay-Verzeichnis
	 * gesucht.
	 *  
	 * @param overlay Das Overlay-Verzeichnis
	 * @return true, falls das Verzeichnis gefunden und als Overlay im System eingefuegt wurde
	 */
	public boolean setOverlay( String overlay) {
		if( overlay.equals("") || (overlay.indexOf('.') != -1) ) {
			// just return false
      		return false;
    	}
		
		Package pack = Package.getPackage(PACKAGE+"."+overlay);
		if( pack == null ) {
			LOG.warn("Overlay-Verzeichnis "+overlay+" exstiert nicht als Package");
			return false;
		}

		this.overlay = overlay;
		return true;
	}
	
	public void compile( String fname ) {
		throw new RuntimeException("Not implemented yet");
	}
	
	private static Map<String,Template> templateMap = Collections.synchronizedMap(new HashMap<String,Template>());
	
	/**
	 * Registriert eine Template-Datei im TemplateEngine unter einem bestimmten Namen.
	 * Die Template-Datei muss in kompilierter Form unter <code>net.driftingsouls.ds2.server.templates</code>
	 * vorliegen
	 * 
	 * @param handle Der Name unter dem das Template zu registrieren ist
	 * @param filename Der Name der Template-Datei
	 * @return true, falls das Template korrekt registriert werden konnte
	 *
	 */
	public boolean set_file( String handle, String filename) {
		if( filename.equals("") ) {
			error("set_file: Aufruf mit den Handle >"+handle+"< jedoch keinem Dateinamen");
			return false;
		}
		
		if( !templateMap.containsKey(overlay+'.'+filename) ) {
			String fname = new File(filename).getName();
			fname = fname.substring(0,fname.length()-".html".length()).replace(".", "");
						
			boolean gotTemplate = false;
			if( overlay != null ) {
				try {
					Class<?> tmpl = Class.forName(PACKAGE+"."+overlay+"."+fname);
					Template t = (Template)tmpl.newInstance();
					templateMap.put(overlay+'.'+filename, t);
					gotTemplate = true;
				}
				catch( Exception e ) {}
			}
			if( !gotTemplate ) {
				try {
					Class<?> tmpl = Class.forName(PACKAGE+"."+fname);
					Template t = (Template)tmpl.newInstance();
					templateMap.put(overlay+'.'+filename, t);
				}
				catch( Exception e ) {
					LOG.fatal("FAILED: Loading class "+PACKAGE+"."+fname+" as "+handle, e);
					return false;
				}
			}
		}

		Template t = templateMap.get(overlay+'.'+filename);
		t.prepare(this, handle);
	
		m_file.put(handle, t);
		
		if( LOG.isDebugEnabled() ) {
			LOG.debug("Loaded class "+PACKAGE+"."+filename+" as "+handle);
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
	public void set_block( String parent, String handle, String replace ) {	
		if( !m_registeredBlocks.containsKey(handle) ) {
			error("set_block: '"+handle+"' entspricht keinem registrierten Block");	
		}
		String name = replace;
		if( name.equals("") ) {
			name = handle;
		}

		m_varNameMap.put(handle, name);

		m_blocks.put(handle, handle); 
		
		TemplateBlock block = m_file.get(m_registeredBlocks.get(handle)[0]).getBlock(handle);
							
		if( block == null ) {
			error("set_block: Der Block '"+handle+"' konnte nicht im compilierten Template gefunden werden");
			
			return;	
		}
		registeredBlockObj.put(handle,block);
		
		if( LOG.isDebugEnabled() ) {
			LOG.debug("Defined block "+handle+" in "+parent+". Replacement: "+name);
		}
		
		return;
	}
	
	/**
	 * Registriert einen Template-Block unter einem festgelegten Namen im System.
	 * Der Block wird aus dem Template entfernt und residiert fortan im Speicher.
	 * An dessen Stelle wird ins Template eine Template-Variable mit dem Namen 
	 * des Blocks gesetzt.
	 * 
	 * @param parent Der Name (Handle) des Elternblocks oder der Template-Datei
	 * @param handle Der Name des Blocks
	 * @see #set_block(String, String, String)
	 */
	public void set_block( String parent, String handle ) {
		set_block( parent, handle, "" );
	}

	/**
	 * Setzt eine Template-Variable auf einem Wert
	 * @param varname Der Name der Template-Variablen
	 * @param value Der Wert der Template-Variablen
	 *
	 */
	public void set_var(String varname, Object value) {
		if( LOG.isTraceEnabled() ) {
			LOG.trace("set_var [single]: setting >"+varname+"< to >"+value+"<\n");
		}
		varvals.put(varname, value);
		if( record ) {
			recordvars.add(varname);
		}
	}
	
	public void set_var( Object ... list  ) {
		if( list.length % 2 != 0 ) {
			throw new RuntimeException("Illegal var list");
		}
		for( int i=0; i < list.length; i+=2 ) {
			set_var(list[i].toString(), list[i+1]);
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
		if( (m_file.get(handle) == null) && (m_blocks.get(handle) == null) ) {
			error("parse: '"+handle+"' ist kein gueltiges Datei- oder Blockhandle");
			return "";	
		}
		
		String str = "";
		
		if( registeredBlockObj.containsKey(handle) ) {
			TemplateBlock block = registeredBlockObj.get(handle);
			str = block.output(this);
		}
		else {
			Template file = m_file.get(handle);
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
				set_var(target, obj);
			}
			else {
				set_var(target, new StringBuffer(str));
			}
		} 
		else {
			set_var(target, str);
		}
		
		if( LOG.isDebugEnabled() ) {
			LOG.debug("parsed "+handle+" -> "+target+" (append: "+(append ? "true" : "false")+")");
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

	/* 
	 *	get_vars()
	 */
	public Map<String,Object> get_vars() {
		return varvals;
	}

	public String getBlockReplacementVar( String varname ) {
		if( m_varNameMap.get(varname) != null ) {
			return getVar(m_varNameMap.get(varname));
		}
		return "";	
	}
	
	public boolean isVarTrue( String varname ) {
		if( varvals.containsKey(varname) ) {
			Object val = varvals.get(varname);
			if( val == null ) {
				return false;
			}
			if( val instanceof Number ) {
				if( ((Number)val).doubleValue() != 0 ) {
					return true;
				}
				return false;
			}
			else if( val instanceof Boolean ) {
				return ((Boolean)val).booleanValue();
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
				if( !val.toString().equals("") ) {
					return true;
				}
				return false;
			}
		}
		return false;
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
		if( varvals.get(varname) != null ) {						
      		Object var = varvals.get(varname);
      		if( var instanceof Number ) {
      			return (Number)var;
      		}
      		try {
      			return Double.parseDouble(var.toString());
      		}
      		catch( NumberFormatException e ) {
      			return new Double(0);
      		}
		}
		return new Double(0);
	}

	/**
	 * Gibt den Inhalt einer Variablen in der Kontext-Antwort aus
	 * @param varname Der Name der auszugebenden Variablen
 	 */
	public void p( String varname ) {
		LOG.debug("out: "+varname);

		Object value = varvals.get(varname);
		m_controller.getResponse().getContent().append(value);
	}
	
	public void registerBlockItrnl( String name, String filehandle, String parent ) {
		LOG.debug("registered block: >"+name+"< >"+filehandle+"< >"+parent+"<");
		m_registeredBlocks.put(name, new String[] {filehandle, name, parent});
	}

	public void start_record() {
		record = true;
		if( LOG.isTraceEnabled() ) {
			LOG.trace("** start record\n");
		}
  	}

	public void stop_record() {
		record = false;
		if( LOG.isTraceEnabled() ) {
			LOG.trace("** stop record\n");
		}
	}

	public void clear_record() {
		for( String key : recordvars ) {
			if( LOG.isTraceEnabled() ) {
				LOG.trace("clear_record: "+key);
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
		m_controller.addError("Template: "+msg);
		LOG.error(msg);
 	}
 	
 	/*	
	 *	__call($method, $parameter)
	 *		php standard method
	 */
 	/*function __call( $method, $parameter ) {
 		if( $method == 'table_begin' ) {
 			echo call_user_func_array('table_begin', $parameter);
 		}
 		elseif( $method == 'table_end' ) {
 			echo table_end();	
 		}	
 	}*/
}
