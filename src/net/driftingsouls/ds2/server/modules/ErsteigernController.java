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
package net.driftingsouls.ds2.server.modules;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.FactionPages;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

/**
 * Zeigt die Fraktionsseiten an
 * @author Christopher Jung
 * 
 * @urlparam Integer faction Die ID der anzuzeigenden Fraktion
 *
 */
public class ErsteigernController extends DSGenerator {
	/**
	 * Ein Eintrag im Shop
	 * @author Christopher Jung
	 *
	 */
	private static abstract class ShopEntry {
		private int id;
		private int factionID;
		private int type;
		private String resource;
		private long price;
		private int availability;
		
		/**
		 * Konstruktor
		 * @param data Die SQL-Ergebniszeile zum Eintrag
		 */
		public ShopEntry(SQLResultRow data) {
			this.id = data.getInt("id");
			this.factionID = data.getInt("faction_id");
			this.type = data.getInt("type");
			this.resource = data.getString("resource");
			this.price = data.getLong("price");
			this.availability = data.getInt("availability");
		}
		
		/**
		 * Gibt die ID des Eintrags zurueck
		 * @return Die ID
		 */
		public int getID() {
			return this.id;
		}
		
		/**
		 * Gibt die ID der Fraktion zurueck, der der Eintrag gehoert
		 * @return Die ID der Fraktion
		 */
		public int getFactionID() {
			return this.factionID;
		}
		
		/**
		 * Gibt den Typ des Eintrags zurueck
		 * @return Der Typ
		 */
		public int getType() {
			return this.type;
		}
		
		/**
		 * Gibt den Namen des Eintrags zurueck
		 * @return Der Name
		 */
		public abstract String getName();
		/**
		 * Gibt das zum Eintrag gehoerende Bild zurueck
		 * @return Das Bild
		 */
		public abstract String getImage();
		/**
		 * Gibt einen zum Eintrag gehoerenden Link zurueck
		 * @return Der Link
		 */
		public abstract String getLink();
		
		/**
		 * Gibt die Verfuegbarkeit des Eintrags zurueck
		 * @return Die Verfuegbarkeit
		 */
		public int getAvailability() {
			return this.availability;
		}
		
		/**
		 * Gibt die Verfuegbarkeit des Eintrags als Text zurueck
		 * @return Die Verfuegbarkeit als Text
		 */
		public String getAvailabilityName() {
			switch( this.getAvailability()  ) {
			case 0:
				return "Genug vorhanden";
			case 1:
				return "Nur noch 1-3 vorhanden";
			case 2:
				return "Nicht verf&uuml;gbar";
			}
			return "";
		}
		
		/**
		 * Gibt die mit der Verfuegbarkeit assoziierte Farbe zurueck
		 * @return Die Farbe der Verfuegbarkeit 
		 */
		public String getAvailabilityColor() {
			switch( this.getAvailability() ) {
			case 0:
				return "#55DD55";
			case 1:
				return "#FFFF44";
			case 2:
				return "#CC2222";
			} 
			return "";
		}
		
		/**
		 * Soll die Verkaufsmenge angezeigt werden?
		 * @return <code>true</code>, falls die Verkaufsmenge angezeigt werden soll
		 */
		public boolean showAmountInput() {
			return true;
		}
		
		/**
		 * Gibt den Kaufpreis zurueck 
		 * @return Der Kaufpreis
		 */
		public long getPrice() {
			return price;
		}
		
		/**
		 * Gibt den Kaufpreis als Text zurueck
		 * @return Der Kaufpreis als Text
		 */
		public String getPriceAsText() {
			return Common.ln(this.getPrice());
		}
		
		/**
		 * Gibt den Verkaufsinhalt, den der Eintrag enthaelt, zurueck.
		 * Der Typ ist Abhaengig vom Typen des Eintrags
		 * @return Der Verkaufsinhalt
		 */
		public String getResource() {
			return this.resource;
		}
	}
	
	/**
	 * Repraesentiert ein Shopeintrag, welcher ein Schiff enthaelt
	 * @author Christopher Jung
	 *
	 */
	private static class ShopShipEntry extends ShopEntry {
		private SQLResultRow shiptype;
		
		/**
		 * Konstruktor
		 * @param data Die SQL-Ergebniszeile des Shopeintrags
		 */
		public ShopShipEntry( SQLResultRow data ) {
			super(data);
			
			this.shiptype = ShipTypes.getShipType(Integer.parseInt(this.getResource()), false);
		}
		
		@Override
		public String getName() {
			return this.shiptype.getString("nickname");	
		}
		
		@Override
		public String getImage() {
			return this.shiptype.getString("picture");	
		}
		
		@Override
		public String getLink() {
			return Common.buildUrl(ContextMap.getContext(), "default", "module", "schiffinfo", "ship", shiptype.getInt("id") );
		}
	}
	
	/**
	 * Repraesentiert ein Shopeintrag, welcher eine Resource enthaelt
	 * @author Christopher Jung
	 *
	 */
	private static class ShopResourceEntry extends ShopEntry {
		private ResourceEntry resourceEntry;
		
		/**
		 * Konstruktor
		 * @param data Die SQL-Ergebniszeile des Shopeintrags
		 */
		public ShopResourceEntry( SQLResultRow data ) {
			super(data);
			
			Cargo cargo = new Cargo();
			cargo.addResource( Resources.fromString(this.getResource()), 1 );
			cargo.setOption( Cargo.Option.SHOWMASS, false );
			cargo.setOption( Cargo.Option.LARGEIMAGES, true );
			this.resourceEntry = cargo.getResourceList().iterator().next();
		}
		
		@Override
		public String getName() {
			return Cargo.getResourceName( resourceEntry.getId() );
		}
		
		@Override
		public String getImage() {
			return resourceEntry.getImage();	
		}
		
		@Override
		public String getLink() {
			if( resourceEntry.getId().isItem() ) {
				return Common.buildUrl(ContextMap.getContext(), "details", "module", "iteminfo", "item", resourceEntry.getId().getItemID());
			}
			
			return "#";	
		}
		
		@Override
		public String getAvailabilityName() {
			if( resourceEntry.getId().isItem() || (this.getAvailability() != 1) ) {
				return super.getAvailabilityName();
			}
			return "Nur noch wenige Einheiten vorhanden";
		}
	}
	
	/**
	 * Repraesentiert ein Shopeintrag, welcher einen Ganymede-Transport enthaelt
	 * @author Christopher Jung
	 *
	 */
	private static class ShopGanyTransportEntry extends ShopEntry {
		/**
		 * Die Schiffstypen-ID einer Ganymede
		 */
		public static final int SHIPTYPE_GANYMEDE = 33;
		
		private long minprice = Long.MAX_VALUE;
		private long maxprice = Long.MIN_VALUE;
		private int ganytransid;
		
		/**
		 * Konstruktor
		 * @param data Die SQL-Ergebniszeile des Shopeintrags
		 */
		public ShopGanyTransportEntry( SQLResultRow[] data ) {
			super(data[0]);
			
			for( int i=0; i < data.length; i++ ) {
				if( data[i].getLong("price") < this.minprice ) {
					this.minprice = data[i].getLong("price");
				}
				if( data[i].getLong("price") > this.maxprice ) {
					this.maxprice = data[i].getLong("price");
				} 	
				this.ganytransid = data[0].getInt("id");
			}
		}
		
		@Override
		public int getID() {
			return ganytransid;
		}
			
		@Override
		public long getPrice() {
			return (this.minprice != this.maxprice) ? (this.minprice + this.maxprice)/2 : this.minprice;
		}
		
		@Override
		public String getPriceAsText() {
			return (this.minprice != this.maxprice) ? (Common.ln(this.minprice)+" - "+Common.ln(this.maxprice)) : (Common.ln(this.minprice))+"<br />pro System";
		}
		
		@Override
		public String getName() {
			return "Ganymede-Transport";	
		}
		
		@Override
		public String getImage() {
			return Configuration.getSetting("URL")+"data/interface/ganymede_transport.png";	
		}
		
		@Override
		public String getLink() {
			return "#";
		}
		
		@Override
		public boolean showAmountInput() {
			return false;
		}
		
		@Override
		public int getAvailability() {
			return 0;
		}
	}
	
