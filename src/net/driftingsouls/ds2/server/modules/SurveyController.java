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

import java.util.HashMap;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Zeigt Umfragen an und laesst den aktiven Spieler die Umfrage ausfuellen
 * @author Christopher Jung
 *
 */
public class SurveyController extends TemplateGenerator {
	private SQLResultRow survey = null;
	
	private static final String ETYPE_TEXTBOX = "textbox";
	private static final String ETYPE_EDITBOX	= "editbox";
	private static final String ETYPE_RATEBOX = "ratebox";
	private static final String ETYPE_COMBOBOX = "combobox";
	private static final String ETYPE_DESCRIPTION = "description";
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public SurveyController(Context context) {
		super(context);
		
		setTemplate("survey.html");	
		
		setPageTitle("Umfrage");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		Database db = getDatabase();
		
		survey = db.first("SELECT * FROM surveys " ,
				"WHERE enabled='1' AND minid<='",user.getId(),"' AND maxid>='",user.getId(),"' AND " ,
				" mintime<='",user.getSignup(),"' AND maxtime>='",user.getSignup(),"' AND timeout>0");
				
		if( survey.isEmpty() ) {
			addError("Es existiert im Moment keine Umfrage, die sie beantworten k&ouml;nnten");
			return false;	
		}
		
		SQLResultRow voted = db.first("SELECT * FROM survey_voted WHERE survey_id=",survey.getInt("id")," AND user_id=",user.getId());
		if( !voted.isEmpty() ) {
			addError("Sie haben bei dieser Umfrage bereits abgestimmt");
			return false;	
		}
		
		return true;
	}

	/**
	 * Speichert ein Abstimmungsergebnis
	 * @urlparam * surveyentry_* Parameter Abhaengig von Feldtyp
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void submitAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = (User)getUser();
		
		HashMap<Integer,String> result = new HashMap<Integer,String>();
		
		SQLQuery entry = db.query("SELECT * FROM survey_entries WHERE survey_id=",this.survey.getInt("id"));
		while( entry.next() ) {
			String resultentry = "";
						
			if( entry.getString("type").equals(ETYPE_TEXTBOX) ) {
				parameterString("surveyentry_"+entry.getInt("id"));
				String text = getString("surveyentry_"+entry.getInt("id"));
				if( text.length() == 0 ) {
					text = "-";
				}
				resultentry = text;
			}
			else if( entry.getString("type").equals(ETYPE_EDITBOX) ) {
				parameterString("surveyentry_"+entry.getInt("id"));
				String text = getString("surveyentry_"+entry.getInt("id"));
				if( text.length() == 0 ) {
					text = "-";
				}
				resultentry = text;
			}
			else if( entry.getString("type").equals(ETYPE_RATEBOX) ) {
				parameterNumber("surveyentry_"+entry.getInt("id"));
				int rate = getInteger("surveyentry_"+entry.getInt("id"));
				if( (rate < 1) || (rate > 6) ) {
					rate = -1;
				} 
				resultentry = Integer.toString(rate);
			}				
			else if( entry.getString("type").equals(ETYPE_COMBOBOX) ) {
				parameterNumber("surveyentry_"+entry.getInt("id"));
				int item = getInteger("surveyentry_"+entry.getInt("id"));
				String[] itemlist = StringUtils.split(StringUtils.replace(entry.getString("params"), "\r\n","\n"), "\n");
				if( (item < 0) || (item >= itemlist.length) ) {
					item = -1;
				}
				resultentry = Integer.toString(item);		
			}
			
			if( resultentry.length() > 0 ) {
				result.put(entry.getInt("id"), resultentry);	
			}
		}
		entry.free();
		
		Byte[] data = ArrayUtils.toObject(SerializationUtils.serialize(result));
		
		db.prepare("INSERT INTO survey_results (survey_id,result) VALUES ( ?, ? )")
			.update(this.survey.getInt("id"), Common.implode(",", data));
		db.update("INSERT INTO survey_voted (survey_id,user_id) VALUES ('",this.survey.getInt("id"),"','",user.getId(),"')");
		
		t.setVar("show.votesuccessful", 1);
	}
	
	/**
	 * Zeigt die Umfrage an
	 * 
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.setVar(	"survey.name",	this.survey.getString("name"),
					"survey.id",	this.survey.getInt("id"),
					"show.survey",	1 );
		
		t.setBlock("_SURVEY", "survey.textbox.listitem", "tmp");
		t.setBlock("_SURVEY", "survey.text.listitem", "tmp");
		t.setBlock("_SURVEY", "survey.ratebox.listitem", "tmp");
		t.setBlock("_SURVEY", "survey.combobox.listitem", "tmp");
		t.setBlock("_SURVEY", "survey.description.listitem", "tmp");
		t.setBlock("survey.combobox.listitem", "comboentry.listitem","comboentry.list");
		
		SQLQuery entry = db.query("SELECT * FROM survey_entries WHERE survey_id=",this.survey.getInt("id"));
		while( entry.next() ) {
			t.setVar(	"entry.name",	Common._text(entry.getString("name")),
						"entry.id",		entry.getInt("id") );
			String entryblock = "";
			
			if( entry.getString("type").equals(ETYPE_TEXTBOX) ) {
				entryblock = "survey.textbox.listitem";
			}
			else if( entry.getString("type").equals(ETYPE_EDITBOX) ) {
				entryblock = "survey.text.listitem";
			}
			else if( entry.getString("type").equals(ETYPE_RATEBOX) ) {
				entryblock = "survey.ratebox.listitem";
			}				
			else if( entry.getString("type").equals(ETYPE_COMBOBOX) ) {
				entryblock = "survey.combobox.listitem";
				String[] itemlist = StringUtils.split(StringUtils.replace(entry.getString("params"), "\r\n","\n"), "\n");
				
				t.setVar("comboentry.list", "");

				for( int i=0; i < itemlist.length; i++ ) {
					t.setVar(	"comboentry.id",	i,
								"comboentry.name",	itemlist[i] ); 	
					
					t.parse("comboentry.list", "comboentry.listitem", true);
				}	
			}
			else if( entry.getString("type").equals(ETYPE_DESCRIPTION) ) {
				entryblock = "survey.description.listitem";
			}
			
			if( entryblock.length() > 0 ) {
				t.parse("survey.list", entryblock, true);
			}
		}
		entry.free();
	}
}
