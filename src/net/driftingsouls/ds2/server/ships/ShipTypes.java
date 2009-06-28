/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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

import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Diverse Funktionen rund um Schiffstypen.
 *  TODO: Ja, ich weiss, das ist nicht besonders schoen. Besser waeren richtige Schiffstypenobjekte...
 * @author Christopher Jung
 *
 */
public class ShipTypes {
	private static final Log log = LogFactory.getLog(ShipTypes.class);
	
	/**
	 * Kennzeichnet das Schiff als Jaeger.
	 */
	public static final String SF_JAEGER = "jaeger";
	
	/**
	 * Das angegebene Schiff verfuegt ueber eine Zerstoererpanzerung.
	 */
	public static final String SF_ZERSTOERERPANZERUNG = "zerstoererpanzerung";
	
	/**
	 * Das angegebene Schiff kann Asteroiden kolonisieren.
	 */
	public static final String SF_COLONIZER = "colonizer";
	
	/**
	 * Das angegebene Schiff kann in Kaempfen fluechtende Schiffe abfangen.
	 */
	public static final String SF_ABFANGEN = "abfangen";
	
	/**
	 * Das angegebene Schiff ist nicht kaperbar.
	 */
	public static final String SF_NICHT_KAPERBAR = "nicht_kaperbar";
	
	/**
	 * Das Schiff wird nach einem Transfervorgang beim naechsten Tick zerstoert.
	 */
	public static final String SF_INSTABIL = "instabil";
	
	/**
	 * Das Schiff ist nur sichtbar, wenn man sich im selben Sektor befindet.
	 */
	public static final String SF_SEHR_KLEIN = "sehr_klein";
	
	/**
	 * Transfers von und zu dem Schiff sind nicht moeglich.
	 */
	public static final String SF_KEIN_TRANSFER = "kein_transfer";
	
	/**
	 * Das Schiff verfuegt ueber erweiterte SRS-Sensoren (mehr Informationen).
	 */
	public static final String SF_SRS_AWAC = "srs_awac";
	
	/**
	 * Das Schiff verfuegt ueber zusaetzliche Erweiterungen zuzueglich zu den erweiterten SRS-Sensoren.
	 * (Nur wirksam in Kombination mit {@link #SF_SRS_AWAC}
	 */
	public static final String SF_SRS_EXT_AWAC = "srs_ext_awac";
	
	/**
	 * Das Schiff verfuegt ueber einen shivanischen Sprungantrieb.
	 */
	public static final String SF_JUMPDRIVE_SHIVAN = "jumpdrive_shivan";
	
	/**
	 * Das Schiff kann direkt einer Schlacht beitreten ohne eine Runde "aussetzen" zu muessen.
	 */
	public static final String SF_INSTANT_BATTLE_ENTER = "instant_battle_enter";
	
	/**
	 * Das Schiff kann nicht gepluendert werden.
	 */
	public static final String SF_NICHT_PLUENDERBAR = "nicht_pluenderbar";
	
	/**
	 * Das Schiff kann nicht zerstoert werden.
	 */
	public static final String SF_GOD_MODE = "god_mode";
	
	/**
	 * Das Schiff kann Drohnen kontrollieren.
	 */
	public static final String SF_DROHNEN_CONTROLLER = "drohnen_controller";
	
	/**
	 * Das Schiff ist eine Drohne und kann daher nur im selben Sektor wie ein Drohnenkontroller agieren ({@link #SF_DROHNEN_CONTROLLER}).
	 * Wenn kein Drohnenkontroller vorhanden ist, ist es handlungsunfaehig.
	 */
	public static final String SF_DROHNE = "drohne";
	
	/**
	 * Das Schiff kann in einer Schlacht in die zweite Reihe fliegen.
	 */
	public static final String SF_SECONDROW = "secondrow";
	
	/**
	 * Das Schiff kann Offiziere in Hoehe der eigenen max. Crew aufnehmen.
	 */
	public static final String SF_OFFITRANSPORT = "offitransport";
	
	/**
	 * Das Schiff kann sich mit anderen Werften zu Werftkomplexen zusammenschliessen.
	 */
	public static final String SF_WERFTKOMPLEX = "werftkomplex";
	
	/**
	 * Das Schiff kann keine selbstzerstoerung.
	 */
	public static final String SF_NICHT_ZERSTOERBAR = "nosuicide";
	
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
		shipTypeFlagNames.put(SF_WERFTKOMPLEX, "Werftkomplex");
		shipTypeFlagNames.put(SF_NICHT_ZERSTOERBAR, "Keine Selbstzerst&ouml;rung");
		
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
		shipTypeFlagDescs.put(SF_WERFTKOMPLEX, "Diese Werft kann sich mit anderen Werften, welche die Eigenschaft Werftkomplex besitzen, zusammenschliessen. Die Ressourcen der Einzelwerften werden dabei gemeinsam genutzt");
		shipTypeFlagDescs.put(SF_NICHT_ZERSTOERBAR, "Dieses Schiff verf&uuml;gt &uuml;ber keine Selbstzerst&ouml;rungsanlage");
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
	 * Gibt die Beschreibung zu einem Schiffsflag zurueck.
	 * @param flag Das Schiffsflag
	 * @return die Beschreibung
	 */
	public static String getShipTypeFlagDescription( String flag ) {
		if( !shipTypeFlagNames.containsKey(flag) ) {
			return "";
		}
		return shipTypeFlagNames.get(flag);
	}
	
