package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.modules.StatsController;
import net.sf.json.JSON;

/**
 * Ajax-Support fuer Statistikmodule.
 * @author Christopher Jung
 *
 */
public interface AjaxStatistic
{
	/**
	 * Liefert die Daten der Statistik als JSON-Response zurueck.
	 * @param contr Der Statistik-Kontroller
	 * @param size Die Anzahl der anzuzeigenden Eintraege
	 * @return Die JSON-Antwort
	 */
	public JSON generateData(StatsController contr, int size);
}
