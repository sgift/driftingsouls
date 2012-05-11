/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.units;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.driftingsouls.ds2.server.bases.BaseUnitCargoEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipUnitCargoEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Repraesentiert einen UnitCargo, also eine Liste von Einheiten mit jeweils einer bestimmten Menge, in DS.
 * <p>Hinweis zu {@link #equals(Object)} und {@link #hashCode()}:<br>
 * Zwei UnitCargoobjekte sind dann gleich, wenn sie zum Zeitpunkt des Vergleichs den selben Inhalt haben. Es wird nicht
 * beruecksichtigt ob die Optionen gleich sind oder die Cargos bei der Initalisierung einen unterschiedlichen Inhalt hatten.</p>
 *
 */
public class UnitCargo implements Cloneable {
	private static final Log log = LogFactory.getLog(UnitCargo.class);
	/**
	 * Variable zum erkennen eines Baseneintrags.
	 */
	public static final int CARGO_ENTRY_BASE = 1;
	/**
	 * Variable zum erkennen eines Schiffseintrags.
	 */
	public static final int CARGO_ENTRY_SHIP = 2;
	
	/**
	 * Diese Klasse ist fuer das Kapern gedacht um die verbleibende Crew auf.
	 * dem Schiff ueber Seiteneffekte zurueckgeben zu koennen
	 */
	public static class Crew {
		int wert;
		
		/**
		 * Konstruktor.
		 */
		public Crew()
		{
			// EMPTY
		}
		
		/**
		 * Konstruktor.
		 * @param wert der Wert
		 */
		public Crew(int wert)
		{
			this.wert = wert;
		}
		
		/**
		 * Gibt den Wert zurueck.
		 * @return der aktuell enthaltene Wert
		 */
		public int getValue()
		{
			return wert;
		}
		
		/**
		 * Setzt den Wert.
		 * @param wert der neue Wert
		 */
		public void setValue(int wert)
		{
			this.wert = wert;
		}
	}
	
	/**
	 * Die verschiedenen Optionen des UnitCargo-Objekts.
	 * @see UnitCargo#setOption(net.driftingsouls.ds2.server.units.UnitCargo.Option, Object)
	 */
	public enum Option {
		/**
		 * Soll die Masse, die einer Einheit verbraucht, angezeigt werden? (java.lang.Boolean)
		 */
		SHOWMASS,
	};
	
	private List<UnitCargoEntry> units = new ArrayList<UnitCargoEntry>();
	private int type;
	private int destid;
	
	private boolean showmass = true;
	
	/**
	 * Erstellt ein neues leeres UnitCargo-Objekt.
	 *
	 */
	public UnitCargo() {
		// Type.EMPTY
	}
	
	/**
	 * <p>Konstruktor.</p>
	 * Erstellt einen neuen UnitCargo aus dem aktuellen UnitCargo sowie den Optionen eines anderen UnitCargo-Objekts.
	 * @param unitcargo Der UnitCargo, dessen Daten genommen werden sollen
	 */
	public UnitCargo(UnitCargo unitcargo) {
		
		List<UnitCargoEntry> unitArray = unitcargo.getUnitArray();
		for( int i=0; i < unitArray.size(); i++ ) {
			UnitCargoEntry unit = unitArray.get(i);
		
			this.units.add(unit.clone());
		}
		
		this.showmass = (Boolean)unitcargo.getOption(Option.SHOWMASS);
	}
	
	/**
	 * Konstruktor, Stellt einen EinheitenCargo des Zielobjekts zusammen.
	 * @param type Der Typ des Eintrages
	 * @param destid Die ID des Zielobjektes
	 */
	public UnitCargo(int type, int destid)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		this.units = Common.cast(db.createQuery("from UnitCargoEntry where key.type = :type and key.destid = :destid")
													.setInteger("type", type)
													.setInteger("destid", destid)
													.list());
		this.type = type;
		this.destid = destid;
		
	}
	
	/**
	 * Ein neuer UnitCargo.
	 * 
	 * @param units Die Einheiten in diesem Cargo.
	 * @param type Cargotyp (Schiff oder Basis).
	 * @param destid Die Id des Zielobjekts.
	 */
	public UnitCargo(List<UnitCargoEntry> units, int type, int destid)
	{
		this.units = units;
		this.type = type;
		this.destid = destid;
	}
	
	protected List<UnitCargoEntry> getUnitArray() {
		return units;
	}
	
	/**
	 * Speichert das aktuelle UnitCargoObjekt.
	 */
	public void save() 
	{
		if( this.type == 0 || this.destid == 0)
		{
			log.warn("Nicht genug Daten zum speichern eines UnitCargoObjektes");
			return;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<UnitCargoEntry> entries = Common.cast(db.createQuery("from UnitCargoEntry where key.type = :type and key.destid = :destid")
						.setInteger("type", this.type)
						.setInteger("destid", this.destid)
						.list());
		
		for(UnitCargoEntry entry: entries)
		{
			db.delete(entry);
		}
		
		db.flush();
		
		for(UnitCargoEntry entry: units)
		{
			db.persist(entry);
		}
	}
	
	/**
	 * Setzt den Typ des UnitCargos.
	 * @param type der Typ
	 */
	public void setTyp(int type)
	{
		this.type = type;
	}

	/**
	 * Setzt die ID des Zielobjektes.
	 * @param destid Die ID
	 */
	public void setDestId(int destid)
	{
		this.destid = destid;
	}
	
	/**
	 * Fuegt dem UnitCargo die angegebene Einheit in der angegebenen Hoehe hinzu.
	 * @param unitid Die Einheit
	 * @param count Die Anzahl an hinzuzufuegenden Einheiten
	 */
	public void addUnit( int unitid, long count ) {
		for( int i=0; i < units.size(); i++ ) {
			UnitCargoEntry aunit = units.get(i);
			if( unitid == aunit.getUnitTypeId()) {
				aunit.setAmount(aunit.getAmount()+count);
				return;
			}
		}

		UnitCargoEntry entry;
		if(this.type == UnitCargo.CARGO_ENTRY_BASE)
		{
			entry = new BaseUnitCargoEntry(this.type, this.destid, unitid, count);
		}
		else
		{
			entry = new ShipUnitCargoEntry(type, destid, unitid, count);
		}
		units.add(entry);
	}
	
	/**
	 * Verringert die angegebene Einheit im UnitCargo um den angegebenen Wert.
	 * @param unitid Die Einheit
	 * @param count Die Anzahl an Einheiten, die abgezogen werden sollen
	 */
	public void substractUnit( int unitid, long count ) {
		for( int i=0; i < units.size(); i++ ) {
			UnitCargoEntry aunit = units.get(i);
			if( unitid == aunit.getUnitTypeId()) {
				aunit.setAmount(aunit.getAmount()-count);
				if( aunit.getAmount() == 0 ) {
					units.remove(i);
				}
					return;
			}
		}
		
		UnitCargoEntry entry;
		if(this.type == UnitCargo.CARGO_ENTRY_BASE)
		{
			entry = new BaseUnitCargoEntry(this.type,this.destid,unitid,-count);
		}
		else
		{
			entry = new ShipUnitCargoEntry(this.type,this.destid,unitid,-count);
		}
	
		// Diese Anweisung wird nur ausgefuerht, wenn das Item nicht im Cargo vorhanden ist
		units.add(entry);
	}
	
	/**
	 * Ueberprueft ob eine Einheit vorhanden ist.
	 * @param unitid Die Einheiten-ID
	 * @return <code>true</code>, falls die Einheit vorhanden ist
	 */
	public boolean hasUnit( int unitid ) {
		return hasUnit(unitid, 0 );
	}
	
	/**
	 * Ueberprueft ob eine Einheit in mindestens der angegebenen Menge vorhanden ist.
	 * @param unitid Die Einheiten-ID
	 * @param count Die Mindestmenge
	 * @return <code>true</code>, falls die Einheit in der Menge vorhanden ist
	 */
	public boolean hasUnit( int unitid, long count ) {
		long amount = getUnitCount(unitid);
		if( count != 0 ) {
			return (amount >= count);
		}
		if( amount != 0 ) {
			return true;
		}
		return false;
	}
	
	/**
	 * Gibt die Anzahl der vorhandenen Einheiten im UnitCargo zurueck.
	 * @param unitid Die gewuenschte Eiheit
	 * @return die Anzahl der Einheiten
	 */
	public long getUnitCount( int unitid ) {
		for( int i=0; i < units.size(); i++ ) {
			UnitCargoEntry aunit = units.get(i);
			if( unitid == aunit.getUnitTypeId()) {
				return aunit.getAmount();
			}
		}
		
		return 0;
	}
	
	/**
	 * Gibt die Gesamtmasse aller Einheiten im <code>UnitCargo</code>-Objekt zurueck.
	 * @return Die Gesamtmasse
	 */
	public long getMass() {
		long tmp = 0;
		
		for( int i=0; i < units.size(); i++ ) {
			UnitType unittype = units.get(i).getUnitType();
			if( unittype == null ) {
				log.warn("Unbekannte Einheit "+units.get(i).getUnitTypeId()+" geortet");
				continue;
			}
			tmp += units.get(i).getAmount()*unittype.getSize();
		} 
		
		return tmp;
	}
	
	/**
	 * Zieht vom UnitCargo den angegebenen UnitCargo ab.
	 * 
	 * @param subcargo Der UnitCargo, um dessen Inhalt dieser UnitCargo verringert werden soll
	 */
	public void substractCargo( UnitCargo subcargo ) {

		List<UnitCargoEntry> units = subcargo.getUnitArray();

		if( !units.isEmpty() ) {			
			for( UnitCargoEntry unit : units ) {
				substractUnit(unit.getUnitTypeId(), unit.getAmount());
			}
		}
	}
	
	/**
	 * Fuegt dem UnitCargo den angegebenen UnitCargo hinzu.
	 * 
	 * @param addcargo Der UnitCargo, um dessen Inhalt dieser UnitCargo erhoeht werden soll
	 */
	public void addCargo( UnitCargo addcargo ) {
		
		List<UnitCargoEntry> units = addcargo.getUnitArray();

		if( !units.isEmpty() ) {			
			for( UnitCargoEntry unit : units ) {
				addUnit(unit.getUnitTypeId(),unit.getAmount());
			}
		}
	}
	
	/**
	 * Setzt die vorhandene Menge der angegebenen Einheit auf 
	 * den angegebenen Wert.
	 * @param unitid Die Einheiten-ID
	 * @param count Die neue Menge
	 */
	public void setUnit( int unitid, long count ) {
		for( int i=0; i < units.size(); i++ ) {
			UnitCargoEntry aunit = units.get(i);
			if( unitid == aunit.getUnitTypeId()) {
				aunit.setAmount(count);
				return;
			}
		}
		
		UnitCargoEntry entry;
		if(this.type == UnitCargo.CARGO_ENTRY_BASE)
		{
			entry = new BaseUnitCargoEntry(this.type,this.destid,unitid,count);
		}
		else
		{
			entry = new ShipUnitCargoEntry(this.type,this.destid,unitid,count);
		}
		units.add(entry);
	}
	
	/**
	 * Prueft, ob der UnitCargo leer ist.
	 * @return <code>true</code>, falls er leer ist
	 */
	public boolean isEmpty() {
		if(units.isEmpty())
		{
			return true;
		}
		
		for( int i=0; i < units.size(); i++ ) 
		{
			UnitCargoEntry aunit = units.get(i);
			if( aunit.getAmount() > 0 ) 
			{
				return false;
			}
		}	
		
		return true;
	}
	
	/**
	 * Setzt eine Option auf den angegebenen Wert.
	 * @param option Die Option
	 * @param data Der Wert
	 */
	public void setOption( Option option, Object data ) {
		switch( option ) {
		case SHOWMASS:
			showmass = (Boolean)data;
			break;
		}
	}
	
	/**
	 * Gibt den Wert einer Option zurueck.
	 * @param option Die Option
	 * @return Der Wert
	 */
	public Object getOption( Option option ) {
		switch( option ) {
		case SHOWMASS:
			return showmass;
		
		}
		return null;
	}
	
	@Override
	public Object clone() {
		try {
			UnitCargo newcargo = (UnitCargo)super.clone();
			newcargo.units = new ArrayList<UnitCargoEntry>();
			for( int i=0; i < this.units.size(); i++ ) {
				newcargo.units.add(i, this.units.get(i).clone());
			}
			newcargo.showmass = this.showmass;

			return newcargo;
		}
		catch( CloneNotSupportedException e ) {
			// Sollte nie passieren, da alle Klassen ueber uns klonen koennen....
			return null;
		}
	}

	/**
	 * Gibt die Masse einer Einheit in einer bestimmten Menge zurueck.
	 * 
	 * @param unitid Die Einheiten-ID
	 * @param count Die Menge
	 * @return Die Masse, die die Einheit in der Menge verbraucht
	 */
	public static long getUnitMass( int unitid, long count ) {
		long tmp = 0;
		org.hibernate.Session db = ContextMap.getContext().getDB();
	
		UnitType unittype = (UnitType)db.get(UnitType.class, unitid);
		
		if( unittype != null ) {
			tmp = count * unittype.getSize();
		}
		
		return tmp;
	}
	
	/**
	 * Prueft, ob zwei UnitCargos im Moment den selben Inhalt haben.
	 * Es wird nicht geprueft, ob die Optionen gleich sind!
	 * @param obj Der zu vergleichende UnitCargo
	 * @return <code>true</code>, falls der Inhalt gleich ist
	 */
	@Override
	public boolean equals(Object obj) {
		if( !(obj instanceof UnitCargo) ) {
			return false;
		}
		UnitCargo c = (UnitCargo)obj;
		
		if( this.units.size() != c.units.size() ) {
			return false;
		}
		
		UnitCargo tmpcargo = (UnitCargo)clone();
		tmpcargo.substractCargo(c);
		
		return tmpcargo.isEmpty();
	}
	
	/**
	 * Gibt die Anzahl an verbrauchter Nahrung durch die Einheiten im UnitCargo zurueck.
	 * @return Die Anzahl an verbrauchter Nahrung
	 */
	public int getNahrung()
	{
		if(isEmpty())
		{
			return 0;
		}
		
		double nahrungsverbrauch = 0;
		for(UnitCargoEntry aunit : units)
		{
			UnitType unittype = aunit.getUnitType();
			nahrungsverbrauch += unittype.getNahrungCost() * aunit.getAmount();
		}
		
		return (int)Math.ceil(nahrungsverbrauch);
	}
	
	/**
	 * Gibt die Anzahl an verbrauchten RE durch die Einheiten im UnitCargo zurueck.
	 * @return Die Anzahl an verbrauchten RE
	 */
	public int getRE()
	{
		if(isEmpty())
		{
			return 0;
		}
		
		double reverbrauch = 0;
		for(UnitCargoEntry aunit : units)
		{
			UnitType unittype = aunit.getUnitType();
			reverbrauch += unittype.getReCost() * aunit.getAmount();
		}
		
		return (int)Math.ceil(reverbrauch);
	}
	
	/**
	 * Gibt den Kaperwert der Einheiten des UnitCargos zurueck.
	 * @return Der Kaperwert
	 */
	public int getKaperValue()
	{
		if(isEmpty())
		{
			return 0;
		}
		
		int kapervalue = 0;
		for(UnitCargoEntry aunit : units)
		{
			UnitType unittype = aunit.getUnitType();
			kapervalue += unittype.getKaperValue() * aunit.getAmount();
		}
		
		return kapervalue;
	}
	
	/**
	 * Es fliehen so viele Einheiten, wie keine Nahrung mehr vorhanden ist.
	 * @param nahrung Die Nahrung die durch die Einheiten zu viel verbraucht wird
	 */
	public void fleeUnits(int nahrung)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<UnitType> unittypes = Common.cast(db.createQuery("FROM UnitType ORDER BY nahrungcost DESC").list());
	
		for(UnitType unittype : unittypes)
		{
			if(hasUnit(unittype.getId()))
			{
				// Aktuelle Einheiten verbrauchen weniger Nahrung als weg muss
				// Alle Einheiten wegpacken und Nahrung reduzieren.
				if(getUnitCount(unittype.getId()) * unittype.getNahrungCost() < nahrung)
				{
					nahrung -= getUnitCount(unittype.getId()) * unittype.getNahrungCost();
					setUnit(unittype.getId(), 0);
				}
				else
				{
					setUnit(unittype.getId(), (long)(getUnitCount(unittype.getId()) - Math.ceil(nahrung / unittype.getNahrungCost())));
					nahrung = 0;
				}
			}
		}
	}
	
	/**
	 * Gibt die Einheitenliste via TemplateEngine aus.
	 * @param t Das TemplateEngine
	 * @param templateblock Der Name des betreffenden TemplateBlocks
	 * @param templateitem Der Name eines Items des TemplateBlocks
	 */
	public void echoUnitList( TemplateEngine t, String templateblock, String templateitem ) {
		
		if(isEmpty())
		{
			return;
		}
		
		t.setVar(templateblock,"");
		
		for( UnitCargoEntry aunit : units ) {
			UnitType unittype = aunit.getUnitType();
			
			t.setVar(	"res.id", 			aunit.getUnitTypeId(),
						"res.count",		Common.ln(aunit.getAmount()),
						"res.name",			unittype.getName(),
						"res.picture",		unittype.getPicture() );
			
			t.parse(templateblock,templateitem,true);
		}
	}
	
	/**
	 * Vergleicht den aktuellen UnitCargo mit dem uebergebenen UnitCargo.
	 * Und gibt dabei die Einheiten sowie die Anzahl der gefundenen Einheiten in beiden UnitCargos zurueck.
	 * @param unitcargo Der UnitCargo mit dem verglichen wird
	 * @return Eine HashMap mit den Einheiten und den jeweiligen Anzahlen
	 */
	public HashMap<Integer, Long[]> compare( UnitCargo unitcargo )
	{
		HashMap<Integer, Long[]> unitlist = new HashMap<Integer, Long[]>();
		for(UnitCargoEntry unit : units)
		{
			unitlist.put(unit.getUnitTypeId(), new Long[] {unit.getAmount(), 0l});
		}
		
		for(UnitCargoEntry unit : unitcargo.getUnitArray())
		{
			if(unitlist.containsKey(unit.getUnitTypeId()))
			{
				unitlist.put(unit.getUnitTypeId(), new Long[] {unitlist.get(unit.getUnitTypeId())[0], unit.getAmount()});
			}
			else
			{
				unitlist.put(unit.getUnitTypeId(), new Long[] {0l, unit.getAmount()});
			}
		}
		
		return unitlist;
	}
	
	/**
	 * Gibt die Einheiten im Cargo als Liste zurueck.
	 * @return Die Liste der Einheiten
	 */
	public HashMap<Integer, Long> getUnitList()
	{
		HashMap<Integer, Long> unitlist = new HashMap<Integer, Long>();
		for(UnitCargoEntry unit : units)
		{
			if(unit.getAmount() != 0)
			{
				unitlist.put(unit.getUnitTypeId(), unit.getAmount());
			}
		}
		
		return unitlist;
	}
	
	/**
	 * Diese Methode fuehrt einen Kapervorgang aus. 
	 * Dabei versuchen die Einheiten des aktuellen UnitCargos die Einheiten des uebergebenen UnitCargos zu kapern.
	 * @param kaperunitcargo Der UnitCargo der gekapert werden soll
	 * @param gefalleneeigeneUnits Ein UnitCargo mit den eigenen Einheiten die gefallen sind
	 * @param gefallenefeindlicheUnits Ein UnitCargo mit den Einheiten des Gegners die gefallen sind
	 * @param feindCrew Die Anzahl der feindlichen Crewmitglieder
	 * @param amulti Der Multiplikator vom angreifenden Offizier
	 * @param defmulti Der Multiplikator vom verteidigenden Offizier
	 * @return <code>true</code>, falls der Kapervorgang verfolgreich war.
	 */
	public boolean kapern(UnitCargo kaperunitcargo, UnitCargo gefalleneeigeneUnits, UnitCargo gefallenefeindlicheUnits, Crew feindCrew, int amulti, int defmulti)
	{
		
		if(getKaperValue()*amulti > (kaperunitcargo.getKaperValue()+10*feindCrew.getValue())*defmulti*3  )
		{
			return true;
		}
		if(getKaperValue()*amulti > (kaperunitcargo.getKaperValue()+10*feindCrew.getValue())*defmulti)
		{
			int totekapervalue = (int)Math.ceil((kaperunitcargo.getKaperValue()+10*feindCrew.getValue())*defmulti*1.0/ amulti );
			gefallenefeindlicheUnits.addCargo(kaperunitcargo);
			kaperunitcargo.substractCargo(new UnitCargo(kaperunitcargo));
			feindCrew.setValue(0);
			reduziereKaperValue(totekapervalue, gefalleneeigeneUnits);
			return true;
		}
		int totekapervalue = (int)Math.ceil((getKaperValue())*amulti*1.0/defmulti);
		if(totekapervalue > kaperunitcargo.getKaperValue())
		{
			feindCrew.setValue(feindCrew.getValue() - (totekapervalue - kaperunitcargo.getKaperValue()) / 10);
			totekapervalue = kaperunitcargo.getKaperValue();
		}
		gefalleneeigeneUnits.addCargo(this);
		units.clear();
		kaperunitcargo.reduziereKaperValue(totekapervalue, gefallenefeindlicheUnits);
		return false;
	}
	
	/**
	 * Diese Methode reduziert diesen UnitCargo um die angegebene Menge an KaperValue.
	 * Dabei werden zuerst Einheiten mit hoher KaperValue entfernt.
	 * @param totekapervalue Die KaperValue die entfernt werden soll
	 * @param toteUnits Der UnitCargo der alle abgestorbenen Einheiten enthaelt
	 */
	public void reduziereKaperValue(int totekapervalue, UnitCargo toteUnits)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<UnitType> unitlist = Common.cast(db.createQuery("from UnitType ORDER BY kapervalue DESC").list());
		
		for(UnitType unit : unitlist)
		{
			if(getUnitCount(unit.getId()) > 0)
			{
				long unitcount = getUnitCount(unit.getId());
				
				// Reichen die Einheiten nicht aus um den Bedarf zu decken
				if( unit.getKaperValue() * unitcount < totekapervalue)
				{
					totekapervalue -= unit.getKaperValue() * unitcount;
					toteUnits.addUnit(unit.getId(), unitcount);
					setUnit(unit.getId(),0);
				}
				// Die Einheiten reichen aus
				else
				{
					toteUnits.addUnit(unit.getId(), (long)Math.ceil(totekapervalue / unit.getKaperValue()));
					substractUnit(unit.getId(), (long)Math.ceil(totekapervalue / unit.getKaperValue()));
					totekapervalue = 0;
					return;
				}
			}
		}
	}
	
	/**
	 * Filtert alle Einheiten aus diesem UnitCargo heraus, die groeszer als die angegebene MaximalGroesze sind.
	 * @param maxsize Die Maximale Groesze die im UnitCargo verbleiben soll
	 * @return Ein UnitCargo Objekt mit allen rausgefilterten Einheiten
	 */
	public UnitCargo trimToMaxSize(int maxsize)
	{
		UnitCargo trimedUnits = new UnitCargo();

		if(isEmpty())
		{
			return trimedUnits;
		}
		
		for(UnitCargoEntry unit : this.units)
		{
			if(unit.getUnitType().getSize() > maxsize)
			{
				trimedUnits.addUnit(unit.getUnitType().getId(), unit.getAmount());
				unit.setAmount(0);
			}
		}
		
		return trimedUnits;
	}
	
	/**
	 * Berechnet, wie viele Einheiten von der angegebenen Menge RE noch versorgt werden koennen.
	 * Alle zusaetzlichen Einheiten werden aus dem aktuellen UnitCargo entfernt und als extra UnitCargo zurueckgegeben.
	 * @param restre Die RE die noch zur Versorgung da sind
	 * @return Ein UnitCargo mit den Einheiten die nicht mehr versorgt werden konnten
	 */
	public UnitCargo getMeuterer(int restre)
	{
		UnitCargo meuterer = new UnitCargo();
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		List<UnitType> unitlist = Common.cast(db.createQuery("from UnitType").list());
		
		for(UnitType unit : unitlist)
		{
			if(hasUnit(unit.getId()))
			{
				// Es koennen nicht mehr alle Einheiten versorgt werden
				if(getUnitCount(unit.getId())*unit.getReCost() > restre)
				{
					long numunits = (long)Math.ceil(restre / unit.getReCost());
					substractUnit(unit.getId(), numunits);
					meuterer.addUnit(unit.getId(), numunits);
					restre -= numunits * unit.getReCost();
				}
				else
				{
					restre -= (long)Math.ceil(getUnitCount(unit.getId()) * unit.getReCost());
				}
			}
		}
		
		return meuterer;
	}
}
