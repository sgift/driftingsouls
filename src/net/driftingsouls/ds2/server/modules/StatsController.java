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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.modules.stats.AjaxStatistic;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestAsteroid;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestFleet;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestPopulation;
import net.driftingsouls.ds2.server.modules.stats.StatBiggestTrader;
import net.driftingsouls.ds2.server.modules.stats.StatData;
import net.driftingsouls.ds2.server.modules.stats.StatEinheiten;
import net.driftingsouls.ds2.server.modules.stats.StatGtuPrice;
import net.driftingsouls.ds2.server.modules.stats.StatMemberCount;
import net.driftingsouls.ds2.server.modules.stats.StatOwnCiv;
import net.driftingsouls.ds2.server.modules.stats.StatOwnKampf;
import net.driftingsouls.ds2.server.modules.stats.StatOwnOffiziere;
import net.driftingsouls.ds2.server.modules.stats.StatPlayerList;
import net.driftingsouls.ds2.server.modules.stats.StatPopulationDensity;
import net.driftingsouls.ds2.server.modules.stats.StatRichestUser;
import net.driftingsouls.ds2.server.modules.stats.StatShipCount;
import net.driftingsouls.ds2.server.modules.stats.StatShips;
import net.driftingsouls.ds2.server.modules.stats.StatSpieleraktivitaet;
import net.driftingsouls.ds2.server.modules.stats.StatWaren;
import net.driftingsouls.ds2.server.modules.stats.StatWarenentwicklung;
import net.driftingsouls.ds2.server.modules.stats.Statistic;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Statistikseite.
 * @author Christopher Jung
 */
@Module(name="stats")
public class StatsController extends Controller
{
	/**
	 * Die minimale User/Ally-ID um in den Statistiken beruecksichtigt zu werden.
	 */
	public static final int MIN_USER_ID = 0;

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
	private Map<Integer,List<StatEntry>> statslist = new HashMap<>();
	private Map<String,Integer> catlist = new LinkedHashMap<>();

	/**
	 * Konstruktor.
	 */
	public StatsController() {
		super();

		setPageTitle("Statistik");

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

		registerStat( "Sonstiges", new StatPopulationDensity(), "Siedlungsdichte", 0 );
		registerStat( "Sonstiges", new StatShips(), "Schiffe", 0 );
		registerStat( "Sonstiges", new StatShipCount(), "Schiffsentwicklung", 0 );
		registerStat( "Sonstiges", new StatSpieleraktivitaet(), "Spieleraktivität", 0 );
		registerStat( "Sonstiges", new StatWarenentwicklung(), "Warenentwicklung", 0 );
		registerStat( "Sonstiges", new StatWaren(), "Waren", 0 );
		registerStat( "Sonstiges", new StatEinheiten(), "Einheiten", 0);
		registerStat( "Sonstiges", new StatData(), "Diverse Daten", 0 );

		registerStat( "Eigene K&auml;mpfe", new StatOwnKampf(), "Eigene K&auml;mpfe", 0 );
		registerStat( "Offiziere", new StatOwnOffiziere(), "Offiziere", 0 );
		registerStat( "Spielerliste", new StatPlayerList(), "Spielerliste", 0 );
	}

	protected int ermittleAnzuzeigendeStatistikkategorie(int show) {
		if( (show == 0) || !this.statslist.containsKey(show) ) {
			show = 1;
		}
		return show;
	}

	private void registerStat( String cat, Statistic stat, String name, int size ) {
		if( !this.catlist.containsKey(cat) ) {
			this.catlist.put(cat, this.catlist.size()+1);
		}

		if( !this.statslist.containsKey(this.catlist.get(cat)) ) {
			this.statslist.put(this.catlist.get(cat), new ArrayList<>());
		}
		this.statslist.get(this.catlist.get(cat)).add(new StatEntry(stat, name, size));
	}

	private void printMenu(int show) throws IOException {
		Writer echo = getContext().getResponse().getWriter();

		Map<Integer,String> lists = new HashMap<>();

		for( int listkey : this.statslist.keySet() ) {
			StringBuilder builder = new StringBuilder();

			List<StatEntry> alist = this.statslist.get(listkey);
			for( int i=0; i < alist.size(); i++ ) {
				builder.append("<dd><a style='font-size:12px;font-weight:normal' class='back' href='").append(Common.buildUrl("default", "show", listkey, "stat", i)).append("'>").append(alist.get(i).name).append("</a></dd>");
			}

			lists.put(listkey, builder.toString());
		}

		int catsize = this.catlist.size();
		int catpos = 0;

		echo.append("<div class='gfxbox' style='width:850px;text-align:center'>");
		for( String catkey : this.catlist.keySet() ) {
			int cat = this.catlist.get(catkey);

			if( this.statslist.containsKey(cat) && (this.statslist.get(cat).size() > 1) ) {
				echo.append("<div class='dropdown' style='width:120px'><dl><dt ");
				if( show == cat ) {
					echo.append("style=\"text-decoration:underline\"");
				}
				echo.append(">").append(catkey).append("<img style='vertical-align:middle; border:0px' src='./data/interface/uebersicht/icon_dropdown.gif' alt='' /></dt>\n");
				echo.append(lists.get(cat));
				echo.append("</dl></div>");
			}
			else {
				echo.append("<a ");
				if( show == cat ) {
					echo.append("style=\"text-decoration:underline\"");
				}
				echo.append(" class=\"forschinfo\" href=\"").append(Common.buildUrl("default", "show", cat)).append("\">").append(catkey).append("</a>\n");
			}

			if( catpos < catsize - 1 ) {
				echo.append(" | \n");
			}
			catpos++;
		}
		echo.append("</div>");
		echo.append("<div><br /><br /></div>\n");
	}

	/**
	 * Ajax-Modus der Statistikseite.
	 * @param stat Die ID der Statistik in der ausgewaehlten Kategorie
	 * @param show die ID der ausgeaehlten Kategorie
	 * @return Die JSON-Daten zur Statistik
	 * @throws IOException
	 */
	@Action(ActionType.AJAX)
	public AjaxStatistic.DataViewModel ajaxAction(int stat, int show) throws IOException {
		show = ermittleAnzuzeigendeStatistikkategorie(show);
		if( this.statslist.get(show).size() <= stat ) {
			stat = 1;
		}

		StatEntry mystat = this.statslist.get(show).get(stat);

		if( mystat.stat instanceof AjaxStatistic )
		{
			return ((AjaxStatistic)mystat.stat).generateData(this, mystat.width);
		}
		return new AjaxStatistic.DataViewModel();
	}

	/**
	 * Anzeige der Statistiken.
	 * @param stat Die ID der Statistik in der ausgewaehlten Kategorie
	 * @param show die ID der ausgeaehlten Kategorie
	 * @throws IOException
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(int stat, int show) throws IOException {
		show = ermittleAnzuzeigendeStatistikkategorie(show);
		if( this.statslist.get(show).size() <= stat ) {
			stat = 1;
		}

		StatEntry mystat = this.statslist.get(show).get(stat);

		printMenu(show);

		Writer echo = getContext().getResponse().getWriter();
		echo.write("<script type='text/javascript'>Stats.setCurrentStatistic("+show+","+stat+");</script>");

		echo.append("<div class='gfxbox statistic'>");

		mystat.stat.show(this, mystat.width);

		echo.append("</div>");
	}
}
