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
package net.driftingsouls.ds2.server.ships;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.framework.CacheMap;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.ThreadLocalMessage;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;

/**
 * Diverse Funktionen rund um Schiffe in DS
 * TODO: Ja, ich weiss, das ist nicht besonders schoen. Besser waeren richtige Schiffsobjekte...
 * @author Christopher Jung
 *
 */
public class Ships {
	private static final int MANGEL_TICKS = 9;
	
	/**
	 * Repraesentiert ein in ein Schiff eingebautes Modul (oder vielmehr die Daten, 
	 * die hinterher verwendet werden um daraus ein Modul zu rekonstruieren)
	 */
	public static class ModuleEntry {
		/**
		 * Der Slot in den das Modul eingebaut ist
		 */
		public final int slot;
		/**
		 * Der Modultyp
		 * @see net.driftingsouls.ds2.server.cargo.modules.Module
		 */
		public final int moduleType;
		/**
		 * Weitere Modultyp-spezifische Daten
		 */
		public final String data;
		
		protected ModuleEntry(int slot, int moduleType, String data) {
			this.slot = slot;
			this.moduleType = moduleType;
			this.data = data;
		}
		
		@Override
		public String toString() {
			return "ModuleEntry: "+slot+":"+moduleType+":"+data;
		}
	}
	
	/**
	 * Objekt mit Funktionsmeldungen
	 */
	public static final ThreadLocalMessage MESSAGE = new ThreadLocalMessage();
	
	private static Database db = new Database();
	
	private static Map<Integer,SQLResultRow> shiptypes = new HashMap<Integer,SQLResultRow>();
	private static Map<Location,Integer> nebel = Collections.synchronizedMap(new CacheMap<Location,Integer>(1000));
	
	public static final String SF_JAEGER = "jaeger";
	public static final String SF_ZERSTOERERPANZERUNG = "zerstoererpanzerung";
	public static final String SF_COLONIZER = "colonizer";
	public static final String SF_ABFANGEN = "abfangen";
	public static final String SF_NICHT_KAPERBAR = "nicht_kaperbar";
	public static final String SF_INSTABIL = "instabil";
	public static final String SF_SEHR_KLEIN = "sehr_klein";
	public static final String SF_KEIN_TRANSFER = "kein_transfer";
	public static final String SF_SRS_AWAC = "srs_awac";
	public static final String SF_SRS_EXT_AWAC = "srs_ext_awac";
	public static final String SF_JUMPDRIVE_SHIVAN = "jumpdrive_shivan";
	public static final String SF_INSTANT_BATTLE_ENTER = "instant_battle_enter";
	public static final String SF_NICHT_PLUENDERBAR = "nicht_pluenderbar";
	public static final String SF_GOD_MODE = "god_mode";
	public static final String SF_DROHNEN_CONTROLLER = "drohnen_controller";
	public static final String SF_DROHNE = "drohne";
	public static final String SF_SECONDROW = "secondrow";
	public static final String SF_OFFITRANSPORT = "offitransport";
	
	public static String[] getShipTypeFlagList(int shiptypeID) {
		SQLResultRow shiptype = getShipType(shiptypeID, false);
		
		return StringUtils.split( shiptype.getString("flags"), ' ');
	}
	
	/**
	 * Testet, ob ein Schiffstyp ein bestimmtes Schiffstypen-Flag (SF_*) hat
	 * @param shiptype Schiffstypenarray
	 * @param flag Das Schiffstypen-Flag (SF_*)
	 * 
	 * @return true, wenn der Schiffstyp das Flag besitzt
	 */
	public static boolean hasShipTypeFlag(SQLResultRow shiptype, String flag) {
		if( shiptype.getString("flags").indexOf(flag) > -1 ) {
			return true;
		}
		return false;
	}
	
