package net.driftingsouls.ds2.server.cargo.modules;

import org.apache.commons.lang3.StringUtils;


/**
 * Repraesentiert ein in ein Schiff eingebautes Modul (oder vielmehr die Daten,
 * die hinterher verwendet werden um daraus ein Modul zu rekonstruieren).
 */
public class ModuleEntry {
	/**
	 * Der Slot in den das Modul eingebaut ist.
	 */
	private final int slot;
	/**
	 * Der Modultyp.
	 * @see net.driftingsouls.ds2.server.cargo.modules.Module
	 */
	private final ModuleType moduleType;
	/**
	 * Weitere Modultyp-spezifische Daten.
	 */
	private final String data;

	/**
	 * Konstruktor.
	 * @param slot Die ID des Slots
	 * @param moduleType Der Modultyp
	 * @param data Die Daten zum Modultyp
	 */
	public ModuleEntry(int slot, ModuleType moduleType, String data) {
		this.slot = slot;
		this.moduleType = moduleType;
		this.data = data;
	}

	/**
	 * Gibt zu den Moduldaten eines Slots auf einem Schiff eine passende Modul-Instanz
	 * zurueck.
	 * @return eine Modul-Instanz oder <code>null</code>, falls keine passende Instanz erzeugt werden konnte
	 */
	public Module createModule() {
		return this.getModuleType().createModule(this);
	}

	@Override
	public String toString() {
		return "ModuleEntry: "+this.slot+":"+this.moduleType+":"+this.data;
	}

	/**
	 * Gibt die ID des Slots zurueck.
	 * @return Die ID
	 */
	public int getSlot()
	{
		return slot;
	}

	/**
	 * Gibt den Modultyp zurueck.
	 * @return Der Typ
	 */
	public ModuleType getModuleType()
	{
		return moduleType;
	}

	/**
	 * Gibt die Daten zum Modultyp zurueck.
	 * @return Die Daten
	 */
	public String getData()
	{
		return data;
	}

	/**
	 * Serialisiert den Moduleintrag in das interne Speicherformat.
	 * @return Der serialisierte Moduleintrag
	 */
	public String serialize()
	{
		return this.slot+":"+this.moduleType.getOrdinal()+":"+this.data;
	}

	/**
	 * Deserialisiert einen Moduleintrag aus dem internen Speicherformat.
	 * @param moduleStr Der serialisierte Moduleintrag
	 * @return Der Moduleintrag
	 * @throws IllegalArgumentException Falls der serialisierte Moduleintrag keinem gueltigem Format genuegt
	 */
	public static ModuleEntry unserialize(String moduleStr) throws IllegalArgumentException
	{
		String[] module = StringUtils.split(moduleStr, ':');
		if( module.length != 3 )
		{
			throw new IllegalArgumentException("Ungueltiges Format");
		}
		ModuleType modType = ModuleType.fromOrdinal(Integer.parseInt(module[1]));
		if( modType == null )
		{
			throw new IllegalArgumentException("Ungueltiges Format");
		}

		return new ModuleEntry(Integer.parseInt(module[0]), modType, module[2]);
	}
}