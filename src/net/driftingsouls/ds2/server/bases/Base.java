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
package net.driftingsouls.ds2.server.bases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

/**
 * <p>Repraesentiert eine Basis in DS</p>
 * Hinweis: Das setzen von Werten aktuallisiert nicht die Datenbank!
 * 
 * @author Christopher Jung
 */
public class Base implements Cloneable {
	private SQLResultRow base;
	private List<AutoGTUAction> autogtuacts;
	private Cargo cargo;
	private Integer[] terrain;
	private Integer[] bebauung;
	private Integer[] active;
	
	/**
	 * Erstellt eine neue Instanz einer Basis
	 * @param base Eine SQL-Ergebniszeile mit den Daten der Basis
	 */
	public Base(SQLResultRow base) {
		this.base = base;
		
		this.terrain = Common.explodeToInteger("|",base.getString("terrain"));
		this.bebauung = Common.explodeToInteger("|",base.getString("bebauung"));
		this.active = Common.explodeToInteger("|",base.getString("active"));
		this.cargo = new Cargo( Cargo.Type.STRING, base.getString("cargo"));
		
		String[] autogtuacts = StringUtils.split(base.getString("autogtuacts"),";");
		List<AutoGTUAction> acts = new ArrayList<AutoGTUAction>();
		for( int i=0; i < autogtuacts.length; i++ ) {
			String[] split = StringUtils.split(autogtuacts[i],":");
			
			acts.add(new AutoGTUAction(Resources.fromString(split[0]), Integer.parseInt(split[1]), Long.parseLong(split[2])) );
		}
		this.autogtuacts = acts;
		
		// Ggf die Feldergroessen fixen
		if( getTerrain().length < getWidth()*getHeight() ) {
			Database db = ContextMap.getContext().getDatabase();
			
			Integer[] terrain = new Integer[getWidth()*getHeight()];
			System.arraycopy(getTerrain(), 0, terrain, 0, getTerrain().length );
			for( int i=Math.max(getTerrain().length-1,0); i < getWidth()*getHeight(); i++ ) {
				int rnd = RandomUtils.nextInt(7);
				if( rnd > 4 ) {
					terrain[i] = rnd - 4;	
				}
				else {
					terrain[i] = 0;	
				}
			}
			this.terrain = terrain;
			db.update("UPDATE bases SET terrain='",Common.implode("|", getTerrain()),"' WHERE id='",getID(),"'");
		}
			
		if( getBebauung().length < getWidth()*getHeight() ) {
			Database db = ContextMap.getContext().getDatabase();
			
			Integer[] bebauung = new Integer[getWidth()*getHeight()];
			System.arraycopy(getBebauung(), 0, bebauung, 0, getBebauung().length );
			for( int i=Math.max(getBebauung().length-1,0); i < getWidth()*getHeight(); i++ ) {
				bebauung[i] = 0;	
			}
			this.bebauung = bebauung;
			db.update("UPDATE bases SET bebauung='",Common.implode("|", getBebauung()),"' WHERE id='",getID(),"'");
		}
		
		if( getActive().length < getWidth()*getHeight() ) {
			Database db = ContextMap.getContext().getDatabase();
			
			Integer[] active = new Integer[getWidth()*getHeight()];
			System.arraycopy(getActive(), 0, active, 0, getActive().length );
			for( int i=Math.max(getActive().length-1,0); i < getWidth()*getHeight(); i++ ) {
				active[i] = 0;	
			}
			this.active = active;
			db.update("UPDATE bases SET active='",Common.implode("|", getActive()),"' WHERE id='",getID(),"'");
		}
	}
	
	/**
	 * Gibt die ID der Basis zurueck
	 * @return die ID der Basis
	 */
	public int getID() {
		return base.getInt("id");
	}
	
	/**
	 * Gibt die Breite der Bauflaeche auf der Basis in Feldern zurueck
	 * @return Die Breite
	 */
	public int getWidth() {
		return base.getInt("width");
	}
	