	private static final Map<String,String> shipTypeFlagNames = new HashMap<String,String>();
	private static final Map<String,String> shipTypeFlagDescs = new HashMap<String,String>();
	static {
		shipTypeFlagNames.put(SF_COLONIZER, "Colonizer");
		shipTypeFlagNames.put(SF_ZERSTOERERPANZERUNG, "Zerst&ouml;rerpanzerung");
		shipTypeFlagNames.put(SF_NICHT_KAPERBAR, "Nicht Kaperbar");
		shipTypeFlagNames.put(SF_KEIN_TRANSFER, "Kein Transfer m&ouml;glich");
		shipTypeFlagNames.put(SF_INSTABIL, "Instabil");
		shipTypeFlagNames.put(SF_JAEGER, "J&auml;ger");
		shipTypeFlagNames.put(SF_ABFANGEN, "Abfangen");
		shipTypeFlagNames.put(SF_SEHR_KLEIN, "Sehr Klein");
		shipTypeFlagNames.put(SF_SRS_AWAC, "Awac SRS");
		shipTypeFlagNames.put(SF_SRS_EXT_AWAC, "Erweiterte Awac SRS");
		shipTypeFlagNames.put(SF_JUMPDRIVE_SHIVAN, "Shivanischer Sprungantrieb");
		shipTypeFlagNames.put(SF_INSTANT_BATTLE_ENTER, "Schnelle Kampfbereitschaft");
		shipTypeFlagNames.put(SF_NICHT_PLUENDERBAR, "Nicht Pl&uuml;nderbar");
		shipTypeFlagNames.put(SF_GOD_MODE, "Nicht Zerst&ouml;rbar (God Mode)");
		shipTypeFlagNames.put(SF_DROHNEN_CONTROLLER, "Drohnen-Kontrollschiff");
		shipTypeFlagNames.put(SF_DROHNE, "Drohne");
		shipTypeFlagNames.put(SF_SECONDROW, "Zweite Reihe");
		shipTypeFlagNames.put(SF_OFFITRANSPORT, "Offizierstransporter");
		
		shipTypeFlagDescs.put(SF_COLONIZER, "Die Eigenschaft Colonzier erm&ouml;glicht es umbewohnte Asteroiden zu kolonisieren");
		shipTypeFlagDescs.put(SF_ZERSTOERERPANZERUNG, "Die Zerst&ouml;rerpanzerung sorgt daf&uuml;r, dass pro Kampfrunde maximal 33% Schaden gemessen an an der maximalen H&uuml;llenst&auml;rke dem Schiff zugef&uuml;gt werden kann");
		shipTypeFlagDescs.put(SF_NICHT_KAPERBAR, "Dieses Schiff kann nicht gekapert wohl aber gepl&uuml;ndert werden");
		shipTypeFlagDescs.put(SF_KEIN_TRANSFER, "Es k&ouml;nnen keine Waren zu oder von dem Objekt transferiert werden, da der Laderaum versiegelt ist. Dies gilt auch f&uuml;r Pl&uuml;nderungen");
		shipTypeFlagDescs.put(SF_INSTABIL, "Ein Objekt mit der Eigenschaft Instabil zerf&auml;llt, sobald es gepl&uuml;ndert wurde");
		shipTypeFlagDescs.put(SF_JAEGER, "Die J&auml;gereigenschaft erm&ouml;glicht es diesem Schiff auf Tr&auml;gern zu landen");
		shipTypeFlagDescs.put(SF_ABFANGEN, "Schiffe mit der Eigenschaft Abfangen k&ouml;nnen im Kampf auf fl&uuml;uchtende Schiffe feuern - gegen leicht erh&ouml;hte AP-Kosten");
		shipTypeFlagDescs.put(SF_SEHR_KLEIN, "Ein Objekt mit der Eigenschaft Sehr Klein kann auf Grund seiner Gr&ouml&szlig;e nicht auf den Langstreckensensoren geortet werden");
		shipTypeFlagDescs.put(SF_SRS_AWAC, "Mit Awac-Kurzstreckensensoren kann das Schiff die Antriebs&uuml;berhitzung anderer Schiffe im Sektor analysieren");
		shipTypeFlagDescs.put(SF_SRS_EXT_AWAC, "Mit den Verbesserten Awac-Kurzstreckensensoren ist eine detailierte Analyse von Antriebs&uuml;berhitzung, Crew und Enegiereserven eines anderen Schiffes im selben Sektor m&ouml;glich");
		shipTypeFlagDescs.put(SF_JUMPDRIVE_SHIVAN, "Dieses Schiff verf&uuml;gt &uuml;ber einen Shivanischen Sprungantrieb");
		shipTypeFlagDescs.put(SF_INSTANT_BATTLE_ENTER, "Dieses Schiff ist in der Runde in der es einer Schlacht beitritt bereits einsatzbereit");
		shipTypeFlagDescs.put(SF_NICHT_PLUENDERBAR, "Sie k&ouml;nnen keine Waren von diesem Schiff pl&uuml;ndern");
		shipTypeFlagDescs.put(SF_GOD_MODE, "Dieses Schiff kann nicht zerst&ouml;rt werden");
		shipTypeFlagDescs.put(SF_DROHNEN_CONTROLLER, "Dieses Schiff kann Drohnen kontrollieren");
		shipTypeFlagDescs.put(SF_DROHNE, "Dieses Schiff ist eine Drohne. Es ben&ouml;tigt ein Drohnen-Kontrollschiff um funktionieren zu k&ouml;nnen");
		shipTypeFlagDescs.put(SF_SECONDROW, "Dieses Schiff kann in einem Kampf in die zweite Reihe wechseln, wo es vor Angriffen des Gegners sicherer ist");
		shipTypeFlagDescs.put(SF_OFFITRANSPORT, "Dieses Schiff kann Offiziere in der H&ouml;he seiner max. Crew transportieren");
	}
	
	/**
	 * Gibt den Namen eines Schiffsflags zurueck. Der Name ist lesbar und kann
	 * auch direkt in der UI angezeigt werden.
	 * @param flag Das Schiffsflag
	 * @return Der Name
	 */
	public static String getShipTypeFlagName( String flag ) {
		if( !shipTypeFlagNames.containsKey(flag) ) {
			return "Flag "+flag+" unbekannt";
		}
		return shipTypeFlagNames.get(flag);
	}
	
	/**
	 * Gibt die Beschreibung zu einem Schiffsflag zurueck
	 * @param flag Das Schiffsflag
	 * @return die Beschreibung
	 */
	public static String getShipTypeFlagDescription( String flag ) {
		if( !shipTypeFlagNames.containsKey(flag) ) {
			return "";
		}
		return shipTypeFlagNames.get(flag);
	}
	
	public static ShipClasses getShipClass(int classid) {
		return ShipClasses.values()[classid];
	}
	
	public static void clearShipCache() {
		// TODO
		Common.stub();
	}
	
	private static String buildShipPicturePath( SQLResultRow type, boolean forceServer ) {
		String picture = type.getString("picture");
		Context context = ContextMap.getContext();
		
		if( (context != null) && !forceServer && !type.getBoolean("hide") && (context.getActiveUser() != null) ) {
			picture = context.getActiveUser().getImagePath()+picture;	
		}
		else {
			picture = User.getDefaultImagePath(db)+picture;
		}
		
		return picture;
	}
	
	/**
	 * Gibt die Typen-Daten des angegebenen Schiffs bzw Schifftyps zurueck 
	 * @param shiptype Die ID des Schiffs bzw des Schifftyps
	 * @param isShip Handelt es sich um ein Schiff (<code>true</code>)?
	 * @return die Typen-Daten
	 */
	public static SQLResultRow getShipType( int shiptype, boolean isShip ) {
		return getShipType(shiptype, isShip, false);
	}
	
