package net.driftingsouls.ds2.server.entities.fraktionsgui;

import net.driftingsouls.ds2.server.entities.User;
import org.hibernate.annotations.ForeignKey;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Die Meldung einer Aktion bei einer Fraktion durch einen Spieler
 * mit der Absicht Loyalitaetspunkte bei dieser Fraktion zu erhalten.
 * @author Christopher Jung
 */
@Entity
@Table(name="fraktion_aktions_meldung")
public class FraktionAktionsMeldung
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Version
	private int version;

	@ManyToOne
	@JoinColumn
	@ForeignKey(name="fraktion_aktions_meldung_fk_users")
	private User gemeldetVon;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="fraktion_aktions_meldung_fk_users2")
	private User fraktion;
	@Lob
	private String meldungstext;
	private Date gemeldetAm;
	private Date bearbeitetAm;

	/**
	 * Konstruktor.
	 */
	protected FraktionAktionsMeldung()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param gemeldetVon Der Spieler der die Meldung erstellt hat
	 * @param fraktion Die Fraktion bei der die Aktion gemeldet wird
	 */
	public FraktionAktionsMeldung(User gemeldetVon, User fraktion)
	{
		this.gemeldetVon = gemeldetVon;
		this.fraktion = fraktion;
		this.gemeldetAm = new Date();
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem die Meldung bearbeitet wurde.
	 * @return Der Zeitpunkt
	 */
	public Date getBearbeitetAm()
	{
		return bearbeitetAm;
	}

	/**
	 * Setzt den Zeitpunkt, an dem die Meldung bearbeitet wurde.
	 * @param bearbeitetAm Der Zeitpunkt
	 */
	public void setBearbeitetAm(Date bearbeitetAm)
	{
		this.bearbeitetAm = bearbeitetAm;
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem die Meldung erstellt wurde.
	 * @return Der Zeitpunkt
	 */
	public Date getGemeldetAm()
	{
		return gemeldetAm;
	}

	/**
	 * Gibt den Spieler zurueck, der die Meldung erstellt hat.
	 * @return Der Spieler
	 */
	public User getGemeldetVon()
	{
		return gemeldetVon;
	}

	/**
	 * Gibt die Fraktion zurueck, bei der die Aktion gemeldet wurde.
	 * @return Die Fraktion
	 */
	public User getFraktion()
	{
		return fraktion;
	}

	/**
	 * Gibt den Text der Meldung zurueck.
	 * @return Der Text
	 */
	public String getMeldungstext()
	{
		return meldungstext;
	}

	/**
	 * Setzt den Text der Meldung.
	 * @param meldungstext Der Text
	 */
	public void setMeldungstext(String meldungstext)
	{
		this.meldungstext = meldungstext;
	}

	/**
	 * Gibt die ID der Meldung zurueck.
	 * @return Die Meldung
	 */
	public Long getId()
	{
		return id;
	}
}