	private int ticks = 0;
	private int faction = 0;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ErsteigernController(Context context) {
		super(context);
		
		setTemplate("ersteigern.html");
		
		parameterNumber("faction");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		// Ausgewaehlte Fraktion ueberpruefen und deren Menueeintraege freischalten
		int faction = this.getInteger("faction");
		User.Relations relationlist = user.getRelations();
		
		if( faction == 0 ) {
			if( Faction.get(user.getID()) != null ) {
				faction = user.getID();
			}
			else {
				Map<Integer,Faction> factions = Faction.getFactions();
				for( Integer aFactionID : factions.keySet() ) {
					if( (user.getRelation(aFactionID) != User.Relation.ENEMY) &&
						(relationlist.fromOther.get(aFactionID) != User.Relation.ENEMY) ) {
						faction = aFactionID;
						break;
					}
				}
			}
		}
		
		if( faction == 0 ) {
			addError("Keine Fraktion will mit ihnen zu handeln solange die Beziehungen feindlich sind");	
			return false;
		}

		if( Faction.get(faction) == null ) {
			addError("Die angegebene Fraktion verf&uuml;gt &uuml;ber keine eigene Seite");	
			return false;
		}
		
		if( (user.getRelation(faction) == User.Relation.ENEMY) ||
			(relationlist.fromOther.get(faction) == User.Relation.ENEMY) ) {
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");	
			return false;
		}
		
		FactionPages pages = Faction.get(faction).getPages();
		for( String aPage : pages.getPages() ) {
			t.set_var("faction."+aPage,1);	
		}
		
		this.faction = faction;
		
		// Fraktionsmenue generieren
		StringBuilder factionmenu = new StringBuilder(200);
		factionmenu.append( StringUtils.replaceChars(Common.tableBegin( 250, "center" ), '"', '\'') );
		
		Map<Integer,Faction> factions = Faction.getFactions();
		for( Faction factionObj : factions.values() ) {
			if( !factionObj.getPages().isEnabled() ) {
				continue;
			}
			User aFactionUser = getContext().createUserObject(factionObj.getID());
			
			if( (user.getRelation(factionObj.getID()) == User.Relation.ENEMY) ||
				(relationlist.fromOther.get(factionObj.getID()) == User.Relation.ENEMY) ) {
				factionmenu.append("<span style='color:red;font-size:14px'>"+StringUtils.replaceChars(Common._title(aFactionUser.getName()), '"', '\'')+"</span><br />");	
			}
			else {	
				factionmenu.append("<a style='font-size:14px' class='profile' href='"+Common.buildUrl(getContext(), "default", "faction", factionObj.getID())+"'>"+StringUtils.replaceChars(Common._title(aFactionUser.getName()), '"', '\'')+"</a><br />");
			}
		}
		factionmenu.append( StringUtils.replaceChars(Common.tableEnd(), '"', '\'') );
		String factionmenuStr = StringEscapeUtils.escapeJavaScript(StringUtils.replace(StringUtils.replace(factionmenu.toString(), "<", "&lt;"), ">", "&gt;"));
		
		User factionuser = getContext().createUserObject(faction);
		
		t.set_var(	"user.konto",			Common.ln(user.getKonto()),
					"global.faction",		faction,
					"global.faction.name",	Common._title(factionuser.getName()),
					"global.menusize",		pages.getMenuSize(),
					"global.factionmenu",	factionmenuStr );
		
		this.ticks = getContext().get(ContextCommon.class).getTick();
		
		SQLResultRow paket = db.first("SELECT id FROM versteigerungen_pakete");
		t.set_var("gtu.paket", !paket.isEmpty());
					
		return true;
	}
	
	/**
	 * Aendert das System, in dem ersteigerte Dinge gespawnt werden sollen
	 * @urlparam Integer favsys Die ID des neuen Systems, in dem ersteigerte Dinge gespawnt werden sollen
	 *
	 */
	public void changeDropZoneAction() {
		if( !Faction.get(faction).getPages().hasPage("versteigerung") ) {
			redirect();
			return;	
		}
		
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("favsys");
		int favsys = getInteger("favsys");
		
		if( Systems.get().system(favsys).getDropZone() != null ) {
			user.setGtuDropZone( favsys );
			t.set_var("show.newcoords",1);
		}
	
		redirect();	
	}
	
	/**
	 * Gibt ein Gebot auf eine Versteigerung ab bzw zeigt, falls kein Gebot angegeben wurde, 
	 * die angegebene Versteigerung an
	 * @urlparam Integer bid Der gebotene Betrag oder 0
	 * @urlparam Integer auk Die Auktion auf die geboten werden soll
	 *
	 */
	public void bidEntryAction() {
		if( !Faction.get(faction).getPages().hasPage("versteigerung") ) {
			redirect();
			return;	
		}
		
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		parameterNumber("bid");
		int bid = getInteger("bid");
		
		parameterNumber("auk");
		int auk = getInteger("auk");
		
		SQLResultRow entry = db.first("SELECT * FROM versteigerungen WHERE id=",auk);
		
		if( entry.isEmpty() || (entry.getInt("owner") == user.getID()) ) {
			addError("Sie k&ouml;nnen nicht bei eigenen Versteigerungen mitbieten");
			redirect();
			return;
		}
		
		// Wenn noch kein Gebot abgegeben wurde -> Versteigerung anzeigen
		if( bid == 0 ) {
			int entrywidth = 0;
			int entryheight = 0;
			long entrycount = 0;
			String entryname = "";
			String entryimage = "";
			String entrylink = "#";
		
			if( entry.getInt("mtype") == 1 ) {	//Schiff
				SQLResultRow shiptype = ShipTypes.getShipType(entry.getInt("type"), false);
				entryname = shiptype.getString("nickname");
				entryimage = shiptype.getString("picture");
				entrylink = Common.buildUrl(getContext(), "default", "module", "schiffinfo", "ship", entry.getInt("type") );
			}
			else if( entry.getInt("mtype") == 2 ) {	// Artefakt
				Cargo cargo = new Cargo( Cargo.Type.STRING, entry.getString("type"));
				cargo.setOption( Cargo.Option.SHOWMASS, false );
				cargo.setOption( Cargo.Option.LARGEIMAGES, true );
				ResourceEntry resource = cargo.getResourceList().iterator().next();
							
				entryname = Cargo.getResourceName( resource.getId() );
				entryimage = resource.getImage();
			
				if( resource.getId().isItem() ) {
					entrylink = Common.buildUrl(getContext(), "details", "module", "iteminfo", "item", resource.getId().getItemID() );
				}
				else {
					entrylink = "#";	
				}
			
				if( !resource.showLargeImages() ) {
					entrywidth = 50;
					entryheight = 50;
				}
			
				if( resource.getCount1() > 1 ) { 
					entrycount = resource.getCount1();
				}
			}

			String bietername = "";

			User bieter = getContext().createUserObject( entry.getInt("bieter") );
			
			if( bieter.getID() == faction ) {
				bietername = bieter.getName();	
			}
			else if( bieter.getID() == user.getID() ) {
				bietername = bieter.getName();
			}
			else if( user.getAccessLevel() > 20 ) {
				bietername = bieter.getName();	
			}
			else if( (bieter.getAlly() != 0) && (bieter.getAlly() == user.getAlly()) ) {
				boolean showGtuBieter = db.first("SELECT showGtuBieter FROM ally WHERE id=",bieter.getAlly()).getBoolean("showGtuBieter");

				if( showGtuBieter ) {
					bietername = bieter.getName();	
				}	
			}
			
			long cost = entry.getLong("preis")+(long)(entry.getLong("preis")/20d);
			if( cost == entry.getLong("preis") ) {
				cost++;
			}

			t.set_var(	"show.bid.entry",	1,
						"entry.type.name",	StringEscapeUtils.escapeJavaScript(StringUtils.replaceChars(entryname, '"', '\'')),
						"entry.type.image",	entryimage,
						"entry.link",		entrylink,
						"entry.width",		entrywidth,
						"entry.height",		entryheight,
						"entry.count",		entrycount,
						"bid.player",		Common._title(bietername),
						"bid.player.id",	bieter.getID(),
						"bid.price",		cost,
						"bid.id",			auk );
			return;
		} 
		// Gebot bestaetigt -> Versteigerung aktuallisieren
		else if( bid > 0 ) {
			long cost = entry.getLong("preis")+(long)(entry.getLong("preis")/20d);
			if( cost == entry.getLong("preis") ) {
				cost++;
			}
		
			String entryname = "";
			
			if( entry.getInt("mtype") == 1 ) {
				SQLResultRow shiptype = ShipTypes.getShipType(entry.getInt("type"), false);
				entryname = shiptype.getString("nickname");
			}
			else if( entry.getInt("mtype") == 2 ) { 
				Cargo cargo = new Cargo( Cargo.Type.STRING, entry.getString("type"));
				cargo.setOption( Cargo.Option.SHOWMASS, false );
				cargo.setOption( Cargo.Option.LARGEIMAGES, true );
				ResourceEntry resource = cargo.getResourceList().iterator().next();
							
				entryname = Cargo.getResourceName( resource.getId() );
			}

			if( (bid >= cost) && (user.getKonto().compareTo(new BigDecimal(bid).toBigInteger()) >= 0 ) ) {
				db.tBegin();
				
				if( entry.getInt("bieter") != faction ) {
					User bieter = getContext().createUserObject(entry.getInt("bieter"));
						
					PM.send(getContext(), faction, entry.getInt("bieter"), "Bei Versteigerung &uuml;berboten", 
							"Sie wurden bei der Versteigerung um '"+entryname+"' &uuml;berboten. Die von ihnen gebotenen RE in H&ouml;he von "+Common.ln(entry.getLong("preis"))+" wurden auf ihr Konto zur&uuml;ck&uuml;berwiesen.\n\nGaltracorp Unlimited");
					 
				 	bieter.transferMoneyFrom( faction, entry.getLong("preis"), "R&uuml;ck&uuml;berweisung Gebot #2"+entry.getInt("id")+" '"+entryname+"'", false, User.TRANSFER_SEMIAUTO);
				}
					
				db.tUpdate(1, "UPDATE versteigerungen " +
						"SET tick=",entry.getInt("tick") <= ticks+3 ? ticks+3 : entry.getInt("tick"),",bieter=",user.getID(),",preis=",bid," " +
						"WHERE id=",auk," AND tick=",entry.getInt("tick")," AND bieter=",entry.getInt("bieter")," AND preis=",entry.getInt("preis"));
					
				User gtu = getContext().createUserObject( faction );
				gtu.transferMoneyFrom( user.getID(), bid, "&Uuml;berweisung Gebot #2"+entry.getInt("id")+" '"+entryname+"'", false, User.TRANSFER_SEMIAUTO);
				
				if( !db.tCommit() ) {
					addError("W&auml;hrend des Bietvorgangs ist ein Fehler aufgetreten. Bitte versuchen sie es sp&auml;ter erneut");
					redirect("versteigerung");
					return;
				}
				
				user.setTemplateVars(t);
				t.set_var( 	"user.konto", 		Common.ln(user.getKonto()),
							"show.highestbid",	1);
			}
			else {
				t.set_var("show.lowres",1);
			}
		}
		
		redirect();
	}
	
