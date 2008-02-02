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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.JumpNodeRouter;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Zeigt die Fraktionsseiten an
 * @author Christopher Jung
 * 
 * @urlparam Integer faction Die ID der anzuzeigenden Fraktion
 *
 */
public class ErsteigernController extends TemplateGenerator {
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
			return Common.buildUrl("default", "module", "schiffinfo", "ship", shiptype.getInt("id") );
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
				return Common.buildUrl("details", "module", "iteminfo", "item", resourceEntry.getId().getItemID());
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
		User user = (User)getUser();
		
		// Ausgewaehlte Fraktion ueberpruefen und deren Menueeintraege freischalten
		int faction = this.getInteger("faction");
		User.Relations relationlist = user.getRelations();
		
		if( faction == 0 ) {
			if( Faction.get(user.getId()) != null ) {
				faction = user.getId();
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
			t.setVar("faction."+aPage,1);	
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
			User aFactionUser = (User)getDB().get(User.class, factionObj.getID());
			
			if( (user.getRelation(factionObj.getID()) == User.Relation.ENEMY) ||
				(relationlist.fromOther.get(factionObj.getID()) == User.Relation.ENEMY) ) {
				factionmenu.append("<span style='color:red;font-size:14px'>"+StringUtils.replaceChars(Common._title(aFactionUser.getName()), '"', '\'')+"</span><br />");	
			}
			else {	
				factionmenu.append("<a style='font-size:14px' class='profile' href='"+Common.buildUrl("default", "faction", factionObj.getID())+"'>"+StringUtils.replaceChars(Common._title(aFactionUser.getName()), '"', '\'')+"</a><br />");
			}
		}
		factionmenu.append( StringUtils.replaceChars(Common.tableEnd(), '"', '\'') );
		String factionmenuStr = StringEscapeUtils.escapeJavaScript(StringUtils.replace(StringUtils.replace(factionmenu.toString(), "<", "&lt;"), ">", "&gt;"));
		
		User factionuser = (User)getDB().get(User.class, faction);
		
		t.setVar(	"user.konto",			Common.ln(user.getKonto()),
					"global.faction",		faction,
					"global.faction.name",	Common._title(factionuser.getName()),
					"global.menusize",		pages.getMenuSize(),
					"global.factionmenu",	factionmenuStr );
		
		this.ticks = getContext().get(ContextCommon.class).getTick();
		
		SQLResultRow paket = db.first("SELECT id FROM versteigerungen_pakete");
		t.setVar("gtu.paket", !paket.isEmpty());
					
		return true;
	}
	