	/**
	 * Gibt die Typen-Daten des angegebenen Schiffs zurueck 
	 * @param shipdata Eine SQL-Ergebniszeile mit den daten des Schiffes
	 * @return die Typen-Daten
	 */
	public static SQLResultRow getShipType( SQLResultRow shipdata ) {
		return getShipType(shipdata, false);
	}
	
	private static PreparedQuery pqGetModuleRow = db.prepare("SELECT nickname,picture,ru,rd,ra,rm,eps,cost,hull,panzerung,cargo,heat,crew,weapons,maxheat,torpedodef,shields,size,jdocks,adocks,sensorrange,hydro,deutfactor,recost,flags,werft,ow_werft FROM ships_modules WHERE id>0 AND id= ? ");
	
	/**
	 * Gibt die Typen-Daten des angegebenen Schiffs zurueck 
	 * @param shipdata Eine SQL-Ergebniszeile mit den daten des Schiffes
	 * @param plaindata Sollen die Bildpfade angepasst werden (<code>false</code>) oder so zurueckgegeben werden,
	 * wie sie in der DB stehen (<code>true</code>)?
	 * @return die Typen-Daten
	 */
	public static SQLResultRow getShipType( SQLResultRow shipdata, boolean plaindata ) {
		int shiptype = shipdata.getInt("type");
		if( shipdata.getString("status").indexOf("tblmodules") != -1 ) {
			shipdata = pqGetModuleRow.pfirst(shiptype);
		}
		else {
			shipdata = null;
		}
		
		return getShipType(shiptype, shipdata, plaindata);
	}
	
	private static PreparedQuery pqGetShipInfos = db.prepare("SELECT type,status FROM ships WHERE id>0 AND id= ?");
	
	private static SQLResultRow getShipType( int shiptype, boolean isShip, boolean plaindata ) {
		if( isShip ) {
			// TODO: Cache!
			SQLResultRow shipdata = pqGetShipInfos.pfirst(shiptype);
			shiptype = shipdata.getInt("type");
			
			if( shipdata.getString("status").indexOf("tblmodules") != -1 ) {
				shipdata = pqGetModuleRow.first(shiptype);
			}
			else {
				shipdata = null;
			}
			
			return getShipType(shiptype, shipdata, plaindata);
		}
		return getShipType(shiptype, null, plaindata);
	}
	
	private static PreparedQuery pqGetShipType = db.prepare("SELECT *,LOCATE('=',weapons) as military FROM ship_types WHERE id= ? ");
	
	private static SQLResultRow getShipType( int shiptype, SQLResultRow shipdata, boolean plaindata ) {
		synchronized (shiptypes) {
			if( !shiptypes.containsKey(shiptype) ) {
				shiptypes.put(shiptype, pqGetShipType.pfirst(shiptype));
			}
		}
		
		SQLResultRow type = (SQLResultRow)shiptypes.get(shiptype).clone();
		if( shipdata != null ) {
			for( String key : shipdata.keySet() ) {
				if( !"".equals(shipdata.get(key)) ) {
					type.put(key, shipdata.get(key));
				}
			}
		}
		
		if( !plaindata ) {
			String picture = "";
			if( (shipdata == null) || !shipdata.containsKey("picture") ) {
				picture = shiptypes.get(shiptype).getString("picture");
			}
	
			type.put("picture", buildShipPicturePath(type, (!shiptypes.get(shiptype).getString("picture").equals(picture) ? true : false) ));
		}
		
		return type;
	}
	
	public static void cacheShipTypes(int[] shiptypelist) {
		// TODO
		Common.stub();
	}
	
	/**
	 * Berechnet das Status-Feld des Schiffes neu. Diese Aktion sollte nach jeder
	 * Operation angewendet werden, die das Schiff in irgendeiner Weise veraendert.
	 * @param shipID die ID des Schiffes
	 * @return der neue Status-String
	 */
	public static String recalculateShipStatus(int shipID) {
		Database db = ContextMap.getContext().getDatabase();

		SQLResultRow ship = db.first("SELECT id,type,crew,status,cargo,owner,alarm,system,x,y FROM ships WHERE id>0 AND id='",shipID,"'");
		
		SQLResultRow type = getShipType(ship);
		
		Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
		
		List<String> status = new ArrayList<String>();
		
		// Alten Status lesen und ggf Elemente uebernehmen
		String[] oldstatus = StringUtils.split(ship.getString("status"), ' ');
		
		if( oldstatus.length > 0 ) {
			for( int i=0; i < oldstatus.length; i++ ) {
				String astatus = oldstatus[i];
				if( !astatus.equals("disable_iff") && !astatus.equals("mangle_nahrung") && !astatus.equals("mangel_reaktor") && !astatus.equals("offizier") && !astatus.equals("nocrew") && !astatus.equals("nebel") ) {
					status.add(astatus);
				}
			}
		}
		
		// Treibstoffverbrauch bereichnen
		if( type.getInt("rm") > 0 ) {
			long ep = cargo.getResourceCount( Resources.URAN ) * type.getInt("ru") + cargo.getResourceCount( Resources.DEUTERIUM ) * type.getInt("rd") + cargo.getResourceCount( Resources.ANTIMATERIE ) * type.getInt("ra");
			long er = ep/type.getInt("rm");
			
			int turns = 2;
			if( (ship.getInt("alarm") == 1) && (type.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) ) {
				turns = 4;	
			}
			
			if( er <= MANGEL_TICKS/turns ) {
				status.add("mangel_reaktor");
			}
		}
		
		// Ist Crew an Bord?
		if( (type.getInt("crew") != 0) && (ship.getInt("crew") == 0) ) {
			status.add("nocrew");	
		}
	
		// Die Items nach IFF und Hydros durchsuchen
		boolean disableIFF = false;
	
		if( cargo.getItemWithEffect(ItemEffect.Type.DISABLE_IFF) != null ) {
			disableIFF = true;
		}
		
		if( disableIFF ) {
			status.add("disable_iff");
		}
		
		Cargo usercargo = new Cargo( Cargo.Type.STRING, db.first("SELECT cargo FROM users WHERE id='"+ship.getInt("owner")+"'").getString("cargo"));
		
		// Den Nahrungsverbrauch berechnen
		if( ship.getInt("crew") > 0 ) {
			double scale = 1;
			if( (ship.getInt("alarm") == 1) && (type.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) ) {
				scale = 0.9;	
			}
			
			int nn = (int)Math.ceil(ship.getInt("crew")/scale) - type.getInt("hydro");
			if( (nn > 0) || ((nn == 0) && (type.getInt("hydro") == 0)) ) {
				if( nn == 0 ) nn = 1;
				long nr = usercargo.getResourceCount( Resources.NAHRUNG )/nn;
				
				if( nr <= MANGEL_TICKS ) {
					status.add("mangel_nahrung");
				}
			}
		}
		
		// Ist ein Offizier an Bord?
		Offizier offi = Offizier.getOffizierByDest('s', shipID);
		if( offi != null ) {
			status.add("offizier");
		}
		
		boolean savestatus = true;
		
		String statusString = Common.implode(" ", status);
		if( ship.getString("status").equals(statusString) ) {
			savestatus = false;
		}
	
		if( savestatus ) {
			db.tUpdate(1, "UPDATE ships SET status='"+statusString+"' WHERE id>0 AND id='",shipID,"' AND status='",ship.getString("status")+"'");
		}
		
		return statusString;
	}
	
