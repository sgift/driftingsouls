package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.config.items.effects.IEDraftShip;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;

/**
 * Der Bauplan zu einem Schiff.
 */
@Entity
@DiscriminatorValue("Schiffsbauplan")
public class Schiffsbauplan extends Item
{
	@ManyToOne
	@JoinColumn(name="schiffsbauplan_schiffstyp_id")
	@ForeignKey(name="schiffsbauplan_fk_schiffstyp")
	private ShipType schiffstyp;

	@ManyToOne
	@JoinColumn(name="schiffsbauplan_rasse_id")
	@ForeignKey(name="schiffsbauplan_fk_rasse")
	private Rasse rasse;
	private boolean flagschiff;

	@Type(type="largeCargo")
	private Cargo baukosten;
	private int crew;
	private int energiekosten;
	private int dauer;

	@ManyToMany
	@JoinTable(name="schiffsbauplan_forschungen")
	@ForeignKey(name="schiffsbauplan_fk_forschungen", inverseName = "schiffsbauplan_forschungen_fk_schiffsbauplan")
	private Set<Forschung> benoetigteForschungen;
	private int werftSlots;
	private boolean allianzEffekt;

	/**
	 * Konstruktor.
	 */
	protected Schiffsbauplan()
	{
		benoetigteForschungen = new HashSet<>();
		baukosten = new Cargo();
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 */
	public Schiffsbauplan(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public Schiffsbauplan(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	/**
	 * Gibt die Baukosten zurueck.
	 * @return Die Baukosten
	 */
	public Cargo getBaukosten() {
		return baukosten;
	}

	/**
	 * Gibt die zum Bau benoetigte Crew zurueck.
	 * @return Die Crew
	 */
	public int getCrew() {
		return crew;
	}

	/**
	 * Gibt die Baudauer zurueck.
	 * @return Die Dauer
	 */
	public int getDauer() {
		return dauer;
	}

	/**
	 * Gibt die Energiekosten zurueck.
	 * @return Die Energiekosten
	 */
	public int getEnergiekosten() {
		return energiekosten;
	}

	/**
	 * Gibt zurueck, ob hergestellte Schiffe Flagschiffe sind.
	 * @return <code>true</code>, falls es Flagschiffe sind
	 */
	public boolean isFlagschiff() {
		return flagschiff;
	}

	/**
	 * Gibt die Rasse zurueck, die diesen Baueintrag nutzen kann.
	 * @return Die Rasse
	 */
	public Rasse getRasse() {
		return rasse;
	}

	/**
	 * Gibt alle benoetigten Forschungen zurueck.
	 * @return Die Forschungen
	 */
	public Set<Forschung> getBenoetigteForschungen()
	{
		return this.benoetigteForschungen;
	}

	/**
	 * Gibt den Schiffstyp zurueck, welcher hiermit gebaut werden kann.
	 * @return Der Schiffstyp
	 */
	public ShipType getSchiffstyp() {
		return schiffstyp;
	}

	/**
	 * Setzt den Schiffstyp, welcher hiermit gebaut werden kann.
	 * @param type Der Schiffstyp
	 */
	public void setSchiffstyp(ShipType type)
	{
		this.schiffstyp = type;
	}

	/**
	 * Gibt die zum Bau benoetigte Werftslots zurueck.
	 * @return Die Werft-Requirements
	 */
	public int getWerftSlots() {
		return werftSlots;
	}

	/**
	 * Setzt die Baukosten.
	 * @param costs Die Baukosten
	 */
	public void setBaukosten(Cargo costs) {
		this.baukosten = costs;
	}

	/**
	 * Setzt die benoetigte Crew.
	 * @param crew Die Crew
	 */
	public void setCrew(int crew) {
		this.crew = crew;
	}

	/**
	 * Setzt die Dauer des Baus.
	 * @param dauer Die Dauer
	 */
	public void setDauer(int dauer) {
		this.dauer = dauer;
	}

	/**
	 * Setzt die Energiekosten.
	 * @param eKosten Die Energiekosten
	 */
	public void setEnergiekosten(int eKosten) {
		this.energiekosten = eKosten;
	}

	/**
	 * Setzt, ob das Schiff ein Flagschiff ist, oder nicht.
	 * @param isFlagschiff <code>true</code> wenn es sich um ein Flagschiff handelt ansonsten <code>false</code>
	 */
	public void setFlagschiff(boolean isFlagschiff) {
		this.flagschiff = isFlagschiff;
	}

	/**
	 * Setzt die zum Bau benoetigte Rasse.
	 * @param race Die Rasse
	 */
	public void setRasse(Rasse race) {
		this.rasse = race;
	}

	/**
	 * Setzt die benoetigten Forschungen.
	 * @param forschungen Die Forschungen
	 */
	public void setBenoetigteForschungen(Set<Forschung> forschungen) {
		this.benoetigteForschungen.clear();
		this.benoetigteForschungen.addAll(forschungen);
	}

	/**
	 * Setzt die Anzahl der benoetigten Werftslots.
	 * @param werftSlots Die Anzahl der Werftslots
	 */
	public void setWerftSlots(int werftSlots) {
		this.werftSlots = werftSlots;
	}

	/**
	 * Gibt zurueck, ob der Bauplan allianzweit zur Verfuegung gestellt werden kann.
	 * @return true, falls dem so ist
	 */
	public boolean isAllianzEffekt()
	{
		return allianzEffekt;
	}

	/**
	 * Setzt, ob der Bauplan allianzweit zur Verfuegung gestellt werden kann.
	 * @param allianzEffekt true, falls dem so ist
	 */
	public void setAllianzEffekt(boolean allianzEffekt)
	{
		this.allianzEffekt = allianzEffekt;
	}

	@Override
	public IEDraftShip getEffect()
	{
		return new IEDraftShip(this);
	}
}
