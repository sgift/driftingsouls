package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEModule;
import net.driftingsouls.ds2.server.ships.SchiffstypModifikation;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.CollectionTable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;

/**
 * Ein Item, dass als Modul in ein Schiff eingebaut werden kann.
 */
@Entity
@DiscriminatorValue("Schiffsmodul")
public class Schiffsmodul extends Item
{
	@ElementCollection
	@CollectionTable(name = "schiffsmodul_slots")
	@ForeignKey(name = "schiffsmodul_slots_fk_schiffsmodul")
	private Set<String> slots = new HashSet<>();
	@ManyToOne
	@JoinColumn
	@ForeignKey(name = "schiffsmodul_fk_schiffsmodulset")
	private SchiffsmodulSet set;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name = "schiffsmodul_fk_schiffseffekt")
	private SchiffstypModifikation mods;

	/**
	 * Konstruktor.
	 */
	protected Schiffsmodul()
	{
	}

	/**
	 * Konstruktor.
	 *
	 * @param id Die ID
	 * @param name Der Name
	 */
	public Schiffsmodul(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 *
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public Schiffsmodul(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	/**
	 * Gibt die Slots zurueck, in die das Modul passt.
	 * @return Die Slots
	 */
	public Set<String> getSlots()
	{
		return slots;
	}

	/**
	 * Setzt die Slots in die das Modul passt.
	 * @param slots Die Slots
	 */
	public void setSlots(Set<String> slots)
	{
		this.slots = slots;
	}

	/**
	 * Gibt das Set-Item zurueck, zu dem das Modul gehoert.
	 * @return Das Set oder <code>null</code>
	 */
	public SchiffsmodulSet getSet()
	{
		return set;
	}

	/**
	 * Setzt das Set-Item, zu dem das Modul gehoert.
	 * @param set Das Set-Item oder <code>null</code>
	 */
	public void setSet(SchiffsmodulSet set)
	{
		this.set = set;
	}

	/**
	 * Gibt die Modifikationen am Schiffstyp zurueck, die dieses Modul bewirkt.
	 * @return die Modifikationen
	 */
	public SchiffstypModifikation getMods()
	{
		return mods;
	}

	/**
	 * Setzt die Modifkationen am Schiffstyp, die dieses Modul bewirkt.
	 * @param mods Die Modifikationen
	 */
	public void setMods(SchiffstypModifikation mods)
	{
		this.mods = mods;
	}

	@Override
	public IEModule getEffect()
	{
		return new IEModule(slots, mods, set);
	}
}