	/**
	 * Gibt die Moduleintraege eines Schiffes zurueck
	 * @param ship Eine SQL-Ergebniszeile mit den Daten des Schiffes
	 * @return Eine Liste von Moduleintraegen
	 */
	public static ModuleEntry[] getModules( SQLResultRow ship ) {
		Database db = ContextMap.getContext().getDatabase();
		
		List<ModuleEntry> result = new ArrayList<ModuleEntry>();
		
		if( ship.getString("status").indexOf("tblmodules") == -1 ) {
			return new ModuleEntry[0];
		}
		SQLResultRow moduletbl = db.first("SELECT * FROM ships_modules WHERE id='",ship.getInt("id"),"'");	
		
		if( moduletbl.getString("modules").length() != 0 ) {
			String[] modulelist = StringUtils.split(moduletbl.getString("modules"), ';');
			if( modulelist.length != 0 ) {
				for( int i=0; i < modulelist.length; i++ ) {
					String[] module = StringUtils.split(modulelist[i], ':');
					result.add(new ModuleEntry(Integer.parseInt(module[0]), Integer.parseInt(module[1]), module[2]));	
				}
			}
		}
		
		return result.toArray(new ModuleEntry[result.size()]);
	}
	
	/**
	 * Fuegt ein Modul in ein Schiff ein
	 * @param ship Das Schiff
	 * @param slot Der Slot, in den das Modul eingebaut werden soll
	 * @param moduleid Die Typen-ID des Modultyps
	 * @param data Weitere Daten, welche das Modul identifizieren
	 */
	public static void addModule( SQLResultRow ship, int slot, int moduleid, String data ) {
		Database db = ContextMap.getContext().getDatabase();

		if( ship.getString("status").indexOf("tblmodules") == -1 ) {
			db.update("INSERT INTO ships_modules (id) VALUES ('",ship.getInt("id"),"')");
			if( ship.getString("status").length() != 0 ) {
				ship.put("status", ship.getString("status")+" tblmodules");	
			}	
			else {
				ship.put("status", "tblmodules");	
			}
			db.update("UPDATE ships SET status='",ship.getString("status"),"' WHERE id>0 AND id='",ship.getInt("id"),"'");
		}
		String oldModuleTbl = db.first("SELECT modules FROM ships_modules WHERE id='",ship.getInt("id"),"'").getString("modules");	
		List<ModuleEntry> moduletbl = new ArrayList<ModuleEntry>();
		moduletbl.addAll(Arrays.asList(getModules(ship)));
				
		//check modules
		
		//rebuild
		moduletbl.add(new ModuleEntry(slot, moduleid, data ));
		
		SQLResultRow type = getShipType( ship.getInt("type"), false, true );
		SQLResultRow basetype = new SQLResultRow();
		basetype.putAll(type);
		
		Map<Integer,String[]>slotlist = new HashMap<Integer,String[]>();
		String[] tmpslotlist = StringUtils.split(type.getString("modules"), ';');
		for( int i=0; i < tmpslotlist.length; i++ ) {
			String[] aslot = StringUtils.split(tmpslotlist[i], ':');
			slotlist.put(Integer.parseInt(aslot[0]), aslot);
		}
		
		List<Module> moduleobjlist = new ArrayList<Module>();
		List<String> moduleSlotData = new ArrayList<String>(); 
		
		for( int i=0; i < moduletbl.size(); i++ ) {
			ModuleEntry module = moduletbl.get(i);
			if( module.moduleType != 0 ) {
				Module moduleobj = Modules.getShipModule( module );
				if( module.slot > 0 ) {
					moduleobj.setSlotData(slotlist.get(module.slot)[2]);
				}
				moduleobjlist.add(moduleobj);
			
				moduleSlotData.set(i, module.slot+":"+module.moduleType+":"+module.data);
			}
		}
		
		for( int i=0; i < moduleobjlist.size(); i++ ) {
			type = moduleobjlist.get(i).modifyStats( type, basetype, moduleobjlist );		
		}
		
		String modulelist = Common.implode(";",moduleSlotData);
	
		db.tUpdate(1,"UPDATE ships_modules ",
				"SET modules='",modulelist,"'," ,
				"nickname='",type.getString("nickname"),"'," ,
				"picture='",type.getString("picture"),"',",
				"ru='",type.getInt("ru"),"'," ,
				"rd='",type.getInt("rd"),"'," ,
				"ra='",type.getInt("ra"),"'," ,
				"rm='",type.getInt("rm"),"'," ,
				"eps='",type.getInt("eps"),"'," ,
				"cost='",type.getInt("cost"),"'," ,
				"hull='",type.getInt("hull"),"'," ,
				"panzerung='",type.getInt("panzerung"),"'," ,
				"cargo='",type.getLong("cargo"),"'," ,
				"heat='",type.getInt("heat"),"'," ,
				"crew='",type.getInt("crew"),"'," ,
				"weapons='",type.getString("weapons"),"'," ,
				"maxheat='",type.getString("maxheat"),"'," ,
				"torpedodef='",type.getInt("torpedodef"),"'," ,
				"shields='",type.getInt("shields"),"'," ,
				"size='",type.getInt("size"),"'," ,
				"jdocks='",type.getInt("jdocks"),"'," ,
				"adocks='",type.getInt("adocks"),"'," ,
				"sensorrange='",type.getInt("sensorrange"),"'," ,
				"hydro='",type.getInt("hydro"),"'," ,
				"deutfactor='",type.getInt("deutfactor"),"'," ,
				"recost='",type.getInt("recost"),"',",
				"flags='",type.getString("flags"),"'," ,
				"werft='",type.getString("werft"),"'," ,
				"ow_werft='",type.getInt("ow_werft"),"'" ,
				" WHERE id='",ship.getInt("id"),"' AND modules='",oldModuleTbl,"'");					
	}
	