	/**
	 * Gibt ein Gebot auf die Versteigerung eines Pakets ab bzw zeigt, falls kein Gebot angegeben wurde, 
	 * die angegebene Versteigerung an
	 * @urlparam Integer bid Der gebotene Betrag oder 0
	 * @urlparam Integer auk Die Auktion auf die geboten werden soll
	 *
	 */
	public void bidPaketAction() {
		if( !Faction.get(faction).getPages().hasPage("paket") ) {
			redirect();
			return;	
		}
		
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		parameterNumber("bid");
		long bid = getInteger("bid");
		
		parameterNumber("auk");
		int auk = getInteger("auk");
		
		SQLResultRow paket = db.first("SELECT * FROM versteigerungen_pakete WHERE id=",auk);
		if( paket.isEmpty() ) {
			redirect("paket");
			return;
		}
		
		long cost = paket.getLong("preis")+(long)(paket.getLong("preis")/20d);
		if( cost == paket.getLong("preis") ) {
			cost++;
		}
		
		// Wenn noch kein Gebot abgegeben wurde -> Versteigerung anzeigen
		if( bid == 0 ) {
			t.set_var(	"show.bid.paket",	1,
						"bid.price",		cost,
						"bid.id",			auk );

			return;
		} 
		// Gebot bestaetigt -> Versteigerung aktuallisieren
		else if( bid > 0 ) {		
			if( (bid >= cost) && (user.getKonto().compareTo(new BigDecimal(bid).toBigInteger()) >= 0 ) ) {
				db.tBegin();
				
				if( paket.getInt("bieter") != faction ) {
					User bieter = getContext().createUserObject(paket.getInt("bieter"));
						
					PM.send(getContext(), faction, bieter.getID(), "Bei Versteigerung um das GTU-Paket &uuml;berboten", 
							"Sie wurden bei der Versteigerung um das GTU-Paket &ueberboten. Die von ihnen gebotenen RE in H&ouml;he von "+Common.ln(paket.getLong("preis"))+" wurden auf ihr Konto zur&uuml;ck&uuml;berwiesen.\n\nGaltracorp Unlimited");
					 
				 	bieter.transferMoneyFrom( faction, paket.getLong("preis"), "R&uuml;ck&uuml;berweisung Gebot #9"+paket.getInt("id")+" 'GTU-Paket'", false, User.TRANSFER_SEMIAUTO);
				}
				
				db.tUpdate(1, "UPDATE versteigerungen_pakete " +
						"SET tick=",paket.getInt("tick") <= ticks+3 ? ticks+3 : paket.getInt("tick"),",bieter=",user.getID(),",preis=",bid," " +
						"WHERE id=",auk," AND preis=",paket.getLong("preis")," AND bieter=",paket.getInt("bieter")," AND tick=",paket.getInt("tick"));
				
				User gtu = getContext().createUserObject( faction );
				gtu.transferMoneyFrom( user.getID(), bid, "&Uuml;berweisung Gebot #9"+auk+" 'GTU-Paket'", false, User.TRANSFER_SEMIAUTO);
				
				if( !db.tCommit() ) {
					addError("W&auml;hrend des Bietvorgangs ist ein Fehler aufgetreten. Bitte versuchen sie es sp&auml;ter erneut");
					redirect("versteigerung");
					return;
				}
							
				user.setTemplateVars(t);
				t.set_var( 	"user.konto", 		Common.ln(user.getKonto()),
							"show.highestbid",	1);
			} 
			else {
				t.set_var("show.lowres",1);
			}
		}
		
		redirect("paket");
	}
	
	/**
	 * Ueberweist einen bestimmten Geldbetrag an einen anderen Spieler.
	 * Wenn die Ueberweisung noch nicht explizit bestaetigt wurde, wird die Bestaetigung
	 * erfragt
	 * @urlparam Integer to Die ID des Spielers, der Ziel der Ueberweisung sein soll
	 * @urlparam String ack <code>yes</code> um die Ueberweisung zu bestaetigen
	 * @urlparam Integer count Die zu ueberweisende Geldmenge
	 *
	 */
	public void ueberweisenAction() {
		if( !Faction.get(faction).getPages().hasPage("other") ) {
			redirect();	
			return;
		}
		
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("to");
		int to = getInteger("to");
		
		parameterString("ack");
		String ack = getString("ack");
		
		parameterNumber("count");
		int count = getInteger("count");
		
		if( user.getKonto().compareTo(new BigDecimal(count).toBigInteger()) < 0 ) {
			count = user.getKonto().intValue();
		}
		
		if( count <= 0 ) {
			redirect();
			return;
		}
		
		// Falls noch keine Bestaetigung vorliegt: Bestaetigung der Ueberweisung erfragen
		if( !ack.equals("yes") ) {
			User tmp = getContext().createUserObject( to );
			
			t.set_var(	"show.ueberweisen",			1,
						"ueberweisen.betrag",		Common.ln(count),
						"ueberweisen.betrag.plain",	count,
						"ueberweisen.to.name",		Common._title(tmp.getName()),
						"ueberweisen.to",			tmp.getID() );
			
			return;
		} 

		User tmp = getContext().createUserObject( to );
			
		tmp.transferMoneyFrom( user.getID(), count, "&Uuml;berweisung vom "+Common.getIngameTime(this.ticks));
			
		PM.send(getContext(), user.getID(), tmp.getID(), "RE &uuml;berwiesen", "Ich habe dir soeben "+Common.ln(count)+" RE &uuml;berwiesen");
			
		user.setTemplateVars(t);
		t.set_var( "user.konto", Common.ln(user.getKonto()) );
	
		redirect("other");	
	}

	/**
	 * Aendert den Anzeigetyp fuer Kontotransaktionen
	 * @urlparam Integer type Der neue Anzeigetyp (0-2)
	 *
	 */
	public void showKontoTransactionTypeAction() {
		if( !Faction.get(faction).getPages().hasPage("other") ) {
			redirect();	
			return;
		}
		
		User user = getUser();
		
		parameterNumber("type");
		int type = getInteger("type");
		
		if( (type >= 0) && (type < 3) ) {
			user.setUserValue("TBLORDER/factions/konto_maxtype", Integer.toString(type));
		}
		redirect("other"); 
	}
	
	/**
	 * Zeigt die Seite mit diversen weiteren Infos an
	 *
	 */
	public void otherAction() {
		if( !Faction.get(faction).getPages().hasPage("other") ) {
			redirect();	
			return;
		}
		
		TemplateEngine t = this.getTemplateEngine();
		Database db = getDatabase();
		User user = this.getUser();
		
		t.set_var("show.other",1);

		// ueberweisungen
		t.set_block("_ERSTEIGERN","ueberweisen.listitem","ueberweisen.list");

		UserIterator iter = getContext().createUserIterator("SELECT * FROM users WHERE !LOCATE('hide',flags) AND id!=",user.getID()," ORDER BY id");
		for( User usr : iter ) {
			t.set_var(	"target.id",	usr.getID(),
						"target.name",	Common._title(usr.getName()) );
			t.parse("ueberweisen.list","ueberweisen.listitem",true);
		}
		iter.free();
		
		// Auwahl max. Transaktionstyp in der Kontoanzeige generieren
		int transtype = Integer.parseInt(user.getUserValue("TBLORDER/factions/konto_maxtype"));
		
		
		String newtypetext = "";
		switch(transtype-1 % 3) {
		case 0: 
			newtypetext = "Nur manuelle anzeigen";
			break;
		case 1: 
			newtypetext = "Alle nicht-automatischen anzeigen";
			break;
		case 2:
		default:
			newtypetext = "Alle anzeigen";
			transtype = 2;
		}

		t.set_var(	"konto.newtranstype.name",	newtypetext,
					"konto.newtranstype",		transtype-1 % 3 );
							
		// Kontobewegungen anzeigen
		t.set_block("_UEBER", "moneytransfer.listitem", "moneytransfer.list");
		
		SQLQuery entry = db.query("SELECT * FROM user_moneytransfer WHERE `type`<=",transtype," AND ((`from`=",user.getID(),") OR (`to`=",user.getID(),")) ORDER BY `time` DESC LIMIT 40");
		while( entry.next() ) {
			User player = null;
			
			if( entry.getInt("from") == user.getID() ) {
				player = getContext().createUserObject(entry.getInt("to"));
			}
			else {
				player = getContext().createUserObject(entry.getInt("from"));
			}
			
			t.set_var(	"moneytransfer.time",		Common.date("j.n.Y H:i",entry.getLong("time")),
						"moneytransfer.from",		(entry.getInt("from") == user.getID() ? 1 : 0),
						"moneytransfer.player",		Common._title(player.getName()),
						"moneytransfer.player.id",	player.getID(),
						"moneytransfer.count",		Common.ln(entry.getLong("count")),
						"moneytransfer.reason",		entry.getString("text") );
								
			t.parse("moneytransfer.list", "moneytransfer.listitem", true);
		}
		entry.free();

		// GTU-Preise
		t.set_block("_ERSTEIGERN","kurse.listitem","kurse.list");
		t.set_block("kurse.listitem","kurse.waren.listitem","kurse.waren.list");

		SQLQuery kurse = db.query("SELECT * FROM gtu_warenkurse");
		while( kurse.next() ) {
			Cargo kurseCargo = new Cargo( Cargo.Type.STRING, kurse.getString("kurse") );
			kurseCargo.setOption( Cargo.Option.SHOWMASS, false );
			
			t.set_var(	"posten.name",		kurse.getString("name"),
						"kurse.waren.list",	"" );
								
			ResourceList reslist = kurseCargo.getResourceList();
			for( ResourceEntry res : reslist ) {
				t.set_var(	"ware.image",	res.getImage(),
							"ware.preis",	(res.getCount1()/1000d > 0.05 ? Common.ln(res.getCount1()/1000d):"") );
									
				t.parse("kurse.waren.list","kurse.waren.listitem",true);
			}
			t.parse("kurse.list","kurse.listitem",true);
		}
		kurse.free();
	}
	
