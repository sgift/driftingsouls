package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.modules.StatsController;

import java.util.ArrayList;
import java.util.List;

/**
 * Ajax-Support fuer Statistikmodule.
 * @author Christopher Jung
 *
 */
public interface AjaxStatistic
{
	@ViewModel
    class DataViewModel
	{
		public static class KeyViewModel
		{
			public int id;
			public String name;
		}

		public static class DataEntryViewModel
		{
			public int index;
			public long count;
		}

		public KeyViewModel key;
		public List<DataEntryViewModel> data = new ArrayList<>();
	}
	/**
	 * Liefert die Daten der Statistik als JSON-Response zurueck.
	 * @param contr Der Statistik-Kontroller
	 * @param size Die Anzahl der anzuzeigenden Eintraege
	 * @return Die JSON-Antwort
	 */
    DataViewModel generateData(StatsController contr, int size);
}