	public static void removeModule( SQLResultRow ship, int slot, int moduleid, int data ) {	
		// TODO
		Common.stub();
	}
	
	public static void recalculateModules( SQLResultRow ship ) {
		// TODO
		Common.stub();
	}
	
	private static Integer[] redAlertCheck( SQLResultRow ship, boolean checkonly ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
	
		User owner = context.createUserObject(ship.getInt("owner"));
		User.Relations relationlist = owner.getRelations();
	
		List<Integer> attackers = new ArrayList<Integer>();
		
		SQLQuery aowner = db.query("SELECT owner FROM ships WHERE id>0 AND x=",ship.getInt("x")," AND y=",ship.getInt("y")," ",
							"AND system=",ship.getInt("system")," AND e>0 AND owner!=",ship.getInt("owner")," AND alarm=1 AND `lock` IS NULL AND docked='' AND !LOCATE('nocrew',status) ",
							"GROUP BY owner ",(checkonly ? "LIMIT 1" : "") );
		while( aowner.next() ) {
			User auser = context.createUserObject(aowner.getInt("owner"));
			if( (auser.getVacationCount() != 0) && (auser.getWait4VacationCount() == 0) ) {
				continue;	
			}
			
			if( relationlist.fromOther.get(aowner) == User.Relation.ENEMY ) {
				attackers.add(aowner.getInt("owner"));
				if( checkonly ) {
					break;
				}
			} 
		}
		aowner.free();
	
		return attackers.toArray(new Integer[attackers.size()]);
	}
	
	/**
	 * Gibt <code>true</code> zurueck, wenn der angegebene Sektor fuer den angegebenen Spieler
	 * unter rotem Alarm steht, d.h. bei einem Einflug eine Schlacht gestartet wird
	 * @param userid Die Spieler-ID
	 * @param system Das System
	 * @param x Die X-Koordinate
	 * @param y Die Y-Koordinate
	 * @return <code>true</code>, falls der Sektor fuer den Spieler unter rotem Alarm steht
	 */
	public static boolean getRedAlertStatus( int userid, int system, int x, int y ) {
		SQLResultRow ship = new SQLResultRow();
		ship.put("owner", userid);
		ship.put("system", system);
		ship.put("x", x);
		ship.put("y", y);
				
		Integer[] attackers = redAlertCheck(ship, true);

		if( attackers.length > 0 ) {
			return true;
		}
		return false;
	}
	
	public static boolean move(int shipID, int direction, int distance, boolean forceLowHeat, boolean disableQuests) {
		// TODO
		Common.stub();
		return false;
	}
	
	public static boolean jump(int shipID, int nodeID, boolean knode ) {
		// TODO
		Common.stub();
		
		return false;
	}
	
	/**
	 * Die verschiedenen Dock-Aktionen
	 */
	public enum DockMode {
		/**
		 * Schiff extern docken
		 */
		DOCK,
		/**
		 * Schiff abdocken
		 */
		UNDOCK,
		/**
		 * Schiff landen
		 */
		LAND,
		/**
		 * Schiff starten
		 */
		START;
	}
	
	/*
	 * dock
	 * 		Schiffe an/abdocken sowie Jaeger landen/starten
	 * 
	 * $db -> Ein DB-Objekt
	 * $mode -> DOCK_DOCK (Andocken), DOCK_UNDOCK (abdocken), DOCK_LAND (landen), DOCK_START (starten) 
	 * $owner -> der Besitzer (der Schiffe oder ein Spieler mit superdock-flag)
	 * $shipID -> das Ausgangs/Zielschiff
	 * $dockids -> 	eine Schiffsid welches (ab)docken oder landen/starten soll
	 * 				ein Array mit Schiffsids, welche (ab)docken oder landen/starten sollen
	 * 				0, falls alle gedockten/gelandeten Schiffe abdocken/starten sollen [Default]
	 * 
	 */
	public static boolean dock(DockMode mode, int owner, int shipID, int[] dockids) {
		// TODO
		Common.stub();
		return false;
	}
	