	/**
	 * Zeigt die Angebote der Fraktion an
	 *
	 */
	public void angeboteAction() {
		if( !Faction.get(faction).getPages().hasPage("angebote") ) {
			redirect();	
			return;
		}
		
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.set_var("show.angebote",1);
	
		t.set_block("_ERSTEIGERN","angebote.item","angebote.list");
		t.set_block("_ERSTEIGERN","angebote.emptyitem","none");
	
		t.set_var( "none", "" );
						
		int count = 0;
		SQLQuery angebot = db.query("SELECT title,image,description FROM factions_angebote WHERE faction=",this.faction);
		while( angebot.next() ) {
			count++;
			t.set_var(	"angebot.title",		Common._title(angebot.getString("title")),
						"angebot.image",		angebot.getString("image"),
						"angebot.description",	Common._text(angebot.getString("description")), 
						"angebot.linebreak",	(count % 3 == 0 ? "1" : "") );
								
			t.parse("angebote.list","angebote.item",true);
		}
		while( count % 3 > 0 ) {
			count++;
			t.parse("angebote.list","angebote.emptyitem",true);
		}
		angebot.free();
	}
	
	/**
	 * Zeigt das zur Versteigerung angebotene Paket an
	 *
	 */
	public void paketAction() {
		if( !Faction.get(faction).getPages().hasPage("paket") ) {
			redirect();
			return;	
		}
		
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		SQLResultRow paket = db.first("SELECT * FROM versteigerungen_pakete");
		t.set_var( "show.pakete", 1 );

		if( !paket.isEmpty() ) {
			User bieter = getContext().createUserObject(paket.getInt("bieter"));

			String bietername = "";
			
			if( bieter.getID() == Faction.GTU ) {
				bietername = bieter.getName();	
			}
			else if( bieter.getID() == user.getID() ) {
				bietername = bieter.getName();	
			}
			else if( user.getAccessLevel() > 20 ) {
				bietername = bieter.getName();	
			}
			else if( (bieter.getAlly() != 0) && (bieter.getAlly() == user.getAlly()) ) {
				boolean showGtuBieter = db.first("SELECT showGtuBieter FROM ally WHERE id=",bieter.getAlly()).getBoolean("showGtuBieter");

				if( showGtuBieter ) {
					bietername = bieter.getName();	
				}	
			}

			t.set_var(	"paket.id",			paket.getInt("id"),
						"paket.dauer",		paket.getInt("tick")-this.ticks,
						"paket.bieter",		Common._title(bietername),
						"paket.bieter.id",	bieter.getID(),
						"paket.preis",		Common.ln(paket.getLong("preis")) );

			t.set_block("_ERSTEIGERN","paket.reslistitem","paket.reslist");
			t.set_block("_ERSTEIGERN","paket.shiplistitem","paket.shiplist");

			if( paket.getString("cargo").length() > 0 ) {
				Cargo cargo = new Cargo( Cargo.Type.STRING, paket.getString("cargo"));
				cargo.setOption( Cargo.Option.SHOWMASS, false );
				cargo.setOption( Cargo.Option.LARGEIMAGES, true );			

				ResourceList reslist = cargo.getResourceList();
				for( ResourceEntry res : reslist ) {
					t.set_var(	"res.image",		res.getImage(),
								"res.name",			res.getName(),
								"res.fixedsize",	!res.showLargeImages(),
								"res.count",		(res.getCount1() > 1 ? res.getCount1() : 0 ) );
									
					t.parse("paket.reslist","paket.reslistitem",true);
				}
			}

			if( paket.getString("ships").length() > 0 ) {
				int[] shiplist = Common.explodeToInt("|", paket.getString("ships"));
				for( int i=0; i < shiplist.length; i++ ) {
					SQLResultRow shiptype = ShipTypes.getShipType( shiplist[i], false );
					t.set_var(	"ship.type.image",	shiptype.getString("picture"),
								"ship.type.name",	shiptype.getString("nickname"),
								"ship.type",		shiplist[i] );
									
					t.parse("paket.shiplist","paket.shiplistitem",true);
				}
			}
		}
	}
	
	/**
	 * Zeigt die laufenden Versteigerungen an
	 *
	 */
	public void versteigerungAction() {
		TemplateEngine t = this.getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		if( !Faction.get(faction).getPages().hasPage("versteigerung") ) {
			redirect();	
			return;
		}
		
		t.set_var( "show.versteigerungen", 1 );
		t.set_block("_ERSTEIGERN","entry.listitem","entry.list");
		t.set_block("_ERSTEIGERN","gtuzwischenlager.listitem","gtuzwischenlager.list");
		
		/*
			Laufende Handelsvereinbarungen anzeigen 
			(nur solche, die man schon selbst erfuellt hat im Moment)
		*/
		Set<Integer> gzlliste = new HashSet<Integer>();
		
		SQLQuery aentry = db.query("SELECT * FROM gtu_zwischenlager WHERE user1=",user.getID()," OR user2=",user.getID());
		while( aentry.next() ) {
			String owncargoneed = aentry.getString("cargo1need");
			if( aentry.getInt("user2") == user.getID() ) {
				owncargoneed = aentry.getString("cargo2need");
			}
			
			if( new Cargo(Cargo.Type.STRING, owncargoneed).isEmpty() ) {
				gzlliste.add(aentry.getInt("posten"));	
			}
		}
		aentry.free();
		
		for( Integer postenid : gzlliste ) {
			SQLResultRow aposten = db.first("SELECT name,x,y,system FROM ships WHERE id=",postenid);
			t.set_var(	"gtuzwischenlager.name",	Common._plaintitle(aposten.getString("name")),
						"gtuzwischenlager.x",		aposten.getInt("x"),
						"gtuzwischenlager.y",		aposten.getInt("y"),
						"gtuzwischenlager.system",	aposten.getInt("system") );
								
			t.parse("gtuzwischenlager.list", "gtuzwischenlager.listitem", true);
		}

		/*
			Einzelversteigerungen
		*/
		
		Boolean showGtuBieter = null;
		
		SQLQuery entry = db.query("SELECT * FROM versteigerungen ORDER BY id DESC");
		while( entry.next() ) {
			User bieter = getContext().createUserObject( entry.getInt("bieter") );
			
			String entryname = "";
			String entryimage = "";
			String entrylink = "";
			int entrywidth = 0;
			int entryheight = 0;
			long entrycount = 1;
		
			if( entry.getInt("mtype") == 1 ) {	//Schiff
				SQLResultRow shiptype = ShipTypes.getShipType(entry.getInt("type"), false);
				entryname = shiptype.getString("nickname");
				entryimage = shiptype.getString("picture");
				entrylink = Common.buildUrl(getContext(), "default", "module", "schiffinfo", "ship", entry.getInt("type") );
			}
			else if( entry.getInt("mtype") == 2 ) {	// Cargo	
				Cargo cargo = new Cargo( Cargo.Type.STRING, entry.getString("type") );
				cargo.setOption( Cargo.Option.SHOWMASS, false );
				cargo.setOption( Cargo.Option.LARGEIMAGES, true );
				ResourceList reslist = cargo.getResourceList();
				ResourceEntry resource = reslist.iterator().next();

				entryname = Cargo.getResourceName( resource.getId() );
				entryimage = resource.getImage();
			
				if( resource.getId().isItem() ) {
					entrylink = Common.buildUrl(getContext(), "details", "module", "iteminfo", "item", resource.getId().getItemID() );
				}
				else {
					entrylink = "#";	
				}
			
				if( !resource.showLargeImages() ) {
					entrywidth = 50;
					entryheight = 50;
				}
			
				if( resource.getCount1() > 1 ) { 
					entrycount = resource.getCount1();
				}
			}
			String bietername = "";

			if( bieter.getID() == faction ) {
				bietername = bieter.getName();	
			}
			else if( bieter.getID() == user.getID() ) {
				bietername = bieter.getName();
			}
			else if( user.getAccessLevel() > 20 ) {
				bietername = bieter.getName();	
			}
			else if( (bieter.getAlly() != 0) && (bieter.getAlly() == user.getAlly()) ) {
				if( showGtuBieter == null ) {
					SQLResultRow ally = db.first("SELECT showGtuBieter FROM ally WHERE id=",bieter.getAlly());
					if( !ally.isEmpty() ) {
						showGtuBieter = ally.getBoolean("showGtuBieter");
					}
				}
				if( showGtuBieter ) {
					bietername = bieter.getName();	
				}	
			}
			
			String ownername = "";
			
			if( (user.getAccessLevel() >= 20) && (entry.getInt("owner") != faction) && (entry.getInt("owner") != user.getID()) ) {
				User ownerobject = getContext().createUserObject(entry.getInt("owner"));
				ownername = Common._title(ownerobject.getName()); 
			}
			
			t.set_var(	"entry.link",		entrylink,
						"entry.type.name",	StringEscapeUtils.escapeJavaScript(StringUtils.replaceChars(entryname, '"', '\'')),
						"entry.type.image",	entryimage,
						"entry.preis",		Common.ln(entry.getLong("preis")),
						"entry.bieter",		Common._title(bietername),
						"entry.bieter.id",	entry.getInt("bieter"),
						"entry.dauer",		entry.getInt("tick") - this.ticks,
						"entry.aukid",		entry.getInt("id"),
						"entry.width",		entrywidth,
						"entry.height",		entryheight, 
						"entry.count",		entrycount,
						"entry.user.name",	ownername,
						"entry.user.id",	entry.getInt("owner"),
						"entry.user",		(entry.getInt("owner") != faction),
						"entry.ownauction",	(entry.getInt("owner") == user.getID()) );
	
			t.parse("entry.list","entry.listitem",true);
		}
		entry.free();

		t.set_block("_ERSTEIGERN","gtu.dropzones.listitem","gtu.dropzones.list");
		for( StarSystem system : Systems.get() ) {
			if( system.getDropZone() != null ) {
				t.set_var(	"dropzone.system.id",	system.getID(),
							"dropzone.system.name",	system.getName(),
							"dropzone.selected",	(user.getGtuDropZone() == system.getID()) );

				t.parse("gtu.dropzones.list","gtu.dropzones.listitem",true);
			}
		}
	}
	
