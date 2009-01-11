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
package net.driftingsouls.ds2.server.modules;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Die technische Datenbank mit Schiffsliste, Tutorial, 
 * Daten und Fakten usw.
 * 
 * @author Christopher Jung
 *
 */
@Configurable
public class TechDatabaseController extends TemplateGenerator {
	
	private Configuration config;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public TechDatabaseController(Context context) {
		super(context);
		
		setTemplate("techdatabase.html");	
	}
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }
	
	@Override
	protected boolean validateAndPrepare(String action) {
		// EMPTY
		return true;
	}
	
	@Override
	protected void printHeader( String action ) throws IOException {
		getContext().getResponse().setContentType("text/html", "UTF-8");
		getContext().getResponse().getWriter().append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		if( !action.equals("default") ) {
			getContext().getResponse().getWriter().append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
			getContext().getResponse().getWriter().append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\" lang=\"de\">");
			super.printHeader(action);	
		}
	}
	
	@Override
	protected void printFooter( String action ) throws IOException {
		if( !action.equals("default") ) {
			super.printFooter(action);	
			getContext().getResponse().getWriter().append("</html>");
		}
		else {
			getTemplateEngine().parse( "OUT", getTemplateID() );
			
			getTemplateEngine().p("OUT");
		}
	}
	
	/**
	 * Zeigt das Frameset an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		getTemplateEngine().setVar( "show.frame", 1 );
	}
	
	/**
	 * Zeigt eine leere Seite an.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void nothingAction() {
		getTemplateEngine().setVar( "show.none", 1 );
	}
	
	/**
	 * Zeigt das Tech-Datenbank Menue an.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void menuAction() {
		setDisableDebugOutput(true);
		
		getTemplateEngine().setVar( "show.menu", 1 );
	}
	
	/**
	 * Zeigt die Schiffsliste an.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void schiffslisteAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();

		SQLResultRow article = db.first("SELECT title,author,article FROM portal_articles WHERE id="+config.get("ARTICLE_SCHIFFSLISTE"));

		String text = Common._text(article.getString("article"));
		
		t.setVar(	"show.shiplist",	1,
					"shiplist.text",	text,
					"shiplist.author",	article.getString("author"),
					"shiplist.title",	article.getString("title") );
	}
	
	/**
	 * Zeigt das Tutorial an.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void tutorialAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();

		SQLResultRow article = db.first("SELECT title,author,article FROM portal_articles WHERE id="+config.get("ARTICLE_TUTORIAL"));

		String text = Common._text(article.getString("article"));
		
		t.setVar(	"show.shiplist",	1,
					"shiplist.text",	text,
					"shiplist.author",	article.getString("author"),
					"shiplist.title",	article.getString("title") );
	}
	
	private static Map<String,String> articleClasses = new HashMap<String,String>();
	static {
		articleClasses.put("ship", "Schiffe");
		articleClasses.put("race", "Rassen");
		articleClasses.put("groups", "Gruppierungen");
		articleClasses.put("history", "Geschichte");
		articleClasses.put("other", "Andere Fakten");
	}
	
	/**
	 * Zeigt die Liste der Daten und Faktenartikel an.
	 * Auf Wunsch wird zusaetzlich ein bestimmter Artikel angezeigt
	 * @urlparam Integer article Die ID des anzuzeigenden Artikels (0 bei keinem) 
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void factsAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("article");
		
		t.setVar("show.facts",1);

		t.setBlock("_TECHDATABASE", "facts.articlegroup.listitem", "facts.articlegroup.list");
		t.setBlock("facts.articlegroup.listitem", "facts.articles.listitem", "facts.articles.list");

		String oldclass = "";

		SQLQuery article = db.query("SELECT id,class,title FROM portal_facts ORDER BY class,title");
		
		while( article.next() ) {
			if( (oldclass.length() != 0) && !oldclass.equals(article.getString("class")) ) {
				t.setVar("articlegroup.name", articleClasses.get(oldclass));
				t.parse("facts.articlegroup.list", "facts.articlegroup.listitem",true);
				
				t.setVar("facts.articles.list", "" );
			}
			
			oldclass = article.getString("class");

			t.setVar(	"article.id",		article.getInt("id"),
						"article.title",	Common._title(article.getString("title")) );

			t.parse("facts.articles.list", "facts.articles.listitem", true);
		}
		article.free();

		t.setVar("articlegroup.name", articleClasses.get(oldclass));
		t.parse("facts.articlegroup.list", "facts.articlegroup.listitem", true);

		int articleID = getInteger("article");

		if( articleID != 0 ) {
			SQLResultRow thisarticle = db.first("SELECT title,text,class FROM portal_facts WHERE id='"+articleID+"'");

			t.setVar(	"article.class",	articleClasses.get(thisarticle.getString("class")),
						"article.title",	Common._title(thisarticle.getString("title")),
						"article.text",		Common._text(thisarticle.getString("text") ) );
		}
	}

}