	/**
	 * Entfernt das angegebene Schiff aus der Datenbank
	 * @param shipid Die ID des Schiffes
	 */
	public static void destroy(int shipid) {
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow ship = db.first("SELECT id,fleet,owner,docked,type,status,respawn FROM ships WHERE id='",shipid,"'");
		
		if( ship.isEmpty() ) {
			return;	
		}
		
		SQLResultRow user = db.first("SELECT id,flagschiff FROM users WHERE id='",ship.getInt("owner"),"'");	
	
		// Checken wir mal ob die Flotte danach noch bestehen darf....
		if( ship.getInt("fleet") != 0 ) {
			int fleetcount = db.first("SELECT count(*) count FROM ships WHERE fleet=",ship.getInt("fleet")).getInt("count");
			if( fleetcount <= 2 ) {
				db.update("DELETE FROM ship_fleets WHERE id=",ship.getInt("fleet"));
				db.update("UPDATE ship_fleets SET fleet=0 WHERE fleet=",ship.getInt("fleet"));
			}
		}
		
		// Ist das Schiff selbst gedockt? -> Abdocken
		if( !ship.getString("docked").equals("") && (ship.getString("docked").charAt(0) != 'l') ) {
			dock( DockMode.UNDOCK, ship.getInt("owner"), Integer.parseInt(ship.getString("docked")), new int[] {ship.getInt("id")} );
		}
	
		// Wenn es das Flagschiff ist -> Flagschiff auf 0 setzen
		if( ship.getInt("id") == user.getInt("flagschiff") ) {
			db.update("UPDATE users SET flagschiff=0 WHERE id=",ship.getInt("owner"));
		}
		
		// Evt. gedockte Schiffe abdocken
		SQLResultRow type = getShipType( ship );
		if( type.getInt("adocks") != 0 ) {
			dock( DockMode.UNDOCK, ship.getInt("owner"), ship.getInt("id"), null );	
		}
		if( type.getInt("jdocks") != 0 ) {
			dock( DockMode.START, ship.getInt("owner"), ship.getInt("id"), null );	
		}
		
		// Gibts bereits eine Loesch-Task? Wenn ja, dann diese entfernen
		Taskmanager taskmanager = Taskmanager.getInstance();
		Task[] tasks = taskmanager.getTasksByData( Taskmanager.Types.SHIP_DESTROY_COUNTDOWN, Integer.toString(ship.getInt("id")), "*", "*");
		for( int i=0; i < tasks.length; i++ ) {
			taskmanager.removeTask(tasks[i].getTaskID());	
		}
		
		// Falls eine respawn-Zeit gesetzt ist und ein Respawn-Image existiert -> respawn-Task setzen
		if( ship.getInt("respawn") != 0 ) {
			int negid = db.first("SELECT id FROM ships WHERE id='",(-shipid),"'").getInt("id");
			if( negid < 0 ) {
				taskmanager.addTask(Taskmanager.Types.SHIP_RESPAWN_COUNTDOWN, ship.getInt("respawn"), Integer.toString(negid), "", "");	
			}
		}
	
		// Und nun loeschen wir es...
		db.update("DELETE FROM ships WHERE id=",ship.getInt("id"));
		db.update("DELETE FROM offiziere WHERE dest='s ",ship.getInt("id"),"'");
		
		db.update("DELETE FROM werften WHERE shipid=",ship.getInt("id"));
		db.update("DELETE FROM ships_modules WHERE id=",ship.getInt("id"));
	}
	
	/**
	 * Generiert ein Truemmerteil mit Loot fuer das angegebene Schiff unter Beruecksichtigung desjenigen,
	 * der es zerstoert hat. Wenn fuer das Schiff kein Loot existiert oder keiner generiert wurde (Zufall spielt eine
	 * Rolle!), dann wird kein Truemmerteil erzeugt.
	 * @param shipid Die ID des Schiffes, fuer das Loot erzeugt werden soll
	 * @param destroyer Die ID des Spielers, der es zerstoert hat
	 */
	public static void generateLoot( int shipid, int destroyer ) {
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow ship = db.first("SELECT id,owner,x,y,system,history FROM ships WHERE id>0 AND id='",shipid,"'");
		
		SQLResultRow shiptype = getShipType( ship, true );
	
		int rnd = new Random().nextInt(101);
		
		// Gibts was zu looten?
		if( rnd > shiptype.getInt("chance4Loot") ) {
			return;
		}
		
		// History analysieren (Alle Schiffe die erst kuerzlich uebergeben wurden, haben kein Loot)
		String[] history = StringUtils.split(ship.getString("history").trim(), '\n');
		if( history.length > 0 ) {
			String lastHistory = history[history.length-1];
			
			if( lastHistory.indexOf("&Uuml;bergeben") != -1 ) {
				int date = Integer.parseInt(lastHistory.substring("&Uuml;bergeben am [tick=".length(),lastHistory.indexOf("] an ")-"&Uuml;bergeben am [tick=".length()));
				if( ContextMap.getContext().get(ContextCommon.class).getTick() - date < 49 ) {
					return;
				}
			}
		}	
		
		// Moeglichen Loot zusammensuchen
		List<SQLResultRow> loot = new ArrayList<SQLResultRow>();
		int maxchance = 0;
		
		SQLQuery lootHandle = db.query("SELECT id,chance,resource,count,totalmax " ,
				"FROM ship_loot " ,
				"WHERE owner='",ship.getInt("owner"),"' AND shiptype IN ('",ship.getInt("type"),"','-",ship.getInt("id"),"') AND targetuser IN ('0','",destroyer,"') AND totalmax!=0");
				
		while( lootHandle.next() ) {
			maxchance += lootHandle.getInt("chance");
			loot.add(lootHandle.getRow());
		}
		lootHandle.free();
	
		if( loot.size() == 0 ) {
			return;	
		}
		
		// Und nun den Loot generieren
		Cargo cargo = new Cargo();
		Random rand = new Random();
		
		for( int i=0; i <= Configuration.getIntSetting("CONFIG_TRUEMER_MAXITEMS"); i++ ) {
			rnd = rand.nextInt(maxchance+1);
			int currentchance = 0;
			for( int j=0; j < loot.size(); j++ ) {
				SQLResultRow aloot = loot.get(j);
				if( aloot.getInt("chance") + currentchance > rnd ) {
					if( aloot.getInt("totalmax") > 0 ) {
						aloot.put("totalmax", aloot.getInt("totalmax")-1);
											
						db.update("UPDATE ship_loot SET totalmax='",aloot.getInt("totalmax"),"' WHERE id='",aloot.getInt("id"),"'");	
					}
					cargo.addResource( Resources.fromString(aloot.getString("resource")), aloot.getLong("count") );
					break;	
				}	
				
				currentchance += aloot.getInt("chance");
			}
			
			rnd = rand.nextInt(101);
		
			// Gibts nichts mehr zu looten?
			if( rnd > shiptype.getInt("chance4Loot") ) {
				break;
			}
		}
		
		// Truemmer-Schiff hinzufuegen und entfernen-Task setzen
		db.update("INSERT INTO ships ",
				"(owner,name,type,cargo,x,y,system,hull,visibility) ",
				"VALUES ",
				"('-1','Tr&uuml;mmerteile',",Configuration.getIntSetting("CONFIG_TRUEMMER"),",'",cargo.save(),"','",
				ship.getInt("x"),"','",ship.getInt("y"),"','",ship.getInt("system"),"','",
				Configuration.getIntSetting("CONFIG_TRUEMMER_HUELLE"),"','",destroyer,"')");
				
		Taskmanager.getInstance().addTask(Taskmanager.Types.SHIP_DESTROY_COUNTDOWN, 21, Integer.toString(db.insertID()), "", "" );
				
		return;
	}
	