	/**
	 * Zeigt den Fraktionstext an
	 *
	 */
	public void generalAction() {
		TemplateEngine t = getTemplateEngine();
		
		if( !Faction.get(faction).getPages().hasPage("general") ) {
			redirect();	
			return;
		}
		
		t.set_var(	"show.general",			1,
					"global.faction.text",	Common._text(Faction.get(faction).getPages().getFactionText()) );
		
		return;
	}
	
	static class Router {
		class Result {
			int distance;
			List<SQLResultRow> path = new ArrayList<SQLResultRow>();
		}
		private Map<Integer,Integer> systemInterestLevel = new HashMap<Integer,Integer>();
		private Map<Integer,List<SQLResultRow>> jnlist = new HashMap<Integer,List<SQLResultRow>>();
		
		Router(Map<Integer,List<SQLResultRow>> jns) {
			for( Integer sys : jns.keySet() ) {
				List<SQLResultRow> jnlist = jns.get(sys);
				this.jnlist.put(sys, new ArrayList<SQLResultRow>(jnlist));
			}
		}
		
		Result locateShortestJNPath( int currentsys, int currentx, int currenty, int targetsys, int targetx, int targety) {				
			if( !jnlist.containsKey(currentsys) ) {
				return null;	
			}
	
			if( currentsys == targetsys ) {
				Result res = new Result();
				res.distance = Math.max(Math.abs(targetx-currentx),Math.abs(targety-currenty));
				return res;	
			}
			
			Result shortestpath = null;
			List<SQLResultRow> sysJNList = jnlist.get(currentsys);
			for( int k=0; k < sysJNList.size(); k++ ) {
				SQLResultRow ajn = sysJNList.get(k);
				
				if( systemInterestLevel.containsKey(ajn.getInt("systemout")) && 
					systemInterestLevel.get(ajn.getInt("systemout")) < 0 ) {
					continue;
				}
				int pathcost = Math.max(Math.abs(ajn.getInt("x")-currentx),Math.abs(ajn.getInt("y")-currenty));
				
				sysJNList.remove(k);
				
				Result cost = locateShortestJNPath(ajn.getInt("systemout"),ajn.getInt("xout"),ajn.getInt("yout"), targetsys, targetx, targety );
				if( cost == null ) {
					if( !systemInterestLevel.containsKey(ajn.getInt("systemout")) ) {
						systemInterestLevel.put(ajn.getInt("systemout"), -1);
					}
					continue;
				}
				else if( shortestpath == null ) {
					shortestpath = cost;
					shortestpath.distance += pathcost;
					shortestpath.path.add(0, ajn);
				}
				else if( shortestpath.distance > cost.distance+pathcost ) {
					shortestpath = cost;
					shortestpath.distance += pathcost;
					shortestpath.path.add(0, ajn);
				}
				if( !systemInterestLevel.containsKey(ajn.getInt("systemout")) ) {
					systemInterestLevel.put(ajn.getInt("systemout"), 1);
				}
			}
				
			return shortestpath;
		}
	}
	
	/**
	 * Berechnet die Kosten eines Transportauftrags und speichert ihn in der Datenbank
	 * @urlparam Integer sourcesystem Das Ausgangssystem
	 * @urlparam Integer ganymedeid Die ID der zu transportierenden Ganymede
	 * @urlparam Integer targetsystem Die ID des Zielsystems
	 * @urlparam Integer targetx Die Ziel-X-Koordinate
	 * @urlparam Integer targety Die Ziel-Y-Koordinate
	 * @urlparam Integer transport Sofert der Wert <code>1</code>, wird der Transportauftrag bestaetigt und abgespeichert
	 *
	 */
	public void shopOrderGanymedeSummaryAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();

		if( !Faction.get(faction).getPages().hasPage("shop") ) {
			redirect();	
			return;
		}
		
		SQLResultRow shopentry = db.first("SELECT id FROM factions_shop_entries WHERE faction_id=",this.faction," AND type=2");
		if( shopentry.isEmpty() ) {
			this.redirect();	
			return;
		}
		
		parameterNumber("sourcesystem");
		parameterNumber("ganymedeid");
		parameterNumber("targetsystem");
		parameterNumber("targetx");
		parameterNumber("targety");
		parameterNumber("transport");
		
		int sourcesystem = getInteger("sourcesystem");
		int ganymedeid = getInteger("ganymedeid");
		int targetsystem = getInteger("targetsystem");
		int targetx = getInteger("targetx");
		int targety = getInteger("targety");
		int transport = getInteger("transport");
		
		SQLResultRow gany = db.first("SELECT id,x,y,name FROM ships WHERE id=",ganymedeid," AND owner=",user.getID()," AND type=",ShopGanyTransportEntry.SHIPTYPE_GANYMEDE," AND system=",sourcesystem);
		if( gany.isEmpty() ) {
			addError("Die angegebene Ganymede konnte im Ausgangssystem nicht lokalisiert werden");
			unsetParameter("sourcesystem");
			unsetParameter("ganymedeid");
			
			redirect("shopOrderGanymede");
			return;
		}
		
		SQLResultRow sameorder = db.first("SELECT id FROM factions_shop_orders WHERE user_id=",user.getID()," AND adddata LIKE \"",gany.getInt("id"),"@%\" AND status<4");
		if( !sameorder.isEmpty() ) {
			addError("Es existiert bereits ein Transport-Auftrag f&uuml;r diese Ganymede");
			unsetParameter("sourcesystem");
			unsetParameter("ganymedeid");
			
			redirect("shopOrderGanymede");
			return;
		}
		