	/**
	 * Aendert das System, in dem ersteigerte Dinge gespawnt werden sollen
	 * @urlparam Integer favsys Die ID des neuen Systems, in dem ersteigerte Dinge gespawnt werden sollen
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void changeDropZoneAction() {
		if( !Faction.get(faction).getPages().hasPage("versteigerung") ) {
			redirect();
			return;	
		}
		
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("favsys");
		int favsys = getInteger("favsys");
		
		if( Systems.get().system(favsys).getDropZone() != null ) {
			user.setGtuDropZone( favsys );
			t.setVar("show.newcoords",1);
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
	@Action(ActionType.DEFAULT)
	public void bidEntryAction() {
		if( !Faction.get(faction).getPages().hasPage("versteigerung") ) {
			redirect();
			return;	
		}
		
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		parameterNumber("bid");
		int bid = getInteger("bid");
		
		parameterNumber("auk");
		int auk = getInteger("auk");
		
		SQLResultRow entry = db.first("SELECT * FROM versteigerungen WHERE id=",auk);
		
		if( entry.isEmpty() || (entry.getInt("owner") == user.getId()) ) {
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
				entrylink = Common.buildUrl("default", "module", "schiffinfo", "ship", entry.getInt("type") );
			}
			else if( entry.getInt("mtype") == 2 ) {	// Artefakt
				Cargo cargo = new Cargo( Cargo.Type.STRING, entry.getString("type"));
				cargo.setOption( Cargo.Option.SHOWMASS, false );
				cargo.setOption( Cargo.Option.LARGEIMAGES, true );
				ResourceEntry resource = cargo.getResourceList().iterator().next();
							
				entryname = Cargo.getResourceName( resource.getId() );
				entryimage = resource.getImage();
			
				if( resource.getId().isItem() ) {
					entrylink = Common.buildUrl("details", "module", "iteminfo", "item", resource.getId().getItemID() );
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

			User bieter = (User)getDB().get(User.class,  entry.getInt("bieter") );
			
			if( bieter.getId() == faction ) {
				bietername = bieter.getName();	
			}
			else if( bieter.getId() == user.getId() ) {
				bietername = bieter.getName();
			}
			else if( user.getAccessLevel() > 20 ) {
				bietername = bieter.getName();	
			}
			else if( (bieter.getAlly() != 0) && (bieter.getAlly() == user.getAlly()) ) {
				boolean showGtuBieter = db.first("SELECT showGtuBieter FROM ally WHERE id=",bieter.getAlly()).getInt("showGtuBieter") != 0;

				if( showGtuBieter ) {
					bietername = bieter.getName();	
				}	
			}
			
			long cost = entry.getLong("preis")+(long)(entry.getLong("preis")/20d);
			if( cost == entry.getLong("preis") ) {
				cost++;
			}

			t.setVar(	"show.bid.entry",	1,
						"entry.type.name",	StringEscapeUtils.escapeJavaScript(StringUtils.replaceChars(entryname, '"', '\'')),
						"entry.type.image",	entryimage,
						"entry.link",		entrylink,
						"entry.width",		entrywidth,
						"entry.height",		entryheight,
						"entry.count",		entrycount,
						"bid.player",		Common._title(bietername),
						"bid.player.id",	bieter.getId(),
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
					User bieter = (User)getDB().get(User.class, entry.getInt("bieter"));
						
					PM.send(getContext(), faction, entry.getInt("bieter"), "Bei Versteigerung &uuml;berboten", 
							"Sie wurden bei der Versteigerung um '"+entryname+"' &uuml;berboten. Die von ihnen gebotenen RE in H&ouml;he von "+Common.ln(entry.getLong("preis"))+" wurden auf ihr Konto zur&uuml;ck&uuml;berwiesen.\n\nGaltracorp Unlimited");
					 
				 	bieter.transferMoneyFrom( faction, entry.getLong("preis"), "R&uuml;ck&uuml;berweisung Gebot #2"+entry.getInt("id")+" '"+entryname+"'", false, User.TRANSFER_SEMIAUTO);
				}
					
				db.tUpdate(1, "UPDATE versteigerungen " +
						"SET tick=",entry.getInt("tick") <= ticks+3 ? ticks+3 : entry.getInt("tick"),",bieter=",user.getId(),",preis=",bid," " +
						"WHERE id=",auk," AND tick=",entry.getInt("tick")," AND bieter=",entry.getInt("bieter")," AND preis=",entry.getInt("preis"));
					
				User gtu = (User)getDB().get(User.class,  faction );
				gtu.transferMoneyFrom( user.getId(), bid, "&Uuml;berweisung Gebot #2"+entry.getInt("id")+" '"+entryname+"'", false, User.TRANSFER_SEMIAUTO);
				
				if( !db.tCommit() ) {
					addError("W&auml;hrend des Bietvorgangs ist ein Fehler aufgetreten. Bitte versuchen sie es sp&auml;ter erneut");
					redirect("versteigerung");
					return;
				}
				
				user.setTemplateVars(t);
				t.setVar( 	"user.konto", 		Common.ln(user.getKonto()),
							"show.highestbid",	1);
			}
			else {
				t.setVar("show.lowres",1);
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
	@Action(ActionType.DEFAULT)
	public void bidPaketAction() {
		if( !Faction.get(faction).getPages().hasPage("paket") ) {
			redirect();
			return;	
		}
		
		User user = (User)getUser();
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
			t.setVar(	"show.bid.paket",	1,
						"bid.price",		cost,
						"bid.id",			auk );

			return;
		} 
		// Gebot bestaetigt -> Versteigerung aktuallisieren
		else if( bid > 0 ) {		
			if( (bid >= cost) && (user.getKonto().compareTo(new BigDecimal(bid).toBigInteger()) >= 0 ) ) {
				db.tBegin();
				
				if( paket.getInt("bieter") != faction ) {
					User bieter = (User)getDB().get(User.class, paket.getInt("bieter"));
						
					PM.send(getContext(), faction, bieter.getId(), "Bei Versteigerung um das GTU-Paket &uuml;berboten", 
							"Sie wurden bei der Versteigerung um das GTU-Paket &ueberboten. Die von ihnen gebotenen RE in H&ouml;he von "+Common.ln(paket.getLong("preis"))+" wurden auf ihr Konto zur&uuml;ck&uuml;berwiesen.\n\nGaltracorp Unlimited");
					 
				 	bieter.transferMoneyFrom( faction, paket.getLong("preis"), "R&uuml;ck&uuml;berweisung Gebot #9"+paket.getInt("id")+" 'GTU-Paket'", false, User.TRANSFER_SEMIAUTO);
				}
				
				db.tUpdate(1, "UPDATE versteigerungen_pakete " +
						"SET tick=",paket.getInt("tick") <= ticks+3 ? ticks+3 : paket.getInt("tick"),",bieter=",user.getId(),",preis=",bid," " +
						"WHERE id=",auk," AND preis=",paket.getLong("preis")," AND bieter=",paket.getInt("bieter")," AND tick=",paket.getInt("tick"));
				
				User gtu = (User)getDB().get(User.class,  faction );
				gtu.transferMoneyFrom( user.getId(), bid, "&Uuml;berweisung Gebot #9"+auk+" 'GTU-Paket'", false, User.TRANSFER_SEMIAUTO);
				
				if( !db.tCommit() ) {
					addError("W&auml;hrend des Bietvorgangs ist ein Fehler aufgetreten. Bitte versuchen sie es sp&auml;ter erneut");
					redirect("versteigerung");
					return;
				}
							
				user.setTemplateVars(t);
				t.setVar( 	"user.konto", 		Common.ln(user.getKonto()),
							"show.highestbid",	1);
			} 
			else {
				t.setVar("show.lowres",1);
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
	@Action(ActionType.DEFAULT)
	public void ueberweisenAction() {
		if( !Faction.get(faction).getPages().hasPage("other") ) {
			redirect();	
			return;
		}
		
		User user = (User)getUser();
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
			User tmp = (User)getDB().get(User.class,  to );
			
			t.setVar(	"show.ueberweisen",			1,
						"ueberweisen.betrag",		Common.ln(count),
						"ueberweisen.betrag.plain",	count,
						"ueberweisen.to.name",		Common._title(tmp.getName()),
						"ueberweisen.to",			tmp.getId() );
			
			return;
		} 

		User tmp = (User)getDB().get(User.class,  to );
			
		tmp.transferMoneyFrom( user.getId(), count, "&Uuml;berweisung vom "+Common.getIngameTime(this.ticks));
			
		PM.send(getContext(), user.getId(), tmp.getId(), "RE &uuml;berwiesen", "Ich habe dir soeben "+Common.ln(count)+" RE &uuml;berwiesen");
			
		user.setTemplateVars(t);
		t.setVar( "user.konto", Common.ln(user.getKonto()) );
	
		redirect("other");	
	}

	/**
	 * Aendert den Anzeigetyp fuer Kontotransaktionen
	 * @urlparam Integer type Der neue Anzeigetyp (0-2)
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void showKontoTransactionTypeAction() {
		if( !Faction.get(faction).getPages().hasPage("other") ) {
			redirect();	
			return;
		}
		
		User user = (User)getUser();
		
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
	@Action(ActionType.DEFAULT)
	public void otherAction() {
		if( !Faction.get(faction).getPages().hasPage("other") ) {
			redirect();	
			return;
		}
		
		TemplateEngine t = this.getTemplateEngine();
		Database db = getDatabase();
		User user = (User)this.getUser();
		
		t.setVar("show.other",1);

		// ueberweisungen
		t.setBlock("_ERSTEIGERN","ueberweisen.listitem","ueberweisen.list");

		List list = getContext().getDB().createQuery("from User where locate('hide',flags)=0 and id!= :user order by id")
			.setInteger("user", user.getId())
			.list();
		for( Iterator iter=list.iterator(); iter.hasNext(); ) {
			User usr = (User)iter.next();
			t.setVar(	"target.id",	usr.getId(),
						"target.name",	Common._title(usr.getName()) );
			t.parse("ueberweisen.list","ueberweisen.listitem",true);
		}
		
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

		t.setVar(	"konto.newtranstype.name",	newtypetext,
					"konto.newtranstype",		transtype-1 % 3 );
							
		// Kontobewegungen anzeigen
		t.setBlock("_UEBER", "moneytransfer.listitem", "moneytransfer.list");
		
		SQLQuery entry = db.query("SELECT * FROM user_moneytransfer WHERE `type`<=",transtype," AND ((`from`=",user.getId(),") OR (`to`=",user.getId(),")) ORDER BY `time` DESC LIMIT 40");
		while( entry.next() ) {
			User player = null;
			
			if( entry.getInt("from") == user.getId() ) {
				player = (User)getDB().get(User.class, entry.getInt("to"));
			}
			else {
				player = (User)getDB().get(User.class, entry.getInt("from"));
			}
			
			t.setVar(	"moneytransfer.time",		Common.date("j.n.Y H:i",entry.getLong("time")),
						"moneytransfer.from",		(entry.getInt("from") == user.getId() ? 1 : 0),
						"moneytransfer.player",		Common._title(player.getName()),
						"moneytransfer.player.id",	player.getId(),
						"moneytransfer.count",		Common.ln(entry.getLong("count")),
						"moneytransfer.reason",		entry.getString("text") );
								
			t.parse("moneytransfer.list", "moneytransfer.listitem", true);
		}
		entry.free();

		// GTU-Preise
		t.setBlock("_ERSTEIGERN","kurse.listitem","kurse.list");
		t.setBlock("kurse.listitem","kurse.waren.listitem","kurse.waren.list");

		SQLQuery kurse = db.query("SELECT * FROM gtu_warenkurse");
		while( kurse.next() ) {
			Cargo kurseCargo = new Cargo( Cargo.Type.STRING, kurse.getString("kurse") );
			kurseCargo.setOption( Cargo.Option.SHOWMASS, false );
			
			t.setVar(	"posten.name",		kurse.getString("name"),
						"kurse.waren.list",	"" );
								
			ResourceList reslist = kurseCargo.getResourceList();
			for( ResourceEntry res : reslist ) {
				t.setVar(	"ware.image",	res.getImage(),
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
	@Action(ActionType.DEFAULT)
	public void angeboteAction() {
		if( !Faction.get(faction).getPages().hasPage("angebote") ) {
			redirect();	
			return;
		}
		
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.setVar("show.angebote",1);
	
		t.setBlock("_ERSTEIGERN","angebote.item","angebote.list");
		t.setBlock("_ERSTEIGERN","angebote.emptyitem","none");
	
		t.setVar( "none", "" );
						
		int count = 0;
		SQLQuery angebot = db.query("SELECT title,image,description FROM factions_angebote WHERE faction=",this.faction);
		while( angebot.next() ) {
			count++;
			t.setVar(	"angebot.title",		Common._title(angebot.getString("title")),
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
	@Action(ActionType.DEFAULT)
	public void paketAction() {
		if( !Faction.get(faction).getPages().hasPage("paket") ) {
			redirect();
			return;	
		}
		
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = (User)getUser();
		
		SQLResultRow paket = db.first("SELECT * FROM versteigerungen_pakete");
		t.setVar( "show.pakete", 1 );

		if( !paket.isEmpty() ) {
			User bieter = (User)getDB().get(User.class, paket.getInt("bieter"));

			String bietername = "";
			
			if( bieter.getId() == Faction.GTU ) {
				bietername = bieter.getName();	
			}
			else if( bieter.getId() == user.getId() ) {
				bietername = bieter.getName();	
			}
			else if( user.getAccessLevel() > 20 ) {
				bietername = bieter.getName();	
			}
			else if( (bieter.getAlly() != 0) && (bieter.getAlly() == user.getAlly()) ) {
				boolean showGtuBieter = db.first("SELECT showGtuBieter FROM ally WHERE id=",bieter.getAlly()).getInt("showGtuBieter") != 0;

				if( showGtuBieter ) {
					bietername = bieter.getName();	
				}	
			}

			t.setVar(	"paket.id",			paket.getInt("id"),
						"paket.dauer",		paket.getInt("tick")-this.ticks,
						"paket.bieter",		Common._title(bietername),
						"paket.bieter.id",	bieter.getId(),
						"paket.preis",		Common.ln(paket.getLong("preis")) );

			t.setBlock("_ERSTEIGERN","paket.reslistitem","paket.reslist");
			t.setBlock("_ERSTEIGERN","paket.shiplistitem","paket.shiplist");

			if( paket.getString("cargo").length() > 0 ) {
				Cargo cargo = new Cargo( Cargo.Type.STRING, paket.getString("cargo"));
				cargo.setOption( Cargo.Option.SHOWMASS, false );
				cargo.setOption( Cargo.Option.LARGEIMAGES, true );			

				ResourceList reslist = cargo.getResourceList();
				for( ResourceEntry res : reslist ) {
					t.setVar(	"res.image",		res.getImage(),
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
					t.setVar(	"ship.type.image",	shiptype.getString("picture"),
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
	@Action(ActionType.DEFAULT)
	public void versteigerungAction() {
		TemplateEngine t = this.getTemplateEngine();
		Database db = getDatabase();
		User user = (User)getUser();
		
		if( !Faction.get(faction).getPages().hasPage("versteigerung") ) {
			redirect();	
			return;
		}
		
		t.setVar( "show.versteigerungen", 1 );
		t.setBlock("_ERSTEIGERN","entry.listitem","entry.list");
		t.setBlock("_ERSTEIGERN","gtuzwischenlager.listitem","gtuzwischenlager.list");
		
		/*
			Laufende Handelsvereinbarungen anzeigen 
			(nur solche, die man schon selbst erfuellt hat im Moment)
		*/
		Set<Integer> gzlliste = new HashSet<Integer>();
		