	/**
	 * Uebergibt ein Schiff an einen anderen Spieler. Gedockte/Gelandete Schiffe
	 * werden, falls moeglich, mituebergeben.
	 * @param user Der aktuelle Besitzer des Schiffs
	 * @param ship Das zu uebergebende Schiff
	 * @param newowner Der neue Besitzer des Schiffs
	 * @param testonly Soll nur getestet (<code>true</code>) oder wirklich uebergeben werden (<code>false</code>)
	 * @return <code>true</code>, falls ein Fehler bei der Uebergabe aufgetaucht ist (Uebergabe nicht erfolgreich)
	 */
	public static boolean consign( User user, SQLResultRow ship, User newowner, boolean testonly ) {
		if( newowner.getID() == 0 ) {
			MESSAGE.get().append("Der angegebene Spieler existiert nicht");
			return true;
		}
		
		if( newowner.getVacationCount() != 0 ) {
			MESSAGE.get().append("Sie k&ouml;nnen keine Schiffe an Spieler &uuml;bergeben, welche sich im Vacation-Modus befinden");
			return true;
		}
			
		if( newowner.hasFlag( User.FLAG_NO_SHIP_CONSIGN ) ) {
			MESSAGE.get().append("Sie k&ouml;nnen diesem Spieler keine Schiffe &uuml;bergeben");
		}
		
		if( ship.getString("lock").length() != 0 ) {
			MESSAGE.get().append("Die '"+ship.getString("name")+"'("+ship.getInt("id")+") kann nicht &uuml;bergeben werden, da diese in ein Quest eingebunden ist");
			return true;
		}
		
		if( ship.getString("status").indexOf("noconsign") != -1 ) {
			MESSAGE.get().append("Die '"+ship.getString("name")+"' ("+ship.getInt("id")+") kann nicht &uuml;bergeben werden");
			return true;
		}
		
		SQLResultRow shiptype = getShipType( ship, true );
		
		if( shiptype.getString("werft").length() != 0 ) {
			MESSAGE.get().append("Die '"+ship.getString("name")+"' ("+ship.getInt("id")+") kann nicht &uuml;bergeben werden, da es sich um eine Werft handelt");
			return true;
		} 
		
		UserFlagschiffLocation flagschiff = user.getFlagschiff();
		
		boolean result = true;		
		if( flagschiff.getID() == ship.getInt("id") ) {
			result = newowner.hasFlagschiffSpace();
		}
	
		if( !result  ) {
			MESSAGE.get().append("Die "+ship.getString("name")+" ("+ship.getInt("id")+") kann nicht &uuml;bergeben werden, da der Spieler bereits &uuml;ber ein Flagschiff verf&uuml;gt");
			return true;
		}
		
		Database db = ContextMap.getContext().getDatabase();
			
		if( !testonly ) {	
			ship.put("history", ship.getString("history")+"&Uuml;bergeben am [tick="+ContextMap.getContext().get(ContextCommon.class).getTick()+"] an "+newowner.getName()+" ("+newowner.getID()+")\n");
				
			db.prepare("UPDATE ships SET owner= ?,fleet=0,history= ? ,alarm='0' WHERE id= ? AND owner= ?")
					.update(newowner.getID(), ship.getString("history"), ship.getInt("id"), ship.getInt("owner"));
			db.update("UPDATE offiziere SET userid='",newowner.getID(),"' WHERE dest='s ",ship.getInt("id"),"'");
	
			Common.dblog( "consign", Integer.toString(ship.getInt("id")), Integer.toString(newowner.getID()),	
					"pos", Location.fromResult(ship).toString(),
					"shiptype", Integer.toString(ship.getInt("type")) );
			
			if( (flagschiff.getType() == UserFlagschiffLocation.Type.SHIP) && (flagschiff.getID() == ship.getInt("id")) ) {
				user.setFlagschiff(0);
				newowner.setFlagschiff(ship.getInt("id"));
			}
		}
		
		StringBuilder message = MESSAGE.get();
		SQLQuery s = db.query("SELECT * FROM ships WHERE id>0 AND docked IN ('",ship.getInt("id")+"','l "+ship.getInt("id")+"')");
		while( s.next() ) {
			int oldlength = message.length();
			boolean tmp = consign( user, s.getRow(), newowner, testonly );
			if( tmp && !testonly ) {
				dock( (s.getString("docked").charAt(0) == 'l' ? DockMode.START : DockMode.UNDOCK), newowner.getID(), ship.getInt("id"), new int[] {s.getInt("id")});			
			}
			
			if( oldlength != message.length() ) {
				message.insert(oldlength-1, "<br />");
			}
		}
		s.free();
		
		Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
		List<ItemCargoEntry> itemlist = cargo.getItems();
		for( ItemCargoEntry item : itemlist ) {
			Item itemobject = item.getItemObject();
			if( itemobject.isUnknownItem() ) {
				newowner.addKnownItem(item.getItemID());
			}
		}
	
		return false;
	}
	
