package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEModuleSetMeta;
import net.driftingsouls.ds2.server.ships.SchiffstypModifikation;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import java.util.HashMap;
import java.util.Map;

/**
 * Ein Item, dass Set-Effekte bei mehreren eingebauten {@link net.driftingsouls.ds2.server.config.items.Schiffsmodul}en
 * beschreibt.
 */
@Entity
@DiscriminatorValue("SchiffsmodulSet")
public class SchiffsmodulSet extends Item
{
	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable
	@ForeignKey(name="schiffsmodulset_fk_schiffstypmodifikation", inverseName = "schiffstypmodifikation_fk_schiffsmodulset")
	private Map<Integer,SchiffstypModifikation> setEffekte  = new HashMap<>();

	/**
	 * Konstruktor.
	 */
	protected SchiffsmodulSet()
	{
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 */
	public SchiffsmodulSet(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public SchiffsmodulSet(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IEModuleSetMeta getEffect()
	{
		return new IEModuleSetMeta(this.getName(), this.setEffekte);
	}

	/**
	 * Gibt die Set-Effekte des Itemtyps zurueck. Der Key ist die Anzahl der mindestens eingebauten Items
	 * dieses Sets in einem Schiff. Der Value ist der dann jeweils eintretende Effekt
	 * @return Die Set-Effekte
	 */
	public Map<Integer, SchiffstypModifikation> getSetEffekte()
	{
		return setEffekte;
	}

	/**
	 * Setzt die Set-Effekte des Itemtyps. Der Key ist die Anzahl der mindestens eingebauten Items
	 * dieses Sets in einem Schiff. Der Value ist der dann jeweils eintretende Effekt
	 * @param setEffekte Die Set-Effekte
	 */
	public void setSetEffekte(Map<Integer, SchiffstypModifikation> setEffekte)
	{
		this.setEffekte = setEffekte;
	}
}
