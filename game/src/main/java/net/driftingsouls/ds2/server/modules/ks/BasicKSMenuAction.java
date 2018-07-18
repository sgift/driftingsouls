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
package net.driftingsouls.ds2.server.modules.ks;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Basisklasse fuer KS-Menues.
 * @author Christopher Jung
 *
 */
public abstract class BasicKSMenuAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public BasicKSMenuAction() {
		// Diese beiden Dinge ueberprueft das KS automatisch fuer uns
		// - Vorausgesetzt wir werden als Menue aufgerufen!
		this.requireCommander(false);
		this.requireActive(false);
	}
	
	/**
	 * Generiert einen Menueeintrag mit Sicherheitsabfrage.
	 * @param t Das Templateengine 
	 * @param title Der Titel des Eintrags
	 * @param params URL-Parameter
	 * @param ask Der Text der Sicherheitsabfrage
	 */
	public static void menuEntryAsk(TemplateEngine t, String title, String params, String ask ) {
		t.setVar(	"menu.entry.params",	params,
					"menu.entry.title",		title,
					"ask.text",				StringEscapeUtils.escapeEcmaScript(ask) );
		t.parse("menu","menu.entry.ask",true);

	}

	/**
	 * Generiert einen Menueeintrag.
	 * @param t Das Templateengine
	 * @param title Der Titel des Eintrags
	 * @param params URL-Parameter
	 */
	public static void menuEntry(TemplateEngine t, String title, String params ) {
		t.setVar(	"menu.entry.params",	params,
					"menu.entry.title",		title );
		t.parse("menu","menu.entry",true);
	}
	
	protected void menuEntryAsk(TemplateEngine t, String title, Object[] params, String ask ) {
		String paramStr = "";
		if( params != null && params.length > 1 ) {
			StringBuilder paramBuilder = new StringBuilder(params.length*5);
			
			for( int i=0; i < params.length - 1; i+=2 ) {
				paramBuilder.append("&amp;").append(params[i]).append("=").append(params[i+1]);
			}
			paramStr = paramBuilder.toString();
		}
	
		menuEntryAsk(t, title, paramStr, ask);
	}
	
	protected void menuEntry(TemplateEngine t, String title, Object ... params ) {
		String paramStr = "";
		if( params != null && params.length > 1 ) {
			StringBuilder paramBuilder = new StringBuilder(params.length*5);
			
			for( int i=0; i < params.length - 1; i+=2 ) {
				paramBuilder.append("&amp;").append(params[i]).append("=").append(params[i + 1]);
			}
			paramStr = paramBuilder.toString();
		}
		
		menuEntry(t, title, paramStr);
	}
	
	/**
	 * Gibt zurueck, ob die angegebene Aktion ausfuehrbar ist.
	 * @param battle Die aktuelle Schlacht
	 * @param action Die Aktion
	 * @return Der Rueckgabewert
	 */
	protected Result isPossible( Battle battle, BasicKSAction action ) {
		action.setController(this.getController());
		return action.validate(battle);
	}
}