		SQLQuery aentry = db.query("SELECT * FROM gtu_zwischenlager WHERE user1=",user.getId()," OR user2=",user.getId());
		while( aentry.next() ) {
			String owncargoneed = aentry.getString("cargo1need");
			if( aentry.getInt("user2") == user.getId() ) {
				owncargoneed = aentry.getString("cargo2need");
			}
			
			if( new Cargo(Cargo.Type.STRING, owncargoneed).isEmpty() ) {
				gzlliste.add(aentry.getInt("posten"));	
			}
		}
		aentry.free();
		
		for( Integer postenid : gzlliste ) {
			SQLResultRow aposten = db.first("SELECT name,x,y,system FROM ships WHERE id=",postenid);
			t.setVar(	"gtuzwischenlager.name",	Common._plaintitle(aposten.getString("name")),
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
			User bieter = (User)getDB().get(User.class,  entry.getInt("bieter") );
			
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
				entrylink = Common.buildUrl("default", "module", "schiffinfo", "ship", entry.getInt("type") );
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
					entrylink = Common.buildUrl("details", "module", "iteminfo", "item", resource.getId().getItemID() );
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

			if( bieter.getId() == faction ) {
				bietername = bieter.getName();	
			}
			else if( bieter.getId() == user.getId() ) {
				bietername = bieter.getName();
			}
			else if( user.getAccessLevel() > 20 ) {
				bietername = bieter.getName();	
			}
			else if( (bieter.getAlly() != 0) && (bieter.getAlly() == user.getAlly()) ) {
				if( showGtuBieter == null ) {
					SQLResultRow ally = db.first("SELECT showGtuBieter FROM ally WHERE id=",bieter.getAlly());
					showGtuBieter = ally.getInt("showGtuBieter") != 0;
				}
				
				if( showGtuBieter ) {
					bietername = bieter.getName();	
				}	
			}
			
			String ownername = "";
			
			if( (user.getAccessLevel() >= 20) && (entry.getInt("owner") != faction) && (entry.getInt("owner") != user.getId()) ) {
				User ownerobject = (User)getDB().get(User.class, entry.getInt("owner"));
				ownername = Common._title(ownerobject.getName()); 
			}
			
			t.setVar(	"entry.link",		entrylink,
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
						"entry.ownauction",	(entry.getInt("owner") == user.getId()) );
	
			t.parse("entry.list","entry.listitem",true);
		}
		entry.free();

		t.setBlock("_ERSTEIGERN","gtu.dropzones.listitem","gtu.dropzones.list");
		for( StarSystem system : Systems.get() ) {
			if( system.getDropZone() != null ) {
				t.setVar(	"dropzone.system.id",	system.getID(),
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
	@Action(ActionType.DEFAULT)
	public void generalAction() {
		TemplateEngine t = getTemplateEngine();
		
		if( !Faction.get(faction).getPages().hasPage("general") ) {
			redirect();	
			return;
		}
		
		t.setVar(	"show.general",			1,
					"global.faction.text",	Common._text(Faction.get(faction).getPages().getFactionText()) );
		
		return;
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
	@Action(ActionType.DEFAULT)
	public void shopOrderGanymedeSummaryAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
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
		
		SQLResultRow gany = db.first("SELECT id,x,y,name FROM ships WHERE id=",ganymedeid," AND owner=",user.getId()," AND type=",ShopGanyTransportEntry.SHIPTYPE_GANYMEDE," AND system=",sourcesystem);
		if( gany.isEmpty() ) {
			addError("Die angegebene Ganymede konnte im Ausgangssystem nicht lokalisiert werden");
			unsetParameter("sourcesystem");
			unsetParameter("ganymedeid");
			
			redirect("shopOrderGanymede");
			return;
		}
		
		SQLResultRow sameorder = db.first("SELECT id FROM factions_shop_orders WHERE user_id=",user.getId()," AND adddata LIKE \"",gany.getInt("id"),"@%\" AND status<4");
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
		JumpNodeRouter.Result shortestpath = new JumpNodeRouter(jumpnodes)
			.locateShortestJNPath(sourcesystem,gany.getInt("x"),gany.getInt("y"),
					targetsystem, targetx, targety);
		if( shortestpath == null ) {
			transport = 0;
			t.setVar("transport.price", "<span style=\"color:red\">Kein Weg gefunden</span>");	
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
				t.setVar(	"transport.price",			Common.ln(totalcost)+" RE",
							"transport.enableOrder",	1 );
			}
			else {
				transport = 0;
				t.setVar("transport.price", "<span style=\"color:red\">"+Common.ln(totalcost)+" RE</span>");
			}
		}
		
		if( transport == 0 ) {
			t.setVar(	"show.shopOrderGanymedeSummary",	1,
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
	
			User faction = (User)getDB().get(User.class,  this.faction );
			faction.transferMoneyFrom( user.getId(), totalcost, "&Uuml;berweisung Bestellung #ganytransXX"+gany.getInt("id"));	
		
			StringBuilder waypoints = new StringBuilder(300);
			waypoints.append("Start: "+sourcesystem+":"+gany.getInt("x")+"/"+gany.getInt("y")+"\n");
			for( int i=0; i < shortestpath.path.size(); i++ ) {
				SQLResultRow jn = shortestpath.path.get(i);
				
				waypoints.append(jn.getInt("system")+":"+jn.getInt("x")+"/"+jn.getInt("y")+" -> ");
				waypoints.append(jn.getInt("systemout")+":"+jn.getInt("xout")+"/"+jn.getInt("yout")+" ");
				waypoints.append("[ID: "+jn.getInt("id")+"]\n");	
			}		
			waypoints.append("Ziel: "+targetsystem+":"+targetx+"/"+targety+"\n");
		
			PM.send(getContext(), user.getId(), this.faction, "[auto] Shop-Bestellung [Ganymede]", "Besteller: [userprofile="+user.getId()+"]"+user.getName()+" ("+user.getId()+")[/userprofile]\nObjekt: "+gany.getString("name")+" ("+gany.getInt("id")+")\nPreis: "+Common.ln(totalcost)+"\nZeitpunkt: "+Common.date("d.m.Y H:i:s")+"\n\n[b][u]Pfad[/u][/b]:\n"+waypoints);
		
			String adddataStr = gany.getInt("id")+"@"+sourcesystem+":"+gany.getInt("x")+"/"+gany.getInt("y")+"->"+targetsystem+":"+targetx+"/"+targety;
			
			PreparedQuery stmt = db.prepare("INSERT INTO factions_shop_orders " ,
					"(shopentry_id,user_id,price,date,adddata) VALUES " ,
					"( ?, ?, ?, ?, ?)");
			stmt.update(shopentry.getInt("id"), user.getId(), totalcost, Common.time(), adddataStr);
	
			Taskmanager taskmanager = Taskmanager.getInstance();
			taskmanager.addTask( Taskmanager.Types.GANY_TRANSPORT, 1, Integer.toString(stmt.insertID()), "", "" );
			
			stmt.close();
	
			if( !db.tCommit() ) {
				addError("Die Bestellung konnte nicht korrekt verarbeitet werden. Bitte versuchen sie es erneut.");
				redirect("shop");
				return;	
			}
		
			t.setVar("show.message", "Bestellung &uuml;ber 1 Ganymede-Transport des Objekts "+gany.getInt("id")+" von "+sourcesystem+":"+gany.getInt("x")+"/"+gany.getInt("y")+" nach "+targetsystem+":"+targetx+"/"+targety+" f&uuml;r "+Common.ln(totalcost)+" erhalten und vom System best&auml;tigt.<br />Einen angenehmen Tag noch!");
			
		
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
	@Action(ActionType.DEFAULT)
	public void shopOrderGanymedeAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
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
		
		t.setBlock("_ERSTEIGERN", "ganytrans.sourcesystem.listitem", "ganytrans.sourcesystem.list");
		t.setBlock("_ERSTEIGERN", "ganytrans.ganymedes.listitem", "ganytrans.ganymedes.list");
		t.setBlock("_ERSTEIGERN", "ganytrans.targetsystem.listitem", "ganytrans.targetsystem.list");
		t.setVar("show.shopOrderGanymede", 1);
		
			
		// Dummyeintrag (Ausgangssysteme)
		t.setVar(	"sourcesystem.id",		0,
					"sourcesystem.name",	"-" );					
		t.parse("ganytrans.sourcesystem.list", "ganytrans.sourcesystem.listitem", true);
		
		// Liste aller bereits mit einem Transport-Auftrag ausgestatteten Ganys generieren
		Set<Integer> blockedganylist = new HashSet<Integer>();
		List<Integer> blockedganysqlList = new ArrayList<Integer>();
		
		SQLQuery adddata = db.query("SELECT t1.adddata FROM factions_shop_orders t1 JOIN factions_shop_entries t2 ON t1.shopentry_id=t2.id WHERE t1.user_id=",user.getId()," AND t1.status<4 AND t2.type=2");
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
		SQLQuery asystem = db.query("SELECT system FROM ships WHERE type=",ShopGanyTransportEntry.SHIPTYPE_GANYMEDE," AND owner=",user.getId()," ",blockedganysql," GROUP BY system ORDER BY system");
		while( asystem.next() ) {
			if( sourcesystem == asystem.getInt("system") ) {
				t.setVar("sourcesystem.selected", 1);
				first = false;
			}
			else {
				t.setVar("sourcesystem.selected", 0);
			}

			t.setVar(	"sourcesystem.id",	asystem.getInt("system"),
						"sourcesystem.name",	Systems.get().system(asystem.getInt("system")).getName() );
									
			t.parse("ganytrans.sourcesystem.list", "ganytrans.sourcesystem.listitem", true);
		}
		asystem.free();
		
		// Check, ob ein System ausgewaehlt wurde.
		// Wenn nicht -> Ende
		if( first || sourcesystem == 0 ) {
			return;	
		}
		
		t.setVar("sourcesystem.known", 1);
		
		// Moegliche Ganymedes ausgeben
		first = true;
		SQLQuery agany = db.query("SELECT id,name FROM ships WHERE type=",ShopGanyTransportEntry.SHIPTYPE_GANYMEDE," AND owner=",user.getId()," AND system=",sourcesystem," ORDER BY x+y");
		while( agany.next() ) {
			if( blockedganylist.contains(agany.getInt("id")) ) {
				continue;
			}
			
			if( first ) {
				t.setVar("ganymede.selected", 1);
				first = false;
			}
			else {
				t.setVar("ganymede.selected", 0);
			}
			t.setVar(	"ganymede.id",		agany.getInt("id"),
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
				t.setVar("targetsystem.selected", 1);
				first = false;
			}
			else {
				t.setVar("targetsystem.selected", 0);
			}
			
			t.setVar(	"targetsystem.id",		system.getID(),
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
	@Action(ActionType.DEFAULT)
	public void shopOrderAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = (User)getUser();
		
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
			t.setVar("show.message", "<span style=\"color:red\">Es existiert kein passendes Angebot</span>");
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
		else {
			throw new RuntimeException("Unbekannter Versteigerungstyp '"+shopentry.getInt("type")+"'");
		}
		
		if( user.getKonto().compareTo(new BigDecimal(entry.getPrice()*ordercount).toBigInteger()) < 0 ) {
			t.setVar("show.message", "<span style=\"color:red\">Sie verf&uuml;gen nicht &uuml;ber genug Geld</span>");
			redirect("shop");
			return;	
		}
		
		if( ordersys == 0 || orderx == 0 || ordery == 0 ) {
			t.setVar(	"show.shopOrderLocation",	1,
						"order.count",				ordercount,
						"order.name",				entry.getName(),
						"order.entry",				entry.getID() );
		}
		else {		
			db.tBegin();
			db.update("INSERT INTO factions_shop_orders " ,
					"(shopentry_id,user_id,count,price,date,adddata) VALUES " ,
					"(",entry.getID(),",",user.getId(),",",ordercount,",",(ordercount*entry.getPrice()),",",Common.time(),",'",ordersys+":"+orderx+"/"+ordery+"')");
			
			User faction = (User)getDB().get(User.class,  this.faction );
			faction.transferMoneyFrom( user.getId(), entry.getPrice()*ordercount, "&Uuml;berweisung Bestellung #"+entry.getType()+entry.getResource()+"XX"+ordercount);	
			
			PM.send(getContext(), user.getId(), this.faction, "[auto] Shop-Bestellung", "Besteller: [userprofile="+user.getId()+"]"+user.getName()+" ("+user.getId()+")[/userprofile]\nObjekt: "+entry.getName()+"\nMenge: "+ordercount+"\nLieferkoordinaten: "+ordersys+":"+orderx+"/"+ordery+"\nZeitpunkt: "+Common.date("d.m.Y H:i:s"));
			if( !db.tCommit() ) {
				addError("Die Bestellung konnte nicht korrekt verarbeitet werden. Bitte versuchen sie es erneut.");
				redirect("shop");
				return;	
			}
			
			t.setVar("show.message", "Bestellung &uuml;ber "+ordercount+"x "+entry.getName()+" f&uuml;r "+Common.ln(entry.getPrice()*ordercount)+" erhalten und vom System best&auml;tigt.<br />Sollten noch R&uuml;ckfragen bestehend so wird sich ein Sachbearbeiter bei ihnen melden.<br />Einen angenehmen Tag noch!");
			
			redirect("shop");
		}
	}
	
	/**
	 * Aendert die Verfuegbarkeit eines Shopeintrags
	 * @urlparam Integer shopentry Die ID des Shopeintrags
	 * @urlparam Integer availability Die neue Verfuegbarkeit
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void shopChangeAvailabilityAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = (User)getUser();
		
		if( !Faction.get(faction).getPages().hasPage("shop") ) {
			redirect();	
			return;
		}
		
		if( this.faction == user.getId() ) {
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
			
			db.update("UPDATE factions_shop_entries SET availability=",availability," WHERE id=",shopentry.getInt("id"));
			
			t.setVar("show.message", "Neuer Status erfolgreich zugewiesen");
		}
		redirect("shop");
	}
	
	/**
	 * Aendert den Auftragsstatus einer Bestellung
	 * @urlparam Integer orderentry Die ID des Auftrags
	 * @urlparam Integer orderstatus Der neue Auftragsstatus
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void changeShopOrderStatusAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = (User)getUser();
		
		if( !Faction.get(faction).getPages().hasPage("shop") ) {
			redirect();	
			return;
		}
		
		if( this.faction == user.getId() ) {
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
			
			t.setVar("show.message", "Neuer Status erfolgreich zugewiesen");
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
	@Action(ActionType.DEFAULT)
	public void shopAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = (User)getUser();
		
		if( !Faction.get(faction).getPages().hasPage("shop") ) {
			redirect();	
			return;
		}			
		
		t.setVar( "show.shop", 1 );
		
		t.setBlock("_ERSTEIGERN", "shop.listitem", "shop.list");
		t.setBlock("_ERSTEIGERN", "shop.orderlist.listitem", "shop.orderlist.list");
		t.setBlock("_ERSTEIGERN", "shop.shopownerlist.listitem", "shop.shopownerlist.list");
	
		if( this.faction != user.getId() ) {									
			SQLQuery orderentry = db.query("SELECT t1.* FROM factions_shop_orders t1 JOIN factions_shop_entries t2 ON t1.shopentry_id=t2.id WHERE t2.faction_id=",this.faction," AND t1.user_id=",user.getId()," AND t1.status<4");
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
				else {
					throw new RuntimeException("Unbekannter Shopeintrag-Typ '"+shopentry.getInt("type")+"'");
				}
			
				t.setVar(	"orderentry.name",			shopEntryObj.getName(),
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
			t.setVar("shop.owner", 1);
			
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
				
				User ownerobj = (User)getDB().get(User.class, orderentry.getInt("user_id"));
				
				t.setVar(	"orderentry.name",		shopEntryObj.getName(),
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

			t.setVar(	"entry.type.image",			shopEntryObj.getImage(),
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
			
			t.setVar(	"entry.type.image",			shopEntryObj.getImage(),
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
	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {
		this.redirect(Faction.get(faction).getPages().getFirstPage());
		
		return;
	}
}
