package net.driftingsouls.ds2.server.modules.stats;

import com.google.gson.JsonElement;
import net.driftingsouls.ds2.server.modules.StatsController;

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
	public JsonElement generateData(StatsController contr, int size);
}
