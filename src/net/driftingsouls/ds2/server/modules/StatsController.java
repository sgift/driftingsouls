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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestAsteroid;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestFleet;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestPopulation;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestTrader;
import net.driftingsouls.ds2.server.modules.stats.StatData;
import net.driftingsouls.ds2.server.modules.stats.StatGtuPrice;
import net.driftingsouls.ds2.server.modules.stats.StatMemberCount;
import net.driftingsouls.ds2.server.modules.stats.StatOwnCiv;
import net.driftingsouls.ds2.server.modules.stats.StatOwnKampf;
import net.driftingsouls.ds2.server.modules.stats.StatOwnOffiziere;
import net.driftingsouls.ds2.server.modules.stats.StatPlayerList;
import net.driftingsouls.ds2.server.modules.stats.StatPopulationDensity;
import net.driftingsouls.ds2.server.modules.stats.StatRichestUser;
import net.driftingsouls.ds2.server.modules.stats.StatShips;
import net.driftingsouls.ds2.server.modules.stats.StatWaren;
import net.driftingsouls.ds2.server.modules.stats.Statistic;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Die Statistikseite
 * @author Christopher Jung
 *
 * @urlparam Integer stat Die ID der Statistik in der ausgewaehlten Kategorie
 * @urlparam Integer show die ID der ausgeaehlten Kategorie
 */
public class StatsController extends DSGenerator {
	/**
	 * Die minimale User/Ally-ID um in den Statistiken beruecksichtigt zu werden
	 */
	public static final int MIN_USER_ID = 0;
	/**
	 * Die groesste moegliche Forschungs-ID + 1
	 */
	public static final int MAX_RESID = 100;
	
	private static class StatEntry {
		Statistic stat;
		String name;
		int width;
		
		StatEntry( Statistic stat, String name, int width ) {
			this.stat = stat;
			this.name = name;
			this.width = width;
		}
	}
	private Map<Integer,List<StatEntry>> statslist = new HashMap<Integer,List<StatEntry>>();
	private Map<String,Integer> catlist = new LinkedHashMap<String,Integer>();
	private int show = 0;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public StatsController(Context context) {
		super(context);

		parameterNumber("stat");
		parameterNumber("show");
		
		setPageTitle("Statistik");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		registerStat( "Spieler", new StatOwnCiv(), "Meine Zivilisation", 0 );
		registerStat( "Spieler", new StatBiggestFleet(false), "Die gr&ouml;ssten Flotten", 60 );
		registerStat( "Spieler", new StatBiggestTrader(false), "Die gr&ouml;ssten Handelsflotten", 60);
		registerStat( "Spieler", new StatRichestUser(false), "Die reichsten Siedler", 60);
		registerStat( "Spieler", new StatBiggestPopulation(false), "Die gr&ouml;&szlig;ten V&ouml;lker", 30 );
		registerStat( "Spieler", new StatBiggestAsteroid(), "Die gr&ouml;&szlig;ten Asteroiden", 100 );
		registerStat( "Spieler", new StatGtuPrice(), "Die h&ouml;chsten Gebote", 60 );

		registerStat( "Allianzen", new StatBiggestFleet(true), "Die gr&ouml;ssten Flotten", 60 );
		registerStat( "Allianzen", new StatBiggestTrader(true), "Die gr&ouml;ssten Handelsflotten", 60);
		registerStat( "Allianzen", new StatRichestUser(true), "Die reichsten Allianzen", 60);
		registerStat( "Allianzen", new StatBiggestPopulation(true), "Die gr&ouml;&szlig;ten V&ouml;lker", 30 );
		registerStat( "Allianzen", new StatMemberCount(), "Die gr&ouml;&szlig;ten Allianzen", 30 );

		registerStat( "Sonstiges", new StatPopulationDensity(), "Besiedlungsdichte", 0 );
		registerStat( "Sonstiges", new StatShips(), "Schiffe", 0 );
		registerStat( "Sonstiges", new StatWaren(), "Waren", 0 );
		registerStat( "Sonstiges", new StatData(), "Diverse Daten", 0 );
		
		registerStat( "Eigene K&auml;mpfe", new StatOwnKampf(), "Eigene K&auml;mpfe", 0 );
		registerStat( "Offiziere", new StatOwnOffiziere(), "Offiziere", 0 );
		registerStat( "Spielerliste", new StatPlayerList(), "Spielerliste", 0 );
		
		int show = getInteger("show");
		if( (show == 0) || !this.statslist.containsKey(show) ) {
			show = 1;
		}
		this.show = show;
		
		return true;
	}
	