	/**
	 * Gibt die Hoehe der Bauflaeche auf der Basis in Feldern zurueck
	 * @return Die Hoehe
	 */
	public int getHeight() {
		return base.getInt("height");
	}
	
	/**
	 * Gibt den Namen der Basis zurueck
	 * @return Der Name
	 */
	public String getName() {
		return base.getString("name");
	}
	
	/**
	 * Gibt der Basis einen neuen Namen
	 * @param name Der neue Name
	 */
	public void setName(String name) {
		base.put("name", name);
	}
	
	/**
	 * Gibt die ID des Besitzers zurueck.
	 * Falls die Basis niemandem gehoert, wird 0 zurueckgegegeben.
	 * 
	 * @return Die ID des Besitzers oder 0
	 */
	public int getOwner() {
		return base.getInt("owner");
	}
	
	/**
	 * Setzt einen neuen Besitzer fuer die Basis
	 * @param owner Die ID des neuen Besitzers
	 */
	public void setOwner(int owner) {
		base.put("owner", owner);
	}
	
	/**
	 * Gibt die Terrain-Typen der einzelnen Basisfelder zurueck.
	 * @return Die Terraintypen der Felder
	 */
	public Integer[] getTerrain() {
		return this.terrain;
	}
	
	/**
	 * Gibt die IDs der auf den einzelnen Feldern stehenden Gebaeude zurueck.
	 * Sollte auf einem Feld kein Gebaeude stehen ist die ID 0.
	 * 
	 * @return Die IDs der Gebaeude
	 */
	public Integer[] getBebauung() {
		return this.bebauung;
	}
	
	/**
	 * Setzt die neue Bebauung der Basis
	 * @param bebauung Die neue Bebauung
	 */
	public void setBebauung(Integer[] bebauung) {
		this.bebauung = bebauung;
	}
	
	/**
	 * Gibt an, auf welchen Feldern die Gebaeude aktiv (1) sind und auf welchen
	 * nicht (0)
	 * @return Aktivierungsgrad der Gebaeude auf den Feldern
	 */
	public Integer[] getActive() {
		return this.active;
	}
	
	/**
	 * Setzt den Aktivierungszustand aller Gebaeude. <code>1</code> bedeutet, dass das
	 * Gebaeude aktiv ist. <code>0</code>, dass das Gebaeude nicht aktiv ist.
	 * @param active Der neue Aktivierungszustand aller Gebaeude
	 */
	public void setActive(Integer[] active) {
		this.active = active;
	}
	
	/**
	 * Gibt den Cargo der Basis zurueck
	 * @return Der Cargo
	 */
	public Cargo getCargo() {
		return cargo;
	}
	
	/**
	 * Setzt den Cargo des Basisobjekts
	 * @param cargo Der neue Cargo
	 */
	public void setCargo(Cargo cargo) {
		this.cargo = cargo;
	}

	/**
	 * Gibt die ID der installierten Core der Basis zurueck.
	 * Falls es keine Core auf der Basis gibt, so wird 0 zurueckgegeben.
	 * 
	 * @return Die ID der Core oder 0
	 */
	public int getCore() {
		return base.getInt("core");
	}
	
	/**
	 * Setzt den neuen Kern der Basis. <code>0</code> bedeutet,
	 * dass kein Kern vorhanden ist.
	 * 
	 * @param core der neue Kern oder <code>0</code>
	 */
	public void setCore(int core) {
		base.put("core", core);
	}

	/**
	 * Gibt die X-Koordinate der Basis zurueck
	 * @return Die X-Koordinate
	 */
	public int getX() {
		return base.getInt("x");
	}

	/**
	 * Gibt die Y-Koordinate der Basis zurueck
	 * @return Die Y-Koordinate
	 */
	public int getY() {
		return base.getInt("y");
	}

	/**
	 * Gibt die ID des Systems zurueck, in dem sich die Basis befindet
	 * @return Die ID des Systems
	 */
	public int getSystem() {
		return base.getInt("system");
	}

