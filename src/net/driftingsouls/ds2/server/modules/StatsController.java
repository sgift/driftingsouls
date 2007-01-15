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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.Forschung;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestAsteroid;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestPopulation;
import net.driftingsouls.ds2.server.modules.stats.StatData;
import net.driftingsouls.ds2.server.modules.stats.StatGtuPrice;
import net.driftingsouls.ds2.server.modules.stats.StatMostResearch;
import net.driftingsouls.ds2.server.modules.stats.StatOwnCiv;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestFleet;
import net.driftingsouls.ds2.server.modules.stats.StatMemberCount;
import net.driftingsouls.ds2.server.modules.stats.StatOwnKampf;
import net.driftingsouls.ds2.server.modules.stats.StatOwnOffiziere;
import net.driftingsouls.ds2.server.modules.stats.StatPlayerList;
import net.driftingsouls.ds2.server.modules.stats.StatPopulationDensity;
import net.driftingsouls.ds2.server.modules.stats.StatResearchIsos;
import net.driftingsouls.ds2.server.modules.stats.StatResearchSilizium;
import net.driftingsouls.ds2.server.modules.stats.StatShips;
import net.driftingsouls.ds2.server.modules.stats.StatWaren;
import net.driftingsouls.ds2.server.modules.stats.Statistic;

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
	
	private class StatEntry {
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
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		registerStat( "Spieler", new StatOwnCiv(), "Meine Zivilisation", 0 );
		registerStat( "Spieler", new StatBiggestFleet(false), "Die gr&ouml;ssten Flotten", 60 );
		registerStat( "Spieler", new StatMostResearch(false), "Die meisten Forschungen", 60 );
		registerStat( "Spieler", new StatResearchSilizium(false), "Das meiste verforschte Silizium", 60 );
		registerStat( "Spieler", new StatResearchIsos(false), "Das meisten verforschten Isos", 60 );
		registerStat( "Spieler", new StatBiggestPopulation(false), "Die gr&ouml;&szlig;ten V&ouml;lker", 30 );
		registerStat( "Spieler", new StatBiggestAsteroid(), "Die gr&ouml;&szlig;ten Asteroiden", 100 );
		registerStat( "Spieler", new StatGtuPrice(), "Die h&ouml;chsten Gebote", 60 );

		registerStat( "Allianzen", new StatBiggestFleet(true), "Die gr&ouml;ssten Flotten", 60 );
		registerStat( "Allianzen", new StatMostResearch(true), "Die meisten Forschungen", 60 );
		registerStat( "Allianzen", new StatResearchSilizium(true), "Das meiste verforschte Silizium", 60 );
		registerStat( "Allianzen", new StatResearchIsos(true), "Das meisten verforschten Isos", 60 );
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
				builder.append("<a style='font-size:12px;font-weight:normal' class='back' href='"+Common.buildUrl(getContext(), "default", "show", listkey, "stat", i)+"'>"+alist.get(i).name+"</a><br />");
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
				echo.append(" class=\"forschinfo\" href=\""+Common.buildUrl(getContext(), "default", "show", cat)+"\">"+catkey+"</a>\n");
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
	public void defaultAction() {
		Database db = getDatabase();
		
		printMenu();
		
		Map<Integer,Integer> bev = new HashMap<Integer,Integer>();
		Map<Integer,Integer> ships = new HashMap<Integer,Integer>();
		Map<Integer,Integer> shipcount = new HashMap<Integer,Integer>();
		Map<Integer,Integer> bevbase = new HashMap<Integer,Integer>();

		// Groesste UserID ermitteln
		int maxid = 0;
		if( this.show == 1 ) {
			maxid = db.first("SELECT MAX(id) maxid FROM users").getInt("maxid");
		} 
		else if( this.show == 2 ) {
			maxid = db.first("SELECT MAX(id) maxid FROM ally").getInt("maxid");
		}

		db.update("CREATE TEMPORARY TABLE IF NOT EXISTS tmpbev ( ",
				"id int(11) NOT NULL auto_increment, ",
  				"res int(11) NOT NULL default '0', ",
  				"PRIMARY KEY  (`id`), ",
				"KEY res(res) ",
				");");
													
		db.update("CREATE TEMPORARY TABLE IF NOT EXISTS tmpres ( ",
 				"id int(11) NOT NULL auto_increment, ",
  				"res int(11) NOT NULL default '0', ",
  				"sili int(11) NOT NULL default '0', ",
  				"iso int(11) NOT NULL default '0', ",
  				"PRIMARY KEY  (id), ",
				"KEY res(res), ",
				"KEY sili(sili), ",
				"KEY iso(iso) ",
				");");

		db.update("CREATE TEMPORARY TABLE IF NOT EXISTS tmpships ( ",
  				"id int(11) NOT NULL auto_increment, ",
  				"ships int(11) NOT NULL default '0', ",
  				"PRIMARY KEY  (id), ",
				"KEY ships(ships) ",
				");");

		// Schiffe pro Typ
		SQLQuery tmp = db.query("SELECT count(*) typecount,type FROM ships WHERE id>0 AND owner>",MIN_USER_ID," GROUP BY type");
		while( tmp.next() ) {
			shipcount.put(tmp.getInt("type"), tmp.getInt("typecount"));
		}
		tmp.free();
		
		//Schiffe pro User; Besatzung pro User ermitteln
		if( this.show == 1 ) {
			tmp = db.query("SELECT count(*) shipcount, sum(crew) totalcrew, owner FROM ships WHERE id>0 AND owner>",MIN_USER_ID," GROUP BY owner");
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
		else if( this.show == 2 ) {
			tmp = db.query("SELECT sum(t1.crew) totalcrew, t1.owner, count(*) shipcount, t2.ally FROM ships t1 JOIN users t2 ON t1.owner=t2.id WHERE t1.id>0 AND t1.owner>",MIN_USER_ID," AND t2.ally>0 GROUP BY t2.ally");
			while( tmp.next() ) {
    	  		ships.put(tmp.getInt("ally"), tmp.getInt("shipcount"));
	      		bev.put(tmp.getInt("ally"), tmp.getInt("totalcrew"));
			}
			tmp.free();

			//Bevoelkerung (Basis) pro User ermitteln (+ zur Besatzung pro User addieren)
			tmp = db.query("SELECT sum(t1.bewohner) bewohner,t2.ally FROM bases t1 JOIN users t2 ON t1.owner=t2.id WHERE t1.owner>",MIN_USER_ID,"  AND t2.ally>0 GROUP BY t2.ally");
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
		db.update("DELETE FROM tmpships");
		db.update("DELETE FROM tmpbev");
		db.update("DELETE FROM tmpres");

		List<String> tmpships_query = new ArrayList<String>();
		List<String> tmpbev_query = new ArrayList<String>();

		if( (this.show == 1) || (this.show == 2) ) {
			for( int i=1; i <= maxid; i++ ) {
				if( ships.containsKey(i) ) {
					tmpships_query.add("("+i+","+ships.get(i)+")");
				}

				if( bev.containsKey(i) ) {
					tmpbev_query.add("("+i+","+bev.get(i)+")");
				}
			}
		}

		if( tmpships_query.size() > 0 ) {
			db.update("INSERT INTO tmpships VALUES "+Common.implode(",", tmpships_query));
		}

		if( tmpbev_query.size() > 0 ) {
			db.update("INSERT INTO tmpbev VALUES "+Common.implode(",", tmpbev_query));
		}

		///////////////////////////////////////////

		Map<Integer,Integer> res = new HashMap<Integer,Integer>();
		Map<Integer,Long> ress = new HashMap<Integer,Long>();
		Map<Integer,Long> resi = new HashMap<Integer,Long>();
		
		SQLQuery userf = null;
		if( this.show == 1 ) {
			userf = db.query("SELECT * FROM user_f WHERE id>",MIN_USER_ID);
			
		} 
		else if( this.show == 2 ) {
			userf = db.query("SELECT t1.*,t2.ally id FROM user_f t1 JOIN users t2 ON t1.id=t2.id WHERE t1.id>",MIN_USER_ID," AND t2.ally>0");
		}
		if( userf != null ) {
			while( userf.next() ) {
				int i = userf.getInt("id");
				for( int a=1; a < MAX_RESID; a++ ) {
					if( userf.getInt("r"+a) == 1 ) {
						if( !res.containsKey(i) ) {
							res.put(i, 0);
							ress.put(i, 0l);
							resi.put(i, 0l);
						}
						res.put(i, res.get(i)+1);
						
	   					Forschung forschung = Forschung.getInstance(a);
	   					if( forschung != null ) {
	   						Cargo cargo = new Cargo(Cargo.Type.STRING,forschung.getCosts());
	   						ress.put( i, ress.get(i) + cargo.getResourceCount( Resources.SILIZIUM ) );
	   						resi.put(i, resi.get(i) + cargo.getResourceCount( Resources.ISOCHIPS ) );
	   					}
	  				}
				}
			}
			userf.free();
		}
		
		List<String> insertstring = new ArrayList<String>();
		for( int i=1; i <= maxid; i++ ) {
			if( res.containsKey(i) ) {
				insertstring.add("("+i+","+res.get(i)+","+ress.get(i)+","+resi.get(i)+")");
			}
		}
		
		if( insertstring.size() > 0 ) {
			db.update("INSERT INTO tmpres VALUES "+Common.implode(",", insertstring));
		}
		
		///////////////////////////////////////////
		///////////////////////////////////////////

		int stat = getInteger("stat");

		if( this.statslist.get(show).size() <= stat ) {
			stat = 1;
		}
		
		StringBuffer echo = getContext().getResponse().getContent();

		if( this.show == 6 ) {
			echo.append(Common.tableBegin(700,"left"));
		}
		else if( this.show == 4 ) {
			echo.append(Common.tableBegin(700,"left"));	
		}
		else if( (this.show != 3) ) {
			echo.append(Common.tableBegin(700,"left"));
		}
		else {
			echo.append(Common.tableBegin(820,"left"));
		}

		StatEntry mystat = this.statslist.get(this.show).get(stat);
		
		mystat.stat.show(this, mystat.width);

		echo.append(Common.tableEnd());
	}
}
