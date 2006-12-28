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
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.FactionPages;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Zeigt die Fraktionsseiten an
 * @author Christopher Jung
 * 
 * @urlparam Integer faction Die ID der anzuzeigenden Fraktion
 *
 */
public class ErsteigernController extends DSGenerator {
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
		
		if( bid == 0 ) {
			int entrywidth = 0;
			int entryheight = 0;
			long entrycount = 0;
			String entryname = "";
			String entryimage = "";
			String entrylink = "#";
		
			if( entry.getInt("mtype") == 1 ) {	//Schiff
				SQLResultRow shiptype = Ships.getShipType(entry.getInt("type"), false);
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
		else if( bid > 0 ) {
			long cost = entry.getLong("preis")+(long)(entry.getLong("preis")/20d);
			if( cost == entry.getLong("preis") ) {
				cost++;
			}
		
			String entryname = "";
			
			if( entry.getInt("mtype") == 1 ) {
				SQLResultRow shiptype = Ships.getShipType(entry.getInt("type"), false);
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
					
				t.set_var("show.highestbid",1);
					
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
				t.set_var( "user.konto", Common.ln(user.getKonto()) );
			}
			else {
				t.set_var("show.lowres",1);
			}
		}
		
		redirect();
	}
	
	public void bidPaketAction() {
		// TODO
		Common.stub();
	}
	
	public void ueberweisenAction() {
		// TODO
		Common.stub();
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
		TemplateEngine t = getTemplateEngine();
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
		
		SQLQuery entry = db.query("SELECT * FROM user_moneytransfer WHERE `type`<='",transtype,"' AND ((`from`=",user.getID(),") OR (`to`=",user.getID(),"')) ORDER BY `time` DESC LIMIT 40");
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

		SQLQuery kurse = db.query("SELECT * FROM warenkurse");
		while( kurse.next() ) {
			Cargo kurseCargo = new Cargo( Cargo.Type.STRING, kurse.getString("kurse") );
			kurseCargo.setOption( Cargo.Option.SHOWMASS, false );
			
			t.set_var(	"posten.name",		kurse.getString("name"),
						"kurse.waren.list",	"" );
								
			ResourceList reslist = kurseCargo.getResourceList();
			for( ResourceEntry res : reslist ) {
				t.set_var(	"ware.image",	res.getImage(),
							"ware.preis",	(res.getCount1()/1000d > 0.05 ? res.getCount1()/1000d:"") );
									
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
					SQLResultRow shiptype = Ships.getShipType( shiplist[i], false );
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
				SQLResultRow shiptype = Ships.getShipType(entry.getInt("type"), false);
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
	
	public void shopOrderGanymedeSummaryAction() {
		// TODO
		Common.stub();
	}
	
	public void shopOrderGanymedeAction() {
		// TODO
		Common.stub();
	}
	
	public void shopOrderAction() {
		// TODO
		Common.stub();
	}
	
	public void shopChangeAvailabilityAction() {
		// TODO
		Common.stub();
	}
	
	public void changeShopOrderStatusAction() {
		// TODO
		Common.stub();
	}
	
	public void shopAction() {
		// TODO
		Common.stub();
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