	/**
	 * Gibt zurueck, ob die Core aktiv ist.
	 * @return <code>true</code>, falls die Core aktiv ist
	 */
	public boolean isCoreActive() {
		return base.getInt("coreactive") != 0;
	}
	
	/**
	 * Setzt den Aktivierungszustand der Core
	 * @param active <code>true</code>, wenn die Core aktiv ist
	 */
	public void setCoreActive(boolean active) {
		base.put("coreactive", active ? 1 : 0);
	}

	/**
	 * Gibt die maximale Masse an Cargo zurueck, die auf der Basis gelagert werden kann
	 * @return Der Max-Cargo
	 */
	public long getMaxCargo() {
		return base.getLong("maxcargo");
	}
	
	/**
	 * Setzt den neuen maximalen Cargo der Basis
	 * @param cargo Der neue maximale Cargo
	 */
	public void setMaxCargo(long cargo) {
		base.put("maxcargo", cargo);
	}

	/**
	 * Gibt die Anzahl der Bewohner auf der Basis zurueck
	 * @return Die Bewohner
	 */
	public int getBewohner() {
		return base.getInt("bewohner");
	}
	
	/**
	 * Setzt die Anzahl der Bewohner auf der Basis
	 * @param bewohner Die neue Anzahl der Bewohner
	 */
	public void setBewohner(int bewohner) {
		base.put("bewohner", bewohner);
	}
	
	/**
	 * Gibt die Anzahl der Arbeiter auf der Basis zurueck
	 * @return Die Arbeiter
	 */
	public int getArbeiter() {
		return base.getInt("arbeiter");
	}
	
	/**
	 * Setzt die neue Menge der Arbeiter auf der Basis. 
	 * @param arbeiter Die Anzahl der Arbeiter
	 */
	public void setArbeiter(int arbeiter) {
		base.put("arbeiter", arbeiter);
	}

	/**
	 * Gibt die vorhandene Energiemenge auf der Basis zurueck
	 * @return Die Energiemenge
	 */
	public int getE() {
		return base.getInt("e");
	}
	
	/**
	 * Setzt die Menge der auf der Basis vorhandenen Energie
	 * @param e Die auf der Basis vorhandene Energie
	 */
	public void setE(int e) {
		base.put("e", e);
	}
	
	/**
	 * Gibt die maximal auf der Basis speicherbare Energiemenge zurueck
	 * @return die max. Energiemenge
	 */
	public int getMaxE() {
		return base.getInt("maxe");
	}
	
	/**
	 * Setzt die maximale Menge an Energie die auf der Basis gespeichert werden kann
	 * @param maxe Die maximale Menge an Energie
	 */
	public void setMaxE(int maxe) {
		base.put("maxe", maxe);
	}
	
	/**
	 * Gibt die Klassennummer der Basis zurueck (= Der Astityp)
	 * @return Die Klassennummer
	 */
	public int getKlasse() {
		return base.getInt("klasse");
	}
	
	/**
	 * Gibt die Anzahl an Feldern zurueck, die in die Gesamtflaechenanzahl eingerechnet werden
	 * duerfen. Entspricht nicht immer der tatsaechlichen Anzahl an Feldern
	 * @return Die verrechenbare Anzahl an Feldern
	 */
	public int getMaxTiles() {
		return base.getInt("maxtiles");
	}
	
	/**
	 * Setzt die Anzahl an Feldern, die in die Gesamtflaechenanzahl eingerechnet werden sollen
	 * @param tiles Die Felderanzahl
	 */
	public void setMaxTiles(int tiles) {
		base.put("maxtiles", tiles);
	}
	
	/**
	 * Gibt den Radius der Basis auf der Sternenkarte zurueck.
	 * 0 bedeutet in diesem Zusammenhang, dass die Basis keine Ausdehung
	 * in benachbarte Felder hat (Normalfall)
	 * @return Der Radius
	 */
	public int getSize() {
		return base.getInt("size");
	}
	