	private void registerStat( String cat, Statistic stat, String name, int size ) {
		if( !this.catlist.containsKey(cat) ) {
			this.catlist.put(cat, this.catlist.size()+1);
		}
	
		if( !this.statslist.containsKey(this.catlist.get(cat)) ) {
			this.statslist.put(this.catlist.get(cat), new ArrayList<StatEntry>());
		}
		this.statslist.get(this.catlist.get(cat)).add(new StatEntry(stat, name, size));
	}
	
	private void printMenu() {
		StringBuffer echo = getContext().getResponse().getContent();

		Map<Integer,String> lists = new HashMap<Integer,String>();

		for( int listkey : this.statslist.keySet() ) {
			StringBuilder builder = new StringBuilder();
			builder.append(StringUtils.replaceChars(Common.tableBegin(300, "left"), '"', '\''));
			
			List<StatEntry> alist = this.statslist.get(listkey);
			for( int i=0; i < alist.size(); i++ ) {
				builder.append("<a style='font-size:12px;font-weight:normal' class='back' href='"+Common.buildUrl("default", "show", listkey, "stat", i)+"'>"+alist.get(i).name+"</a><br />");
			}
	
			builder.append(StringUtils.replaceChars(Common.tableEnd(), '"', '\''));
			lists.put(listkey, StringEscapeUtils.escapeJavaScript(StringUtils.replace(StringUtils.replace(builder.toString(), "<", "&lt;"), ">", "&gt;")));
		}

		int catsize = this.catlist.size();
		int catpos = 0;

		echo.append(Common.tableBegin(750, "center"));
		for( String catkey : this.catlist.keySet() ) {
			int cat = this.catlist.get(catkey);
			echo.append("<a ");
			if( this.show == cat ) {
				echo.append("style=\"text-decoration:underline\"");
			}
			
			if( this.statslist.containsKey(cat) && (this.statslist.get(cat).size() > 1) ) {
				echo.append(" name=\"m"+cat+"_popup\" id=\"m"+cat+"_popup\" class=\"forschinfo\" onclick=\"javascript:overlib('"+lists.get(cat)+"', REF,'m"+cat+"_popup', REFY,22,FGCLASS,'gfxtooltip',BGCLASS,'gfxclass',TEXTFONTCLASS,'gfxclass',NOCLOSE,STICKY);\" onmouseout=\"return nd();\" href=\"#\">"+catkey+"</a>\n");
			}
			else {
				echo.append(" class=\"forschinfo\" href=\""+Common.buildUrl("default", "show", cat)+"\">"+catkey+"</a>\n");
			}
	
			if( catpos < catsize - 1 ) {
				echo.append(" | \n");
			}
			catpos++;
		}
		echo.append(Common.tableEnd());
		echo.append("<div><br /><br /></div>\n");
	}

