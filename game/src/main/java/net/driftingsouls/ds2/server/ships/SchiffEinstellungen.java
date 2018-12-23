package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name="schiff_einstellungen")
@BatchSize(size=50)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class SchiffEinstellungen
{
	@SuppressWarnings("unused")
	@Id
	@GeneratedValue
	private int id;
	@SuppressWarnings("unused")
	@Version
	private int version;

	private int destsystem;
	private int destx;
	private int desty;
	@Lob
	@Column(nullable = false)
	private String destcom;
	@Index(name="schiffeinstellungen_bookmark")
	private boolean bookmark;
	private byte autodeut;
	private byte automine;
	private boolean startFighters;
	private boolean gotoSecondrow;
	@Index(name="schiffeinstellungen_feeding")
	private boolean isfeeding;
	@Index(name="schiffeinstellungen_allyfeeding")
	private boolean isallyfeeding;
	@Column(nullable = false)
	private TradepostVisibility showtradepost;


	public SchiffEinstellungen()
	{
		this.destcom = "";
		this.autodeut = 1;
		this.automine = 1;
		this.gotoSecondrow = true;
		this.showtradepost = TradepostVisibility.ALL;
	}

	/**
	 * Gibt zurueck, ob das Schiff automatisch Deuterium sammeln soll oder nicht.
	 * @return <code>true</code>, falls das Schiff automatisch Deuterium sammeln soll
	 */
	public boolean getAutoDeut() {
		return autodeut != 0;
	}

	/**
	 * Setzt das automatische Deuteriumsammeln.
	 * @param autodeut <code>true</code>, falls das Schiff automatisch Deuterium sammeln soll
	 */
	public void setAutoDeut(boolean autodeut) {
		this.autodeut = autodeut ? (byte)1 : 0;
	}

	/**
	 * Gibt zurueck, ob das Schiff automatisch Felsbrocken abbauen soll oder nicht.
	 * @return <code>true</code>, falls das Schiff automatisch Felsbrocken abbauen soll
	 */
	public boolean getAutoMine() {
		return automine != 0;
	}

	/**
	 * Setzt das automatische Felsbrockenabbauen.
	 * @param automine <code>true</code>, falls das Schiff automatisch Felsbrocken abbauen soll
	 */
	public void setAutoMine(boolean automine) {
		this.automine = automine ? (byte)1 : 0;
	}

	/**
	 * Gibt zurueck, ob das Schiff gebookmarkt ist.
	 * @return <code>true</code>, falls es ein Lesezeichen hat
	 */
	public boolean isBookmark() {
		return bookmark;
	}

	/**
	 * Setzt den Lesezeichenstatus fuer ein Schiff.
	 * @param bookmark <code>true</code>, falls es ein Lesezeichen haben soll
	 */
	public void setBookmark(boolean bookmark) {
		this.bookmark = bookmark;
	}

	/**
	 * Gibt zurueck, ob dieser Versorger aktuell versorgen soll.
	 * @return <code>true</code>, falls er versorgen soll
	 */
	public boolean isFeeding()
	{
		return isfeeding;
	}

	/**
	 * Setzt, ob dieser Versorger aktuell versorgen soll.
	 * @param feeding <code>true</code>, falls der Versorger versorgen soll
	 */
	public void setFeeding(boolean feeding)
	{
		this.isfeeding = feeding;
	}

	/**
	 * Gibt zuruecl, ob dieser Versorger Allianzschiffe mit versorgt.
	 * @return <code>true</code>, falls dieser Versorger Allianzschiffe versorgt
	 */
	public boolean isAllyFeeding()
	{
		return isallyfeeding;
	}

	/**
	 * Setzt, ob dieser Versorger Allianzschiffe mit versorgt.
	 * @param feeding <code>true</code>, falls dieser Versorger Allianzschiffe versorgen soll
	 */
	public void setAllyFeeding(boolean feeding)
	{
		this.isallyfeeding = feeding;
	}


	/**
	 * Gibt den mit dem Schiff assoziierten Kommentar zurueck.
	 * @return Der Kommentar
	 */
	public String getDestCom() {
		return destcom;
	}

	/**
	 * Setzt den Kommentar des Schiffes.
	 * @param destcom Der Kommentar
	 */
	public void setDestCom(String destcom) {
		this.destcom = destcom;
	}

	/**
	 * Gibt das Zielsystem zurueck.
	 * @return Das Zielsystem
	 */
	public int getDestSystem() {
		return destsystem;
	}

	/**
	 * Setzt das Zielsystem.
	 * @param destsystem Das neue Zielsystem
	 */
	public void setDestSystem(int destsystem) {
		this.destsystem = destsystem;
	}

	/**
	 * Gibt die Ziel-X-Koordinate zurueck.
	 * @return Die Ziel-X-Koordinate
	 */
	public int getDestX() {
		return destx;
	}

	/**
	 * Setzt die Ziel-X-Koordinate.
	 * @param destx Die neue X-Koordinate
	 */
	public void setDestX(int destx) {
		this.destx = destx;
	}

	/**
	 * Gibt die Ziel-Y-Koordinate zurueck.
	 * @return Die Ziel-Y-Koordinate
	 */
	public int getDestY() {
		return desty;
	}

	/**
	 * Setzt die Ziel-Y-Koordinate.
	 * @param desty Die neue Y-Koordinate
	 */
	public void setDestY(int desty) {
		this.desty = desty;
	}

	/**
	 * Gibt zurueck, ob Jaeger beim Kampfbeginn gestartet werden sollen.
	 * @return <code>true</code>, falls sie gestartet werden sollen
	 */
	public boolean startFighters() {
		return startFighters;
	}

	/**
	 * Setzt, ob Jaeger beim Kampfbeginn gestartet werden sollen.
	 * @param startFighters <code>true</code>, falls sie gestartet werden sollen
	 */
	public void setStartFighters(boolean startFighters) {
		this.startFighters = startFighters;
	}

	/**
	 * Gibt zurueck, ob Schiffe beim Kampfbeginn in die 2. Reihe wechseln.
	 * @return <code>true</code>, falls sie gestartet werden sollen
	 */
	public boolean gotoSecondrow() {
		return gotoSecondrow;
	}

	/**
	 * Setzt, ob Schiffe beim Kampfbeginn in die 2. Reihe wechseln.
	 * @param gotoSecondrow <code>true</code>, falls sie gestartet werden sollen
	 */
	public void setGotoSecondrow(boolean gotoSecondrow) {
		this.gotoSecondrow = gotoSecondrow;
	}

	/**
	 * returns who can see the tradepost entry in factions.
	 * @return The variable who can see the post
	 */
	@Enumerated
	public TradepostVisibility getShowtradepost()
	{
		return showtradepost;
	}

	/**
	 * Sets who can see the tradepost entry in factions.
	 * 0 everybody is able to see the tradepost.
	 * 1 everybody except enemys is able to see the tradepost.
	 * 2 every friend is able to see the tradepost.
	 * 3 the own allymembers are able to see the tradepost.
	 * 4 nobody except owner is able to see the tradepost.
	 * @param showtradepost the value who can see the tradepost.
	 */
	public void setShowtradepost(TradepostVisibility showtradepost)
	{
		this.showtradepost = showtradepost;
	}

	private boolean isModified()
	{
		if( this.destsystem != 0 || this.destx != 0 || this.desty != 0 )
		{
			return true;
		}
		if( this.destcom == null || !this.destcom.isEmpty() )
		{
			return true;
		}
		if( this.bookmark || this.autodeut != 1 || this.automine != 1 || this.startFighters || !this.gotoSecondrow)
		{
			return true;
		}
		if( this.isfeeding || this.isallyfeeding )
		{
			return true;
		}
		return this.showtradepost != TradepostVisibility.ALL;
	}

	public void persistIfNecessary(Ship ship)
	{
		if( !this.isModified() )
		{
			return;
		}
		ship.setEinstellungen(this);

		org.hibernate.Session db = ContextMap.getContext().getDB();
		if( !db.contains(this) )
		{
			db.persist(this);
		}
	}
}