	/**
	 * Gibt die Liste der automatischen GTU-Verkaufsaktionen zurueck
	 * @return Die Liste der GTU-Verkaufsaktionen beim Tick
	 */
	public List<AutoGTUAction> getAutoGTUActs() {
		return autogtuacts;
	}
	
	/**
	 * Setzt die Liste der automatischen GTU-Verkaufsaktionen
	 * @param list Die neue Liste
	 */
	public void setAutoGTUActs(List<AutoGTUAction> list) {
		this.autogtuacts = list;
	}
	
	/**
	 * Generiert den aktuellen Verbrauch/Produktion-Status einer Basis
	 * @param context Der aktuelle Kontext
	 * @param base die ID der Basis
	 * @return der aktuelle Verbrauchs/Produktions-Status
	 */
	public static BaseStatus getStatus( Context context, int base ) {
		Database db = context.getDatabase();
		
		SQLResultRow baseRow = db.first("SELECT * FROM bases WHERE id='"+base+"'");	

		return getStatus( context, new Base(baseRow) );
	}
		
	/**
	 * Generiert den aktuellen Verbrauch/Produktion-Status einer Basis
	 * @param context Der aktuelle Kontext
	 * @param base die Basis
	 * @return der aktuelle Verbrauchs/Produktions-Status
	 */
	public static BaseStatus getStatus( Context context, Base base ) {
		Cargo stat = new Cargo();
		int e = 0;
		int arbeiter = 0;
		int bewohner = 0;
		Map<Integer,Integer> buildinglocs = new TreeMap<Integer,Integer>(); 
	
		if( (base.getCore() > 0) && base.isCoreActive() ) {
			Core core = Core.getCore(context.getDatabase(), base.getCore() );
	
			stat.substractCargo(core.getConsumes());
			stat.addCargo(core.getProduces());
			
			e = e - core.getEVerbrauch() + core.getEProduktion();
			arbeiter += core.getArbeiter();
			bewohner += core.getBewohner();
		}
		
		Integer[] bebauung = base.getBebauung();
		Integer[] bebon = base.getActive();
			
		for( int o=0; o < base.getWidth() * base.getHeight(); o++ ) {
			if( bebauung[o] == 0 ) {
				continue;
			} 
			
			Building building = Building.getBuilding(context.getDatabase(), bebauung[o]);
	
			if( !buildinglocs.containsKey(building.getID()) ) {
				buildinglocs.put(building.getID(), o);
			}
				
			bebon[o] = building.isActive( base, bebon[o], o ) ? 1 : 0;
		
			if( bebon[o] == 0 ) {
				continue;
			}
			
			building.modifyStats( base, stat );
		
			stat.substractCargo(building.getConsumes());
			stat.addCargo(building.getProduces());
			
			e = e - building.getEVerbrauch() + building.getEProduktion();
			arbeiter += building.getArbeiter();
			bewohner += building.getBewohner();
		}
	
		stat.substractResource( Resources.NAHRUNG, base.getBewohner() );

		return new BaseStatus(stat, e, bewohner, arbeiter, Collections.unmodifiableMap(buildinglocs), bebon);
	}

	@Override
	public Object clone() {
		Base base;
		try {
			base = (Base)super.clone();
			base.base = new SQLResultRow();
			base.base.putAll(this.base);
			base.terrain = this.getTerrain().clone();
			base.active = this.getActive().clone();
			base.bebauung = this.getBebauung().clone();
			base.cargo = (Cargo)this.getCargo().clone();
			
			base.autogtuacts = new ArrayList<AutoGTUAction>();
			for( int i=0; i < this.autogtuacts.size(); i++ ) {
				base.autogtuacts.add((AutoGTUAction)this.autogtuacts.get(i).clone());
			}
		
			return base;
		} catch (CloneNotSupportedException e) {
			// EMPTY
		}
		return null;
	}
}