	/**
	 * Testet, ob ein Schiffstyp ein bestimmtes Schiffstypen-Flag (SF_*) hat.
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
	
	/**
	 * Gibt die zu einer Schiffsklassen-ID gehoerende Schiffsklasse
	 * zurueck.
	 * @param classid Die Schiffsklassen-ID
	 * @return Die Schiffsklasse
	 */
	public static ShipClasses getShipClass(int classid) {
		return ShipClasses.values()[classid];
	}

	private static Map<Integer,SQLResultRow> shiptypes = new HashMap<Integer,SQLResultRow>();

	private static String buildShipPicturePath( SQLResultRow type, boolean forceServer ) {
		String picture = type.getString("picture");
		Context context = ContextMap.getContext();
		
		if( (context != null) && !forceServer && !type.getBoolean("hide") && (context.getActiveUser() != null) ) {
			picture = context.getActiveUser().getImagePath()+picture;	
		}
		else {
			picture = BasicUser.getDefaultImagePath()+picture;
		}
		
		return picture;
	}

	/**
	 * Gibt die Typen-Daten des angegebenen Schiffs zurueck.
	 * @param shipdata Eine SQL-Ergebniszeile mit den daten des Schiffes
	 * @return die Typen-Daten
	 */
	public static SQLResultRow getShipType( SQLResultRow shipdata ) {
		return getShipType(shipdata, false);
	}

	/**
	 * Gibt die Typen-Daten des angegebenen Schiffs zurueck.
	 * @param shipdata Eine SQL-Ergebniszeile mit den daten des Schiffes
	 * @param plaindata Sollen die Bildpfade angepasst werden (<code>false</code>) oder so zurueckgegeben werden,
	 * wie sie in der DB stehen (<code>true</code>)?
	 * @return die Typen-Daten
	 */
	public static SQLResultRow getShipType( SQLResultRow shipdata, boolean plaindata ) {
		if( !shipdata.containsKey("type") || !shipdata.containsKey("status") ) {
			Database db = ContextMap.getContext().getDatabase();
			shipdata = db.first("SELECT * FROM ships WHERE id="+shipdata.getInt("id"));
			log.warn("getShipType: type oder status fehlen in den Schiffsdaten", new Throwable());
		}
		
		int shiptype = shipdata.getInt("type");
		
		if( !shipdata.containsKey("modules") || (shipdata.getInt("modules") != 0) ) {
			Database db = ContextMap.getContext().getDatabase();
			shipdata = db.prepare("SELECT nickname,picture,ru,rd,ra,rm,eps,cost,hull,panzerung,cargo,heat,crew,weapons,maxheat,torpedodef,shields,size,jdocks,adocks,sensorrange,hydro,deutfactor,recost,flags,werft,ow_werft " +
					"FROM ships_modules " +
					"WHERE id>0 AND id= ? ")
				.first(shipdata.getInt("id"));
			
			if( shipdata.isEmpty() ) {
				shipdata = null;
			}
		}
		else {
			shipdata = null;
		}
		
		return getShipType(shiptype, shipdata, plaindata);
	}

	/**
	 * Gibt die Typen-Daten des angegebenen Schiffs bzw Schifftyps zurueck.
	 * @param shiptype Die ID des Schiffs bzw des Schifftyps
	 * @param isShip Handelt es sich um ein Schiff (<code>true</code>)?
	 * @param plaindata Sollen die Bildpfade angepasst werden (<code>false</code>) oder so zurueckgegeben werden,
	 * wie sie in der DB stehen (<code>true</code>)?
	 * @return die Typen-Daten
	 */
	protected static SQLResultRow getShipType( int shiptype, boolean isShip, boolean plaindata ) {
		if( isShip ) {
			// TODO: Schiffscache implementieren!
			int shipid = shiptype;
			
			Database db = ContextMap.getContext().getDatabase();
			SQLResultRow shipdata = db.prepare("SELECT type,status FROM ships WHERE id>0 AND id= ?")
				.first(shiptype);
			
			shiptype = shipdata.getInt("type");
			
			if( !shipdata.containsKey("modules") || (shipdata.getInt("modules") != 0) ) {
				shipdata = db.prepare("SELECT nickname,picture,ru,rd,ra,rm,eps,cost,hull,panzerung,cargo,heat,crew,weapons,maxheat,torpedodef,shields,size,jdocks,adocks,sensorrange,hydro,deutfactor,recost,flags,werft,ow_werft " +
						"FROM ships_modules " +
						"WHERE id>0 AND id= ? ")
					.first(shipid);
			
				if( shipdata.isEmpty() ) {
					shipdata = null;
				}
			}
			else {
				shipdata = null;
			}
			
			return getShipType(shiptype, shipdata, plaindata);
		}
		return getShipType(shiptype, null, plaindata);
	}

	private static SQLResultRow getShipType( int shiptype, SQLResultRow shipdata, boolean plaindata ) {
		synchronized (shiptypes) {
			if( !shiptypes.containsKey(shiptype) ) {
				Database db = ContextMap.getContext().getDatabase();
				SQLResultRow row = db.prepare("SELECT *,LOCATE('=',weapons) as military FROM ship_types WHERE id= ? ")
					.first(shiptype);
				
				if( row.isEmpty() ) {
					throw new NoSuchShipTypeException("Unbekannter Schiffstyp '"+shiptype+"'");
				}
				
				shiptypes.put(shiptype, row);
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
			else {
				picture = shipdata.getString("picture");
			}
	
			type.put("picture", buildShipPicturePath(type, (!shiptypes.get(shiptype).getString("picture").equals(picture) ? true : false) ));
		}
		
		return type;
	}
}