	/**
	 * Gibt den Positionstext unter Beruecksichtigung von Nebeleffekten zurueck.
	 * Dadurch kann der Positionstext teilweise unleserlich werden (gewuenschter Effekt) 
	 * @param system Die System-ID
	 * @param x Die X-Koordinate
	 * @param y Die Y-Koordinate
	 * @param noSystem Soll das System angezeigt werden?
	 * @return der Positionstext
	 */
	public static String getLocationText(int system, int x, int y, boolean noSystem) {
		int nebel = getNebula(new Location(system, x, y));
		
		StringBuilder text = new StringBuilder(8);
		if( !noSystem ) {
			text.append(system);
			text.append(":");
		}
		
		if( nebel == 3 ) {
			text.append(x / 10);
			text.append("x/");
			text.append(y / 10);
			text.append('x');
			
			return text.toString();
		}
		else if( (nebel == 4) || (nebel == 5) ) {
			text.append(":??/??");
			return text.toString();
		}
		text.append(x);
		text.append('/');
		text.append(y);
		return text.toString();
	}
	
	public static String getLocationText(SQLResultRow ship, boolean noSystem) {
		return getLocationText(ship.getInt("system"), ship.getInt("x"), ship.getInt("y"), noSystem);
	}

	public static void cacheNebula( SQLResultRow nebel ) {	
		Ships.nebel.put(new Location(nebel.getInt("system"), nebel.getInt("x"), nebel.getInt("y")), nebel.getInt("type"));
	}
	
	/**
	 * Gibt den Nebeltyp an der Position zurueck, an der sich das Schiff gerade befindet
	 * @param ship Das Schiff
	 * @return Der Typ des Nebels. <code>-1</code>, falls an der Stelle kein Nebel ist
	 */
	public static int getNebula(SQLResultRow ship) {
		return getNebula(new Location(ship.getInt("system"), ship.getInt("x"), ship.getInt("y")));
	}
	
	private static PreparedQuery pqGetNebula = db.prepare("SELECT id,type FROM nebel WHERE system= ? AND x= ? AND y= ? ");
	
	/**
	 * Gibt den Nebeltyp an der angegebenen Position zurueck. Sollte sich an der Position kein
	 * Nebel befinden, wird <code>-1</code> zurueckgegeben.
	 * @param loc Die Position
	 * @return Der Nebeltyp oder <code>-1</code>
	 */
	public static synchronized int getNebula(Location loc) {
		if( !nebel.containsKey(loc) ) {
			SQLResultRow neb = pqGetNebula.pfirst(loc.getSystem(), loc.getX(), loc.getY());
			if( neb.isEmpty() ) {
				nebel.put(loc, -1);	
			}
			else {
				nebel.put(loc, neb.getInt("type"));	
			}
		}
		return nebel.get(loc);
	}
	
	private static Map<Integer,Integer> fleetCountList = Collections.synchronizedMap(new CacheMap<Integer,Integer>(500));
	
	/**
	 * Entfernt das Schiff aus der Flotte. 
	 * @param ship Die SQL-Ergebniszeile des Schiffs
	 */
	public static void removeFromFleet( SQLResultRow ship ) {
		Database db = ContextMap.getContext().getDatabase();
		
		if( ship.getInt("fleet") == 0 ) {
			return;
		}

		if( fleetCountList.containsKey(ship.getInt("fleet")) ) {
			// Kein Check auf id > 0, da auch (Spawn)Schiffe mit einer id < 0 der Flotte angehoeren koennen!
			fleetCountList.put(ship.getInt("fleet"), db.first("SELECT count(*) count FROM ships WHERE fleet="+ship.getInt("fleet")).getInt("count"));
		}
		int fleetcount = fleetCountList.get(ship.getInt("fleet"));
				
		if( fleetcount > 2 ) {
			db.tUpdate(1, "UPDATE ships SET fleet=0 WHERE id>0 AND id=",ship.getInt("id"));
			MESSAGE.get().append("aus der Flotte ausgetreten");
			
			fleetCountList.put(ship.getInt("fleet"), --fleetcount);
		} 
		else {
			db.tUpdate(1, "UPDATE ships SET fleet=0 WHERE fleet="+ship.getInt("fleet"));
			db.tUpdate(1, "DELETE FROM ship_fleets WHERE id="+ship.getInt("fleet"));
			MESSAGE.get().append("Flotte aufgel&ouml;&szlig;t");
			
			fleetCountList.remove(ship.getInt("fleet"));
		}
	}
	
	public static ShipTypeDataChangeset getTypeChangeSetFromXML(Node node) {
		Common.stub();
		return null;
	}
}
