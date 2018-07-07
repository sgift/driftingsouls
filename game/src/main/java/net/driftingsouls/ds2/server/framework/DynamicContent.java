package net.driftingsouls.ds2.server.framework;

import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.io.Serializable;
import java.util.Date;

/**
 * Metadaten zu einer dynamicContent-Datei.
 * @author Christopher Jung
 */
@Entity
@Table(name="dynamic_content")
public class DynamicContent implements Serializable
{

	public enum Lizenz
	{
		PUBLIC_DOMAIN("Public Domain"),
		CREATIVE_COMMONS("Creative Commons"),
		OPEN_SOURCE("Open Source"),
		DS_SPEZIFISCH("DS-Spezifisch"),
		DS_ALTBESTAND("DS-Altbestand mit unklarer Lizenz/Quelle");

		private final String label;
		Lizenz(String label)
		{
			this.label = label;
		}

		/**
		 * Gibt das Anzeigelabel des Lizenztyps zurueck.
		 * @return Das Label
		 */
		public String getLabel()
		{
			return this.label;
		}
	}

	@Id
	private String id;
	@Version
	private int version;
	private Date anlagedatum;
	private Date aenderungsdatum;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="dynamic_content_fk_users")
	private BasicUser hochgeladenDurch;
	private String quelle;
	private String autor;
	@Enumerated(EnumType.STRING)
	private Lizenz lizenz;
	@Lob
	private String lizenzdetails;

	/**
	 * Konstruktor.
	 */
	protected DynamicContent()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param id Die ID des Content (identisch mit Dateiname ohne Endung)
	 */
	public DynamicContent(String id)
	{
		this.id = id;
		this.lizenzdetails = "";
		this.quelle = "";
		this.autor = "";
		this.anlagedatum = new Date();
		this.aenderungsdatum = new Date();
	}

	/**
	 * Gibt die ID des Content zurueck.
	 * @return Die ID
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Gibt das Anlagedatum des Eintrags zurueck.
	 * @return Das Anlagedatum
	 */
	public Date getAnlagedatum()
	{
		return anlagedatum;
	}

	/**
	 * Setzt das Anlagedatum des Eintrags
	 * @param anlagedatum Das Datum
	 */
	public void setAnlagedatum(Date anlagedatum)
	{
		this.anlagedatum = anlagedatum;
	}

	/**
	 * Gibt zurueck, durch wen die Datei hochgeladen wurde.
	 * @return Der User
	 */
	public BasicUser getHochgeladenDurch()
	{
		return hochgeladenDurch;
	}

	/**
	 * Setzt, durch wen die Datei hochgeladen wurde.
	 * @param hochgeladenDurch Der User
	 */
	public void setHochgeladenDurch(BasicUser hochgeladenDurch)
	{
		this.hochgeladenDurch = hochgeladenDurch;
	}

	/**
	 * Gibt die Quelle der Datei zurueck.
	 * @return Die QUelle
	 */
	public String getQuelle()
	{
		return quelle;
	}

	/**
	 * Setzt die Quelle der Datei.
	 * @param quelle Die Quelle
	 */
	public void setQuelle(String quelle)
	{
		this.quelle = quelle;
	}

	/**
	 * Setzt den Autor der Datei.
	 * @return Der Autor
	 */
	public String getAutor()
	{
		return autor;
	}

	/**
	 * Gibt den Autor der Datei zurueck.
	 * @param autor Der Autor
	 */
	public void setAutor(String autor)
	{
		this.autor = autor;
	}

	/**
	 * Gibt den Lizenztyp zurueck, unter dem die Datei verfuegbar ist.
	 * @return Der Lizenztyp
	 */
	public Lizenz getLizenz()
	{
		return lizenz;
	}

	/**
	 * Setzt den Lizenztyp, unter dem die Datei verfuegbar ist.
	 * @param lizenz Der Lizenztyp
	 */
	public void setLizenz(Lizenz lizenz)
	{
		this.lizenz = lizenz;
	}

	/**
	 * Gibt die Details zur Lizenz zurueck.
	 * @return Die Details
	 */
	public String getLizenzdetails()
	{
		return lizenzdetails;
	}

	/**
	 * Setzt die Details zur Lizenz.
	 * @param lizenzdetails Die Details
	 */
	public void setLizenzdetails(String lizenzdetails)
	{
		this.lizenzdetails = lizenzdetails;
	}

	/**
	 * Gibt das Datum der letzten Aenderung zurueck.
	 * @return Das Datum
	 */
	public Date getAenderungsdatum()
	{
		return aenderungsdatum;
	}

	/**
	 * Setzt das Datum der letzten Aenderung.
	 * @param aenderungsdatum Das Datum
	 */
	public void setAenderungsdatum(Date aenderungsdatum)
	{
		this.aenderungsdatum = aenderungsdatum;
	}
}
