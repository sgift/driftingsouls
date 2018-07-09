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
package net.driftingsouls.ds2.server.modules.stats;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.entities.statistik.StatAktiveSpieler;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Zeigt allgemeine Daten zu DS und zum Server an.
 * @author Christopher Jung
 *
 */
public class StatSpieleraktivitaet implements Statistic {
	// ca 2 Monate anzeigen
	private static final int ANZAHL_TICKS = 7*7*8;
	// 2 Tage Abstand zum aktuellen Tick um keine direkten Rueckschluesse auf laufende "Aktionen" zu erlauben
	private static final int TICK_ABSTAND = 7*2;

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		Writer echo = context.getResponse().getWriter();

		int curTick = context.get(ContextCommon.class).getTick();

		List<List<Integer>> statistiken = new ArrayList<>();
		for( int i=0; i < 6; i++ ) {
			statistiken.add(new ArrayList<>());
		}
		List<List<Integer>> ticks = new ArrayList<>();

		int counter = 0;
		List<?> result = db.createQuery("from StatAktiveSpieler WHERE tick>=:mintick and tick<=:maxtick ORDER BY tick ASC")
				.setInteger("mintick", curTick-ANZAHL_TICKS-TICK_ABSTAND)
				.setInteger("maxtick", curTick-TICK_ABSTAND)
				.list();
		for (Object o : result)
		{
			StatAktiveSpieler sas = (StatAktiveSpieler)o;
			statistiken.get(0).add(sas.getSehrAktiv());
			statistiken.get(1).add(sas.getAktiv());
			statistiken.get(2).add(sas.getTeilweiseAktiv());
			statistiken.get(3).add(sas.getWenigAktiv());
			statistiken.get(4).add(sas.getInaktiv());
			statistiken.get(5).add(sas.getVacation());

			// Nur jeden 7 Eintrag labeln
			if( ++counter % 7 == 0 )
			{
				ticks.add(Arrays.asList(counter, sas.getTick()));
			}
		}

		echo.append("<div id='statspieleraktivitaet'></div><script type='text/javascript'>$(document).ready(function(){\n");
		echo.append("DS.plot('statspieleraktivitaet', ");
		echo.append(new Gson().toJson(statistiken));
		echo.append(", {");
		echo.append("highligher: {show:true, showTooltip: false},");
		echo.append("stackSeries:true, seriesDefaults:{fill:true}, legend: {show:true, placement:'outsideGrid'}, ");
		echo.append("series: [{label: 'Sehr Aktiv'},{label: 'Aktiv'},{label: 'Teilweise Aktiv'},{label: 'Wenig Aktiv'},{label: 'Inaktiv'}, {label: 'Urlaub'}], ");
		echo.append("axes:{xaxis:{ticks:").append(new Gson().toJson(ticks)).append(",label:'Tick',pad:0}, yaxis:{label:'Spieleranzahl', min:0, tickOptions: {formatString: '%d'}} }");
		echo.append("} )});");
		echo.append("</script>");
	}
}