	/**
	 * Anzeige der Statistiken
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		Database db = getDatabase();
		
		int stat = getInteger("stat");

		if( this.statslist.get(show).size() <= stat ) {
			stat = 1;
		}
		
		StatEntry mystat = this.statslist.get(this.show).get(stat);
		
		printMenu();

		// Groesste ID ermitteln
		int maxid = 0;
		if( mystat.stat.getRequiredData() != 0 ) {
			if( !mystat.stat.generateAllyData() ) {
				maxid = db.first("SELECT MAX(id) maxid FROM users").getInt("maxid");
			} 
			else {
				maxid = db.first("SELECT MAX(id) maxid FROM ally").getInt("maxid");
			}
		}
		
		if( (mystat.stat.getRequiredData() & Statistic.DATA_SHIPS) != 0 ||
			(mystat.stat.getRequiredData() & Statistic.DATA_CREW) != 0 ) {
			generateShipAndCrewData(db, mystat, maxid);
		}

		
		StringBuffer echo = getContext().getResponse().getContent();

		// TODO: Hack entfernen
		if( this.show == 3 ) {
			echo.append(Common.tableBegin(820,"left"));
		}
		else {
			echo.append(Common.tableBegin(700,"left"));
		}

		mystat.stat.show(this, mystat.width);

		echo.append(Common.tableEnd());
	}

	private void generateShipAndCrewData(Database db, StatEntry mystat, int maxid) {
		Map<Integer,Integer> bev = new HashMap<Integer,Integer>();
		Map<Integer,Integer> ships = new HashMap<Integer,Integer>();
		Map<Integer,Integer> bevbase = new HashMap<Integer,Integer>();
		
		//Schiffe pro User; Besatzung pro User ermitteln
		if( !mystat.stat.generateAllyData() ) {
			SQLQuery tmp = db.query("SELECT count(*) shipcount, sum(crew) totalcrew, owner FROM ships WHERE id>0 AND owner>",MIN_USER_ID," GROUP BY owner");
			while( tmp.next() ) {
				ships.put(tmp.getInt("owner"), tmp.getInt("shipcount"));
				bev.put(tmp.getInt("owner"), tmp.getInt("totalcrew"));
			}
			tmp.free();

			//Bevoelkerung (Basis) pro User ermitteln (+ zur Besatzung pro User addieren)
			tmp = db.query("SELECT sum(bewohner) bewohner,owner FROM bases WHERE owner>",MIN_USER_ID," GROUP BY owner");
			while( tmp.next() ){
				bevbase.put(tmp.getInt("owner"), tmp.getInt("bewohner"));
				if( !bev.containsKey(tmp.getInt("owner")) ) {
					bev.put(tmp.getInt("owner"), tmp.getInt("bewohner"));
				}
				else {
					bev.put(tmp.getInt("owner"), bev.get(tmp.getInt("owner"))+tmp.getInt("bewohner"));
				}
			}
			tmp.free();
		} 
		else {
			SQLQuery tmp = db.query("SELECT sum(s.crew) totalcrew, s.owner, count(*) shipcount, u.ally " +
					"FROM ships s JOIN users u ON s.owner=u.id " +
					"WHERE s.id>0 AND s.owner>",MIN_USER_ID," AND u.ally IS NOT NULL GROUP BY u.ally");
			while( tmp.next() ) {
		  		ships.put(tmp.getInt("ally"), tmp.getInt("shipcount"));
		  		bev.put(tmp.getInt("ally"), tmp.getInt("totalcrew"));
			}
			tmp.free();

			//Bevoelkerung (Basis) pro User ermitteln (+ zur Besatzung pro User addieren)
			tmp = db.query("SELECT sum(s.bewohner) bewohner,u.ally " +
					"FROM bases s JOIN users u ON s.owner=u.id " +
					"WHERE s.owner>",MIN_USER_ID," AND u.ally IS NOT NULL GROUP BY u.ally");
			while( tmp.next() ){
				bevbase.put(tmp.getInt("ally"), tmp.getInt("bewohner"));
				if( !bev.containsKey(tmp.getInt("ally")) ) {
					bev.put(tmp.getInt("ally"), tmp.getInt("bewohner"));
				}
				else {
					bev.put(tmp.getInt("ally"), bev.get(tmp.getInt("ally"))+tmp.getInt("bewohner"));
				}
			}
			tmp.free();
		} 

		///////////////////////////////////////////

		List<String> tmpships_query = new ArrayList<String>();
		List<String> tmpbev_query = new ArrayList<String>();

		for( int i=1; i <= maxid; i++ ) {
			if( ships.containsKey(i) ) {
				tmpships_query.add("("+i+","+ships.get(i)+")");
			}

			if( bev.containsKey(i) ) {
				tmpbev_query.add("("+i+","+bev.get(i)+")");
			}
		}

		if( (mystat.stat.getRequiredData() & Statistic.DATA_SHIPS) != 0 ) {
			db.update("CREATE TEMPORARY TABLE IF NOT EXISTS tmpships ( ",
					"id int(11) NOT NULL auto_increment, ",
					"ships int(11) NOT NULL default '0', ",
					"PRIMARY KEY  (id), ",
					"KEY ships(ships) ",
					");");
			
			db.update("DELETE FROM tmpships");
			
			if( tmpships_query.size() > 0 ) {
				db.update("INSERT INTO tmpships VALUES "+Common.implode(",", tmpships_query));
			}
		}

		if( (mystat.stat.getRequiredData() & Statistic.DATA_CREW) != 0 ) {
			db.update("CREATE TEMPORARY TABLE IF NOT EXISTS tmpbev ( ",
					"id int(11) NOT NULL auto_increment, ",
					"res int(11) NOT NULL default '0', ",
					"PRIMARY KEY  (`id`), ",
					"KEY res(res) ",
					");");
			
			db.update("DELETE FROM tmpbev");
			
			if( tmpbev_query.size() > 0 ) {
				db.update("INSERT INTO tmpbev VALUES "+Common.implode(",", tmpbev_query));
			}
		}
	}
}