		StarSystem system = Systems.get().system(targetsystem);
		if( (system.getAccess() == StarSystem.AC_ADMIN) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) ) {
			addError("Die angegebene Zielsystem konnte nicht lokalisiert werden");
			unsetParameter("targetsystem");
			
			redirect("shopOrderGanymede");
			return;
		} 
		else if( (system.getAccess() == StarSystem.AC_NPC) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) && !user.hasFlag( User.FLAG_VIEW_SYSTEMS ) ) {
			addError("Die angegebene Zielsystem konnte nicht lokalisiert werden");
			unsetParameter("targetsystem");
			
			redirect("shopOrderGanymede");
			return;
		}
		
		if( (targetx < 1) || (targetx > system.getWidth() ) || 
			(targety < 1) || (targety > system.getHeight()) ) {
			addError("Die angegebene Zielkoordinaten konnten im Zielsystem nicht lokalisiert werden");
			unsetParameter("targetx");
			unsetParameter("targety");
			
			redirect("shopOrderGanymede");
			return;		
		}
		
		// Weg finden und Preis ausrechnen
		Map<Integer,List<SQLResultRow>> jumpnodes = new HashMap<Integer,List<SQLResultRow>>();
		Map<Integer,SQLResultRow> jumpnodeindex = new HashMap<Integer,SQLResultRow>();
		SQLQuery jnRow = db.query("SELECT * FROM jumpnodes WHERE hidden=0 AND (systemout!=",sourcesystem," OR system=",sourcesystem,")");
		while( jnRow.next() ) {
			if( !jumpnodes.containsKey(jnRow.getInt("system")) ) {
				jumpnodes.put(jnRow.getInt("system"), new ArrayList<SQLResultRow>());
			}
			jumpnodes.get(jnRow.getInt("system")).add(jnRow.getRow());
			jumpnodeindex.put(jnRow.getInt("id"), jnRow.getRow());
		}
		jnRow.free();
		
		long totalcost = 0;
		Router.Result shortestpath = new Router(jumpnodes).locateShortestJNPath(sourcesystem,gany.getInt("x"),gany.getInt("y"),targetsystem, targetx, targety);
		if( shortestpath == null ) {
			transport = 0;
			t.set_var("transport.price", "<span style=\"color:red\">Kein Weg gefunden</span>");	
		}
		else {
			Map<String,Long> costindex = new HashMap<String,Long>();
			SQLQuery entry = db.query("SELECT * FROM factions_shop_entries WHERE faction_id=",this.faction," AND type=2");
			while( entry.next() ) {
				costindex.put(entry.getString("resource"), entry.getLong("price"));
			}
			entry.free();
			
			Set<Integer> systemlist = new HashSet<Integer>();
			systemlist.add(sourcesystem);
			for( int i=0; i < shortestpath.path.size(); i++ ) {
				SQLResultRow jn = shortestpath.path.get(i);
				systemlist.add(jn.getInt("systemout"));
			}
			
			for( Integer sys : systemlist ) {
				if( costindex.containsKey(Integer.toString(sys)) ) {
					totalcost += costindex.get(Integer.toString(sys));
				}
				else {
					totalcost += costindex.get("*");	
				}	
			}
			
			if( totalcost < 0 ) {
				totalcost = 0;
			}
			
			if( user.getKonto().compareTo(new BigDecimal(totalcost).toBigInteger()) >= 0 ) {
				t.set_var(	"transport.price",			Common.ln(totalcost)+" RE",
							"transport.enableOrder",	1 );
			}
			else {
				transport = 0;
				t.set_var("transport.price", "<span style=\"color:red\">"+Common.ln(totalcost)+" RE</span>");
			}
		}
		
		if( transport == 0 ) {
			t.set_var(	"show.shopOrderGanymedeSummary",	1,
						"ganymede.id",		gany.getInt("id"),
						"ganymede.name",	gany.getString("name"),
						"source.system",	sourcesystem,
						"source.x",			gany.getInt("x"),
						"source.y",			gany.getInt("y"),
						"target.system",	targetsystem,
						"target.x",			targetx,
						"target.y",			targety );
		}
		else {
			db.tBegin();
	
			User faction = getContext().createUserObject( this.faction );
			faction.transferMoneyFrom( user.getID(), totalcost, "&Uuml;berweisung Bestellung #ganytransXX"+gany.getInt("id"));	
		
			StringBuilder waypoints = new StringBuilder(300);
			waypoints.append("Start: "+sourcesystem+":"+gany.getInt("x")+"/"+gany.getInt("y")+"\n");
			for( int i=0; i < shortestpath.path.size(); i++ ) {
				SQLResultRow jn = shortestpath.path.get(i);
				
				waypoints.append(jn.getInt("system")+":"+jn.getInt("x")+"/"+jn.getInt("y")+" -> ");
				waypoints.append(jn.getInt("systemout")+":"+jn.getInt("xout")+"/"+jn.getInt("yout")+" ");
				waypoints.append("[ID: "+jn.getInt("id")+"]\n");	
			}		
			waypoints.append("Ziel: "+targetsystem+":"+targetx+"/"+targety+"\n");
		
			PM.send(getContext(), user.getID(), this.faction, "[auto] Shop-Bestellung [Ganymede]", "Besteller: [userprofile="+user.getID()+"]"+user.getName()+" ("+user.getID()+")[/userprofile]\nObjekt: "+gany.getString("name")+" ("+gany.getInt("id")+")\nPreis: "+Common.ln(totalcost)+"\nZeitpunkt: "+Common.date("d.m.Y H:i:s")+"\n\n[b][u]Pfad[/u][/b]:\n"+waypoints);
		
			String adddataStr = gany.getInt("id")+"@"+sourcesystem+":"+gany.getInt("x")+"/"+gany.getInt("y")+"->"+targetsystem+":"+targetx+"/"+targety;
			
			PreparedQuery stmt = db.prepare("INSERT INTO factions_shop_orders " ,
					"(shopentry_id,user_id,price,date,adddata) VALUES " ,
					"( ?, ?, ?, ?, ?)");
			stmt.update(shopentry, user.getID(), totalcost, Common.time(), adddataStr);
	
			Taskmanager taskmanager = Taskmanager.getInstance();
			taskmanager.addTask( Taskmanager.Types.GANY_TRANSPORT, 1, Integer.toString(stmt.insertID()), "", "" );
			
			stmt.close();
	
			if( !db.tCommit() ) {
				addError("Die Bestellung konnte nicht korrekt verarbeitet werden. Bitte versuchen sie es erneut.");
				redirect("shop");
				return;	
			}
		
			t.set_var("show.message", "Bestellung &uuml;ber 1 Ganymede-Transport des Objekts "+gany.getInt("id")+" von "+sourcesystem+":"+gany.getInt("x")+"/"+gany.getInt("y")+" nach "+targetsystem+":"+targetx+"/"+targety+" f&uuml;r "+Common.ln(totalcost)+" erhalten und vom System best&auml;tigt.<br />Einen angenehmen Tag noch!");
			
		
			redirect("shop");
		
		}
	}
	
	/**
	 * Zeigt die GUI zur Erstellung eines Ganymede-Transportauftrags
	 * @urlparam Integer sourcesystem Das Ausgangssystem
	 * @urlparam Integer ganymedeid Die ID der zu transportierenden Ganymede
	 * @urlparam Integer targetsystem Die ID des Zielsystems
	 * @urlparam Integer targetx Die Ziel-X-Koordinate
	 * @urlparam Integer targety Die Ziel-Y-Koordinate
	 *
	 */
	public void shopOrderGanymedeAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();

		if( !Faction.get(faction).getPages().hasPage("shop") ) {
			redirect();	
			return;
		}
		
		SQLResultRow shopentry = db.first("SELECT id FROM factions_shop_entries WHERE faction_id=",this.faction," AND type=2");
		if( shopentry.isEmpty() ) {
			this.redirect();	
			return;
		}
		
		parameterNumber("sourcesystem");
		parameterNumber("ganymedeid");
		parameterNumber("targetsystem");
		parameterNumber("targetx");
		parameterNumber("targety");
		
		int sourcesystem = getInteger("sourcesystem");
		
		// Wenn alle Parameter eingegebene wurden -> shopOrderGanymedeSummary
		if( getInteger("targetsystem") != 0 && getInteger("ganymedeid") != 0 &&
			getInteger("targetx") != 0 && getInteger("targety") != 0 && sourcesystem != 0 ) {
				
			redirect("shopOrderGanymedeSummary");
			return;				
		}
		
		t.set_block("_ERSTEIGERN", "ganytrans.sourcesystem.listitem", "ganytrans.sourcesystem.list");
		t.set_block("_ERSTEIGERN", "ganytrans.ganymedes.listitem", "ganytrans.ganymedes.list");
		t.set_block("_ERSTEIGERN", "ganytrans.targetsystem.listitem", "ganytrans.targetsystem.list");
		t.set_var("show.shopOrderGanymede", 1);
		
			
		// Dummyeintrag (Ausgangssysteme)
		t.set_var(	"sourcesystem.id",		0,
					"sourcesystem.name",	"-" );					
		t.parse("ganytrans.sourcesystem.list", "ganytrans.sourcesystem.listitem", true);
		
		// Liste aller bereits mit einem Transport-Auftrag ausgestatteten Ganys generieren
		Set<Integer> blockedganylist = new HashSet<Integer>();
		List<Integer> blockedganysqlList = new ArrayList<Integer>();
		
		SQLQuery adddata = db.query("SELECT t1.adddata FROM factions_shop_orders t1 JOIN factions_shop_entries t2 ON t1.shopentry_id=t2.id WHERE t1.user_id=",user.getID()," AND t1.status<4 AND t2.type=2");
		while( adddata.next() ) {
			String[] tmp = StringUtils.split(adddata.getString("adddata"), "@");
			int ganyid = Integer.parseInt(tmp[0]);
			
			blockedganylist.add(ganyid);
			blockedganysqlList.add(ganyid);
		}
		adddata.free();
		
		String blockedganysql = "";
		if( blockedganysqlList.size() > 0 ) {
			blockedganysql = "AND !(id IN ("+Common.implode(",",blockedganysqlList)+"))";
		}
			
		boolean first = true;
		SQLQuery asystem = db.query("SELECT system FROM ships WHERE type=",ShopGanyTransportEntry.SHIPTYPE_GANYMEDE," AND owner=",user.getID()," ",blockedganysql," GROUP BY system ORDER BY system");
		while( asystem.next() ) {
			if( sourcesystem == asystem.getInt("system") ) {
				t.set_var("sourcesystem.selected", 1);
				first = false;
			}
			else {
				t.set_var("sourcesystem.selected", 0);
			}

			t.set_var(	"sourcesystem.id",	asystem.getInt("system"),
						"sourcesystem.name",	Systems.get().system(asystem.getInt("system")).getName() );
									
			t.parse("ganytrans.sourcesystem.list", "ganytrans.sourcesystem.listitem", true);
		}
		asystem.free();
		
		// Check, ob ein System ausgewaehlt wurde.
		// Wenn nicht -> Ende
		if( first || sourcesystem == 0 ) {
			return;	
		}
		
		t.set_var("sourcesystem.known", 1);
		
		// Moegliche Ganymedes ausgeben
		first = true;
		SQLQuery agany = db.query("SELECT id,name FROM ships WHERE type=",ShopGanyTransportEntry.SHIPTYPE_GANYMEDE," AND owner=",user.getID()," AND system=",sourcesystem," ORDER BY x+y");
		while( agany.next() ) {
			if( blockedganylist.contains(agany.getInt("id")) ) {
				continue;
			}
			
			if( first ) {
				t.set_var("ganymede.selected", 1);
				first = false;
			}
			else {
				t.set_var("ganymede.selected", 0);
			}
			t.set_var(	"ganymede.id",		agany.getInt("id"),
						"ganymede.name",	Common._plaintitle(agany.getString("name")) );
									
			t.parse("ganytrans.ganymedes.list", "ganytrans.ganymedes.listitem", true);
		}
		agany.free();
		
		// Zielsysteme ausgeben
		first = true;
		for( StarSystem system : Systems.get() ) {
			if( (system.getAccess() == StarSystem.AC_ADMIN) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) ) {
				continue;
			} 
			else if( (system.getAccess() == StarSystem.AC_NPC) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) && !user.hasFlag( User.FLAG_VIEW_SYSTEMS ) ) {
				continue;
			}
		
			if( first ) {
				t.set_var("targetsystem.selected", 1);
				first = false;
			}
			else {
				t.set_var("targetsystem.selected", 0);
			}
			
			t.set_var(	"targetsystem.id",		system.getID(),
						"targetsystem.name",	system.getName() );
									
			t.parse("ganytrans.targetsystem.list", "ganytrans.targetsystem.listitem", true);
		}
	}
	
	/**
	 * Fuehrt eine Bestellung im Shop aus. Der User muss dazu eine gewuenschte Lieferposition angeben.
	 * Wenn diese noch nicht angegeben wurde, wird sie erfragt
	 * 
	 * @urlparam Integer shopentry Die ID des Shopeintrags, der bestellt werden soll
	 * @urlparam Integer ordercount Die Liefermenge
	 * @urlparam Integer ordersys Das Liefersystem
	 * @urlparam Integer orderx Die X-Komponente der Lieferkoordinate
	 * @urlparam Integer ordery Die Y-Komponente der Lieferkoordinate
	 *
	 */
	public void shopOrderAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		if( !Faction.get(faction).getPages().hasPage("shop") ) {
			redirect();	
			return;
		}
		
		parameterNumber("shopentry");
		parameterNumber("ordercount");
		
		int shopentryID = getInteger("shopentry");
		int ordercount = getInteger("ordercount");
		
		SQLResultRow shopentry = db.first("SELECT * FROM factions_shop_entries WHERE id=",shopentryID);
		if( shopentry.isEmpty() ) {
			t.set_var("show.message", "<span style=\"color:red\">Es existiert kein passendes Angebot</span>");
			redirect("shop");
			return;	
		}
		
		// Ganymed-Transporte verarbeiten
		if( shopentry.getInt("type") == 2 ) {
			redirect("shopOrderGanymede");
			
			return;	
		}
		
		if( ordercount < 1 ) {
			redirect("shop");
			return;	
		}
		
		parameterNumber("ordersys");
		parameterNumber("orderx");
		parameterNumber("ordery");
		
		int ordersys = getInteger("ordersys");
		int orderx = getInteger("orderx");
		int ordery = getInteger("ordery");
		
		ShopEntry entry = null;
		
		if( shopentry.getInt("type") == 1 ) {	//Schiff
			entry = new ShopShipEntry(shopentry);
		}
		else if( shopentry.getInt("type") == 0 ) {	// Cargo	
			entry = new ShopResourceEntry(shopentry);
		}
		
		if( user.getKonto().compareTo(new BigDecimal(entry.getPrice()*ordercount).toBigInteger()) < 0 ) {
			t.set_var("show.message", "<span style=\"color:red\">Sie verf&uuml;gen nicht &uuml;ber genug Geld</span>");
			redirect("shop");
			return;	
		}
		
		if( ordersys == 0 || orderx == 0 || ordery == 0 ) {
			t.set_var(	"show.shopOrderLocation",	1,
						"order.count",				ordercount,
						"order.name",				entry.getName(),
						"order.entry",				entry.getID() );
		}
		else {		
			db.tBegin();
			db.update("INSERT INTO factions_shop_orders " ,
					"(shopentry_id,user_id,count,price,date,adddata) VALUES " ,
					"(",entry.getID(),",",user.getID(),",",ordercount,",",(ordercount*entry.getPrice()),",",Common.time(),",'",ordersys+":"+orderx+"/"+ordery+"')");
			
			User faction = getContext().createUserObject( this.faction );
			faction.transferMoneyFrom( user.getID(), entry.getPrice()*ordercount, "&Uuml;berweisung Bestellung #"+entry.getType()+entry.getResource()+"XX"+ordercount);	
			
			PM.send(getContext(), user.getID(), this.faction, "[auto] Shop-Bestellung", "Besteller: [userprofile="+user.getID()+"]"+user.getName()+" ("+user.getID()+")[/userprofile]\nObjekt: "+entry.getName()+"\nMenge: "+ordercount+"\nLieferkoordinaten: "+ordersys+":"+orderx+"/"+ordery+"\nZeitpunkt: "+Common.date("d.m.Y H:i:s"));
			if( !db.tCommit() ) {
				addError("Die Bestellung konnte nicht korrekt verarbeitet werden. Bitte versuchen sie es erneut.");
				redirect("shop");
				return;	
			}
			
			t.set_var("show.message", "Bestellung &uuml;ber "+ordercount+"x "+entry.getName()+" f&uuml;r "+Common.ln(entry.getPrice()*ordercount)+" erhalten und vom System best&auml;tigt.<br />Sollten noch R&uuml;ckfragen bestehend so wird sich ein Sachbearbeiter bei ihnen melden.<br />Einen angenehmen Tag noch!");
			
			redirect("shop");
		}
	}
	
	/**
	 * Aendert die Verfuegbarkeit eines Shopeintrags
	 * @urlparam Integer shopentry Die ID des Shopeintrags
	 * @urlparam Integer availability Die neue Verfuegbarkeit
	 *
	 */
	public void shopChangeAvailabilityAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		if( !Faction.get(faction).getPages().hasPage("shop") ) {
			redirect();	
			return;
		}
		
		if( this.faction == user.getID() ) {
			parameterNumber("shopentry");
			parameterNumber("availability");
			
			int shopentryID = getInteger("shopentry");
			int availability = getInteger("availability");
			
			SQLResultRow shopentry = db.first("SELECT * FROM factions_shop_entries WHERE faction_id=",this.faction," AND id=",shopentryID);
			if( shopentry.isEmpty() ) {
				addError("Es konnte kein passender Shopeintrag gefunden werden");
				redirect("shop");
				return;	
			}
			
			if( availability < 0 || availability > 2 ) {
				addError("Ung&uuml;ltiger Status");
				redirect("shop");
				return;
			}
			
			db.query("UPDATE factions_shop_entries SET availability=",availability," WHERE id=",shopentry.getInt("id"));
			
			t.set_var("show.message", "Neuer Status erfolgreich zugewiesen");
		}
		redirect("shop");
	}
	
	/**
	 * Aendert den Auftragsstatus einer Bestellung
	 * @urlparam Integer orderentry Die ID des Auftrags
	 * @urlparam Integer orderstatus Der neue Auftragsstatus
	 *
	 */
	public void changeShopOrderStatusAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		if( !Faction.get(faction).getPages().hasPage("shop") ) {
			redirect();	
			return;
		}
		
		if( this.faction == user.getID() ) {
			parameterNumber("orderstatus");
			parameterNumber("orderentry");
			
			int orderstatus = getInteger("orderstatus");
			int orderentryID = getInteger("orderentry");
			
			SQLResultRow orderentry = db.first("SELECT t1.* FROM factions_shop_orders t1 JOIN factions_shop_entries t2 ON t1.shopentry_id=t2.id WHERE t2.faction_id=",this.faction," AND t1.status<4 AND t1.id=",orderentryID);
			if( orderentry.isEmpty() ) {
				addError("Es konnte kein passender Ordereintrag gefunden werden");
				redirect("shop");
				return;	
			}
			
			if( orderstatus < 0 || orderstatus > 4 ) {
				addError("Ung&uuml;ltiger Status");
				redirect("shop");
				return;
			}
			
			db.update("UPDATE factions_shop_orders SET status=",orderstatus," WHERE id=",orderentry.getInt("id"));
			
			t.set_var("show.message", "Neuer Status erfolgreich zugewiesen");
		}
		redirect("shop");
	}
	
	private String getStatusColor(int status) {
		switch(status) {
		case 0:
			return "#DDDD00";
		case 1:
			return "#4477DD";
		case 2:
			return "#55DD55";
		case 3:
			return "#CC2222";
		}
		return "";
	}
	
	private String getStatusName(int status) {
		switch(status) {
		case 0:
			return "&nbsp;&nbsp;Neu&nbsp;&nbsp;";
		case 1:
			return "In Bearbeitung";
		case 2:
			return "Auslieferung";
		case 3:
			return "Nicht verf&uuml;gbar";
		}
		return "";
	}
	
	/**
	 * Zeigt den Shop der Fraktion an
	 *
	 */
	public void shopAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		if( !Faction.get(faction).getPages().hasPage("shop") ) {
			redirect();	
			return;
		}			
		
		t.set_var( "show.shop", 1 );
		
		t.set_block("_ERSTEIGERN", "shop.listitem", "shop.list");
		t.set_block("_ERSTEIGERN", "shop.orderlist.listitem", "shop.orderlist.list");
		t.set_block("_ERSTEIGERN", "shop.shopownerlist.listitem", "shop.shopownerlist.list");
	
		if( this.faction != user.getID() ) {									
			SQLQuery orderentry = db.query("SELECT t1.* FROM factions_shop_orders t1 JOIN factions_shop_entries t2 ON t1.shopentry_id=t2.id WHERE t2.faction_id=",this.faction," AND t1.user_id=",user.getID()," AND t1.status<4");
			while( orderentry.next() ) {
				// Keine so geschickte loesung, es reicht aber fuer den moment:
				SQLResultRow shopentry = db.first("SELECT * FROM factions_shop_entries WHERE id=",orderentry.getInt("shopentry_id"));
				ShopEntry shopEntryObj = null;
				
				String entryadddata = "";
				if( shopentry.getInt("type") == 1 ) {	//Schiff
					shopEntryObj = new ShopShipEntry(shopentry);
				}
				else if( shopentry.getInt("type") == 0 ) {	// Cargo	
					shopEntryObj = new ShopResourceEntry(shopentry);
				}
				else if( shopentry.getInt("type") == 2 ) {	//Ganytransport
					shopEntryObj = new ShopGanyTransportEntry(new SQLResultRow[] {shopentry});
					
					String[] tmp = StringUtils.split(orderentry.getString("adddata"), "@");
					int ganyid = Integer.parseInt(tmp[0]);
						
					String ganyname = Common._plaintitle(db.first("SELECT name FROM ships WHERE id=",ganyid).getString("name"));

					String[] coords = StringUtils.split(tmp[1], "->");
					entryadddata = ganyname+" ("+ganyid+")<br />nach "+coords[1];
				}
			
				t.set_var(	"orderentry.name",			shopEntryObj.getName(),
							"orderentry.adddata",		entryadddata,
							"orderentry.type.image",	shopEntryObj.getImage(),
							"orderentry.link",			shopEntryObj.getLink(),
							"orderentry.id",			orderentry.getInt("id"),
							"orderentry.price",			Common.ln(orderentry.getLong("price")),
							"orderentry.count",			Common.ln(orderentry.getInt("count")),
							"orderentry.status",		getStatusName(orderentry.getInt("status")),
							"orderentry.bgcolor",		getStatusColor(orderentry.getInt("status")) );
			
				t.parse("shop.orderlist.list", "shop.orderlist.listitem", true);
			}
			orderentry.free();
		}
		else {						
			t.set_var("shop.owner", 1);
			
			SQLQuery orderentry = db.query("SELECT t1.*,IF(!t1.status,t1.status,t1.date) as orderprio FROM factions_shop_orders t1 JOIN factions_shop_entries t2 ON t1.shopentry_id=t2.id WHERE t2.faction_id=",this.faction," AND t1.status<4 ORDER BY orderprio ASC");
			while( orderentry.next() ) {
				// Keine so geschickte loesung, es reicht aber fuer den moment:
				SQLResultRow shopentry = db.first("SELECT * FROM factions_shop_entries WHERE id=",orderentry.getInt("shopentry_id"));
				ShopEntry shopEntryObj = null;
			
				String entryadddata = "";
				if( shopentry.getInt("type") == 1 ) {	//Schiff
					shopEntryObj = new ShopShipEntry(shopentry);

					entryadddata = "LK: "+orderentry.getString("adddata");
				}
				else if( shopentry.getInt("type") == 0 ) {	// Cargo	
					shopEntryObj = new ShopResourceEntry(shopentry);
					entryadddata = "LK: "+orderentry.getString("adddata");
				}
				else if( shopentry.getInt("type") == 2 ) {	//Ganytransport
					String[] tmp = StringUtils.split(orderentry.getString("adddata"), "@");
					int ganyid = Integer.parseInt(tmp[0]);
					
					String[] coords = StringUtils.split(tmp[1], "->");
					
					entryadddata = ganyid+"<br />"+coords[0]+" - "+coords[1];
					shopEntryObj = new ShopGanyTransportEntry(new SQLResultRow[] {shopentry});
				}
				
				User ownerobj = getContext().createUserObject(orderentry.getInt("user_id"));
				
				t.set_var(	"orderentry.name",		shopEntryObj.getName(),
							"orderentry.adddata",	entryadddata,
							"orderentry.owner",		orderentry.getInt("user_id"),
							"orderentry.owner.name",	Common._title(ownerobj.getName()),
							"orderentry.link",		shopEntryObj.getLink(),
							"orderentry.id",		orderentry.getInt("id"),
							"orderentry.price",		Common.ln(orderentry.getLong("price")),
							"orderentry.count",		Common.ln(orderentry.getInt("count")),
							"orderentry.status",	orderentry.getInt("status"),
							"orderentry.status.name",	getStatusName(orderentry.getInt("status")),
							"orderentry.bgcolor",		getStatusColor(orderentry.getInt("status")) );
			
				t.parse("shop.shopownerlist.list", "shop.shopownerlist.listitem", true);
			}
			orderentry.free();
		}
		
		// Zuerst alle Ganymed-Transportdaten auslesen
		
		SQLQuery shopentry = db.query("SELECT * FROM factions_shop_entries WHERE faction_id=",this.faction," AND type=2");
		
		SQLResultRow[] ganytransport = new SQLResultRow[shopentry.numRows()];
		int i=0;
		
		while( shopentry.next() ) {
			ganytransport[i++] = shopentry.getRow();
		}
		shopentry.free();
		
		// Falls vorhanden jetzt eine Ganymed-Infozeile ausgeben
		if( ganytransport.length > 0 ) {
			ShopEntry shopEntryObj = new ShopGanyTransportEntry(ganytransport);

			t.set_var(	"entry.type.image",			shopEntryObj.getImage(),
						"entry.name",				shopEntryObj.getName(),
						"entry.link",				shopEntryObj.getLink(),
						"entry.id",					shopEntryObj.getID(),
						"entry.availability.name",	shopEntryObj.getAvailabilityName(),
						"entry.availability.color",	shopEntryObj.getAvailabilityColor(),
						"entry.availability",		shopEntryObj.getAvailability(),
						"entry.price",				shopEntryObj.getPriceAsText(),
						"entry.showamountinput",	shopEntryObj.showAmountInput() );
		
			t.parse("shop.list", "shop.listitem", true);
		}
		
		// Nun den normalen Shop ausgeben
		shopentry = db.query("SELECT * FROM factions_shop_entries WHERE faction_id=",this.faction," AND type!=2");
		while( shopentry.next() ) {		
			ShopEntry shopEntryObj = null;
			if( shopentry.getInt("type") == 1 ) {
				shopEntryObj = new ShopShipEntry(shopentry.getRow());
			}
			else if( shopentry.getInt("type") == 0 ) {
				shopEntryObj = new ShopResourceEntry(shopentry.getRow());
			}
			
			t.set_var(	"entry.type.image",			shopEntryObj.getImage(),
						"entry.name",				shopEntryObj.getName(),
						"entry.link",				shopEntryObj.getLink(),
						"entry.id",					shopEntryObj.getID(),
						"entry.availability.name",	shopEntryObj.getAvailabilityName(),
						"entry.availability.color",	shopEntryObj.getAvailabilityColor(),
						"entry.availability",		shopEntryObj.getAvailability(),
						"entry.price",				shopEntryObj.getPriceAsText(),
						"entry.showamountinput",	shopEntryObj.showAmountInput() );
			
			t.parse("shop.list", "shop.listitem", true);
		}
		shopentry.free();
	}
	
	/**
	 * Leitet zur Default-Seite einer Fraktion weiter
	 */
	@Override
	public void defaultAction() {
		this.redirect(Faction.get(faction).getPages().getFirstPage());
		
		return;
	}
}
