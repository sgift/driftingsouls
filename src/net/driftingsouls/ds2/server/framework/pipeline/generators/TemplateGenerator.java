/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.framework.pipeline.generators;

import java.io.File;
import java.io.IOException;

import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Generator fuer Module mit Templates
 * @author Christopher Jung
 *
 */
public abstract class TemplateGenerator extends DSGenerator {
	private TemplateEngine templateEngine;
	private String masterTemplateID;
	
	/**
	 * Konstruktor
	 * @param context Der Kontext
	 */
	public TemplateGenerator(Context context) {
		super(context);
		
		templateEngine = null;
		masterTemplateID = "";
		
		parameterString("_style");
	}

	private void createTemplateEngine() {
		if( templateEngine != null ) {
			return;
		}
				
		templateEngine = new TemplateEngine();

		String style = getString("_style");
		if( !style.equals("") ) {
			templateEngine.setOverlay(style);	
		}
				
		if( getBrowser().equals("opera") ) {
			templateEngine.setVar("_BROWSER_OPERA",1);
		}
		else if( getBrowser().equals("msie") ) {
			templateEngine.setVar("_BROWSER_MSIE",1);
		}
		else {
			templateEngine.setVar("_BROWSER_MOZILLA",1);
		}
		
		if( getUser() != null ) {
			templateEngine.setVar("global.datadir", getUser().getImagePath());
		}
		else {
			templateEngine.setVar("global.datadir", BasicUser.getDefaultImagePath());
		}
		
		templateEngine.setVar(	"global.sess",	getString("sess"),
								"global.module", getString("module") );
	}
	
	/**
	 * Gibt die mit dem Generator verknuepfte Instanz des Template-Engines zurueck
	 * @return Das Template-Engine
	 */
	public TemplateEngine getTemplateEngine() {
		if( templateEngine == null ) {
			createTemplateEngine();
		}
		return templateEngine;
	}
	
	/**
	 * Gibt die ID der im System registrierten Template-File zurueck. 
	 * Sollte noch kein Template-File registriert sein, so wird ein leerer
	 * String zurueckgegeben
	 * @return die ID der Template-File oder <code>""</code>
	 */
	public String getTemplateID() {
		return masterTemplateID;
	}

	/**
	 * Setzt das vom Generator verwendete Template-File auf die angegebene Datei. Die Datei muss
	 * in kompilierter Form im System vorliegen (das vorhandensein der unkompilierten Variante ist nicht
	 * erforderlich).
	 * @param file Der Dateiname der unkompilierten Template-Datei
	 */
	public void setTemplate( String file ) {
		if( !file.equals("") ) {		
			if( templateEngine == null ) {
				createTemplateEngine();
			}
		
			String mastertemplate = new File(file).getName();
			if( mastertemplate.indexOf(".html") > -1 ) {
				mastertemplate = mastertemplate.substring(0,mastertemplate.lastIndexOf(".html"));
			}
			mastertemplate = "_"+mastertemplate.toUpperCase();
		
			masterTemplateID = mastertemplate;

			if( !templateEngine.setFile( masterTemplateID, file ) ) {
				masterTemplateID = "";
			}
			
			if( getContext().getActiveUser() != null ) {
				getContext().getActiveUser().setTemplateVars( templateEngine );	
			}	
		}
		else {
			masterTemplateID = "";	
		}
	}

	@Override
	protected void printFooter(String action) throws IOException {
		if( (getActionType() == ActionType.DEFAULT) && (getTemplateID().length() > 0) ) {
			getTemplateEngine().parse( "OUT", getTemplateID() );
				
			getTemplateEngine().p("OUT");
		}
		super.printFooter(action);
	}

	@Override
	protected void printHeader(String action) throws IOException {
		if( (getActionType() == ActionType.DEFAULT) && (this.templateEngine != null) ) {
			getOutputHelper().setAttribute("header", getTemplateEngine().getVar("__HEADER"));
		}
		super.printHeader(action);
	}
}
