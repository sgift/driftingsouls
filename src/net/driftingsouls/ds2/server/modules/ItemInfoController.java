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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.Forschung;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.IEAmmo;
import net.driftingsouls.ds2.server.config.IEDisableShip;
import net.driftingsouls.ds2.server.config.IEDraftShip;
import net.driftingsouls.ds2.server.config.IEModule;
import net.driftingsouls.ds2.server.config.IEModuleSetMeta;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Zeigt Informationen zu Items an
 * @author Christopher Jung
 *
 */
public class ItemInfoController extends DSGenerator {

	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ItemInfoController(Context context) {
		super(context);
		
		setTemplate("iteminfo.html");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}

	private String parseModuleModifiers( SQLResultRow mods ) {
		StringBuilder effecttext = new StringBuilder(300);
		
		for( String key : mods.keySet() ) {
			Object mod = mods.get(key);
			
			boolean closeSpan = false;
			if( mod instanceof Number ) {
				int value = ((Number)mod).intValue();
				if( value < 0 ) {
					effecttext.append("<span style=\"color:red\">");	
				}
				else {
					effecttext.append("<span style=\"color:green\">");
					mod = "+"+mod;	
				}
				closeSpan = true;
			}	
				
			if( key.equals("ru") ) {
				effecttext.append("Reaktor <img src=\""+Cargo.getResourceImage(Resources.URAN)+"\" alt=\"\" /> "+mod);
			}
			else if( key.equals("rd") ) {
				effecttext.append("Reaktor <img src=\""+Cargo.getResourceImage(Resources.DEUTERIUM)+"\" alt=\"\" /> "+mod);
			}
			else if( key.equals("ra") ) {
				effecttext.append("Reaktor <img src=\""+Cargo.getResourceImage(Resources.ANTIMATERIE)+"\" alt=\"\" /> "+mod);
			}
			else if( key.equals("rm") ) {
				effecttext.append("Reaktor <img src=\""+Configuration.getSetting("URL")+"data/interface/energie.gif\" alt=\"\" /> "+mod);
			}	
			else if( key.equals("eps") ) {
				effecttext.append("Energiespeicher "+mod);
			}
			else if( key.equals("cost") ) {
				effecttext.append("Flugkosten "+mod);
			}
			else if( key.equals("heat") ) {
				effecttext.append("&Uuml;berhitzung "+mod);
			}
			else if( key.equals("hull") ) {
				effecttext.append("H&uuml;lle "+mod);
			}
			else if( key.equals("panzerung") ) {
				effecttext.append("Panzerung "+mod);
			}
			else if( key.equals("cargo") ) {
				effecttext.append("Cargo "+mod);
			}
			else if( key.equals("crew") ) {
				effecttext.append("Crew "+mod);
			}
			else if( key.equals("shields") ) {
				effecttext.append("Schilde "+mod);
			}
			else if( key.equals("size") ) {
				effecttext.append("Gr&ouml;&szlig;e "+mod);
			}
			else if( key.equals("jdocks") ) {
				effecttext.append("J&auml;gerdocks "+mod);
			}	
			else if( key.equals("adocks") ) {
				effecttext.append("Externe Docks "+mod);
			}
			else if( key.equals("sensorrange") ) {
				effecttext.append("Sensorreichweite "+mod);
			}
			else if( key.equals("hydro") ) {
				effecttext.append("Produziert <img src=\""+Cargo.getResourceImage(Resources.NAHRUNG)+"\" alt=\"\" />"+mod);
			}	
			else if( key.equals("deutfactor") ) {
				effecttext.append("Sammelt <img src=\""+Cargo.getResourceImage(Resources.DEUTERIUM)+"\" alt=\"\" />"+mod);
			}
			else if( key.equals("recost") ) {
				effecttext.append("Wartungskosten "+mod);
			}
			else if( key.equals("werft") ) {
				if( mod.equals("ganymed") ) {
					effecttext.append("Ganymed-Werft");	
				}
				else if( mod.equals("pwerft") ) {
					effecttext.append("Planetare Werft");	
				}
				else {
					effecttext.append("Werft: "+mod);	
				}
			}
			else if( key.equals("flags") ) {
				String[] flags = StringUtils.split(mod.toString(), ' ');
				for( int i=0; i < flags.length; i++ ) {
					if( i > 0 ) {
						effecttext.append("<br />\n");	
					}
					effecttext.append(ShipTypes.getShipTypeFlagName(flags[i]));
				}
			}
			else if( key.equals("weapons") ) {
				Map<String,Integer[]> weaponlist = (Map<String,Integer[]>)mod;
				
				StringBuilder wpntext = new StringBuilder(50);
				for( String weaponclassname : weaponlist.keySet() ) {
					Integer[] weaponmods = weaponlist.get(weaponclassname);
					
					int weaponcount = weaponmods[0];
					int weaponheat = weaponmods[1];	
					
					if( wpntext.length() > 0 ) {
						wpntext.append("<br />");	
					}
					wpntext.append("<span class=\"nobr\">");
					if( Math.abs(weaponcount) > 1 ) {
						wpntext.append(weaponcount+"x ");	
					}
					wpntext.append(Weapons.get().weapon(weaponclassname).getName());
					wpntext.append("</span><br />[Max. Hitze: "+weaponheat+"]");
				}
				effecttext.append(wpntext);
			}
			else if( key.equals("maxheat") ) {
				Map<String,Integer> weaponlist = (Map<String,Integer>)mod;
				
				StringBuilder wpntext = new StringBuilder(50);
				for( String weaponclassname : weaponlist.keySet() ) {
					int weaponheat = weaponlist.get(weaponclassname);
					
					if( wpntext.length() > 0 ) {
						wpntext.append("<br />");	
					}
					wpntext.append(Weapons.get().weapon(weaponclassname).getName()+":<br />+"+weaponheat+" Max-Hitze");
				}
				effecttext.append(wpntext);
			}
			
			if( closeSpan ) {
				effecttext.append("</span>");
			}	
			effecttext.append("<br />\n");
		}
		
		return effecttext.toString();	
	}
	
	/**
	 * Zeigt Details zu einem Item an
	 * @urlparam Integer itemid Die ID des anzuzeigenden Items
	 */
	public void detailsAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		parameterNumber("item");
		int itemid = getInteger("item");
				
		if( Items.get().item(itemid) == null ) {
			t.setVar("iteminfo.message", "Es ist kein Item mit dieser Identifikationsnummer bekannt");
		
			return;
		}
		
		if( Items.get().item(itemid).getAccessLevel() > user.getAccessLevel() ) {
			t.setVar("iteminfo.message", "Es ist kein Item mit dieser Identifikationsnummer bekannt");
		
			return;
		}
		
		if( Items.get().item(itemid).isUnknownItem() && !user.isKnownItem(itemid) && (user.getAccessLevel() < 15) ) {
			t.setVar("iteminfo.message", "Es ist kein Item mit dieser Identifikationsnummer bekannt");
		
			return;
		}
		
		String name = Common._plaintitle(Items.get().item(itemid).getName());
		if( Items.get().item(itemid).getQuality().color().length() > 0 ) {
			name = "<span style=\"color:"+Items.get().item(itemid).getQuality().color()+"\">"+name+"</span>";	
		}
		
		t.setVar(	"iteminfo.details",	1,
					"item.picture",		Items.get().item(itemid).getPicture(),
					"item.name",		name,
					"item.cargo",		Items.get().item(itemid).getCargo(),
					"item.accesslevel",	Items.get().item(itemid).getAccessLevel(),
					"item.allyitem",	Items.get().item(itemid).getEffect().hasAllyEffect(),
					"item.class",		Items.get().item(itemid).getEffect().getType().getName(),
					"item.description",	Common._text(Items.get().item(itemid).getDescription()) );
		
		t.setBlock("_ITEMINFO", "itemdetails.entry", "itemdetails.entrylist");
		
		switch( Items.get().item(itemid).getEffect().getType() ) {
		/*
		
			EFFECT_DRAFT_SHIP
			
		*/
		case DRAFT_SHIP: {
			IEDraftShip effect = (IEDraftShip)Items.get().item(itemid).getEffect();
			
			SQLResultRow shiptype = ShipTypes.getShipType( effect.getShipType(), false );
	
			StringBuilder data = new StringBuilder(100);
			if( shiptype.getBoolean("hide") ) {
        		if( user.getAccessLevel() > 20 ) {
        			data.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "module", "schiffinfo", "ship", effect.getShipType())+"\">"+shiptype.getString("nickname")+"</a><br /><span style=\"font-style:italic;color:red\" class=\"verysmallfont\">[unsichtbar]</span>\n");
	        	} 
	        	else {
    	    		data.append("Unbekannt");
        		}
			}	
			else if( !shiptype.isEmpty() ) {
				data.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "module", "schiffinfo", "ship", effect.getShipType())+"\">"+shiptype.getString("nickname")+"</a>\n");
			}	
			else {
				data.append("<span style=\"color:red\">Unbekannter Schiffstyp</span>");
			}
			
			t.setVar(	"entry.name",	"Schiffstyp",
						"entry.data",	data );
			
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);
						
			t.setVar(	"entry.name",	"Rasse",
						"entry.data",	Rassen.get().rasse(effect.getRace()).getName() );
			
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);
	
			data.setLength(0);
			boolean entry = false;
			for( int i = 0; i < 3; i++ ) {
				if( effect.getTechReq(i) != 0 ) {
					Forschung dat = Forschung.getInstance(effect.getTechReq(i));
					if( !dat.isVisibile() && (!user.hasResearched(dat.getRequiredResearch(1)) || !user.hasResearched(dat.getRequiredResearch(2)) || !user.hasResearched(dat.getRequiredResearch(3))) ) {
						data.append("Unbekannt");
						if( user.getAccessLevel() > 20 ) {
							data.append(" [ID:"+effect.getTechReq(i)+"]");
						}	
						data.append("<br />\n");
						entry = true;
						
						continue;
					}
					
					data.append("<a class=\"nonbold\" href=\""+Common.buildUrl("default","module", "forschinfo", "res", effect.getTechReq(i))+"\">");
			 		
			 		if( user.hasResearched(effect.getTechReq(i)) ) {
			 			data.append("<span style=\"color:green; font-size:14px\">");
		 			}
		 			else {
			 			data.append("<span style=\"color:red; font-size:14px\">");
			 		}
			 		data.append(dat.getName());
		 			data.append("</span></a><br />\n");
		 			entry = true;
				}
			}
			if( !entry ) {
				data.append("-");
			}
			
			t.setVar(	"entry.name",	"Ben&ouml;tigt",
						"entry.data",	data );
			
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);

			data.setLength(0);
			Cargo costs = effect.getBuildCosts();
			ResourceList reslist = costs.getResourceList();
			for( ResourceEntry res : reslist ) {
				data.append("<img src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+"<br />\n");
			}
			data.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/energie.gif\" alt=\"\" />"+effect.getE()+"<br />\n");
			data.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/besatzung.gif\" alt=\"\" />"+effect.getCrew()+"<br />\n");
			
			t.setVar(	"entry.name",	"Kosten",
						"entry.data",	data );
			
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);
			
			t.setVar(	"entry.name",	"Dauer",
						"entry.data",	"<img valign=\"middle\" src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"\" />"+effect.getDauer() );
			
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);
	
			data.setLength(0);
			
			boolean first = true;
			if( Common.inArray("pwerft", effect.getWerftReqs()) ) {
				if( !first ) {
					data.append("<br />\n");
				}
				data.append("Planetare Werft");
        		first = false;
			}
			if( Common.inArray("ganymed", effect.getWerftReqs()) ) {
				if( !first ) {
					data.append("<br />\n");
				}
				data.append("Ganymed");
        		first = false;
			}
			
			t.setVar(	"entry.name",	"Baubar in",
						"entry.data",	data );
			
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);
	
			t.setVar(	"entry.name",	"Flagschiff",
						"entry.data",	(effect.isFlagschiff() ? "ja" : "nein") );
			
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);
			
			break;
		}
		/*
		
			EFFECT_DISABLE_SHIP
			
		*/
		case DISABLE_SHIP: {
			IEDisableShip effect = (IEDisableShip)Items.get().item(itemid).getEffect();
			t.setVar( "entry.name", "Schiffstyp" );
			
			SQLResultRow shiptype = ShipTypes.getShipType( effect.getShipType(), false );
			if( !shiptype.isEmpty() ) {
				t.setVar("entry.data", "<a class=\"forschinfo\" href=\""+Common.buildUrl("default","module", "schiffinfo", "ship", effect.getShipType())+"\">"+shiptype.getString("nickname")+"</a>" );
			}
			else {
				t.setVar("entry.data", "<span style=\"color:red\">Unbekannter Schiffstyp</span>");
			}
			
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);
			
			break;
		}
		/*
		
			EFFECT_MODULE
			
		*/
		case MODULE: {
			IEModule effect = (IEModule)Items.get().item(itemid).getEffect();
			
			StringBuilder targetslots = new StringBuilder(50);
			List<String> slots = effect.getSlots();
			for( int i=0; i < slots.size(); i++ ) {
				String aslot = slots.get(i);
				
				if( targetslots.length() > 0 ) {
					targetslots.append(", ");	
				}
				if( ModuleSlots.get().slot(aslot) != null ) {
					targetslots.append(ModuleSlots.get().slot(aslot).getName());
				}
			}
			
			int setknowncount = 0;
			if( effect.getSetID() != 0 ) {
				int setcount = 0;
				
				for( Item aitem : Items.get() ) {
					if( aitem.getEffect().getType() != ItemEffect.Type.MODULE ) {
						continue;	
					}	
					if( ((IEModule)aitem.getEffect()).getSetID() != effect.getSetID() ) {
						continue;	
					}
					setcount++;
					if( (user.getAccessLevel() >= 15) || user.isKnownItem(aitem.getID()) ) {
						setknowncount++;	
					}
				}
				
				t.setVar(	"entry.name",	"Set",
							"entry.data",	((IEModuleSetMeta)Items.get().item(effect.getSetID()).getEffect()).getName()+" ("+setcount+")" );
								
				t.parse("itemdetails.entrylist", "itemdetails.entry", true);	
			}
			
			t.setVar(	"entry.name",	"Passt in",
						"entry.data",	targetslots.toString() );
								
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);
			
			String effecttext = this.parseModuleModifiers( effect.getMods() );
			
			t.setVar(	"entry.name",	"Effekt",
						"entry.data",	effecttext );
								
			t.parse("itemdetails.entrylist", "itemdetails.entry", true);
								
			if( effect.getSetID() != 0 ) {
				IEModuleSetMeta meta = ((IEModuleSetMeta)Items.get().item(effect.getSetID()).getEffect());
				Map<Integer,SQLResultRow> modlist = meta.getCombos();
				
				for( Integer modulecount : modlist.keySet() ) {
					SQLResultRow mods = modlist.get(modulecount);
			
					if( modulecount <= setknowncount ) {
						effecttext = this.parseModuleModifiers( mods );
						
						t.setVar(	"entry.name",	"Set-Combo ("+modulecount+" Items)",
									"entry.data",	effecttext );	
						t.parse("itemdetails.entrylist", "itemdetails.entry", true);
					}
				}	
			}
			
			break;
		}	
		/*
		
			EFFECT_AMMO
			
		*/
		case AMMO: {
			IEAmmo effect = (IEAmmo)Items.get().item(itemid).getEffect();
			
			SQLResultRow ammo = db.first("SELECT * FROM ammo WHERE id=",effect.getAmmoID());
		
			if( ammo.isEmpty() ) {
				t.setVar(	"entry.name",	"Munition",
							"entry.data",	"Es liegen keine genaueren Daten zur Munition vor" );
			
				t.parse("itemdetails.entrylist", "itemdetails.entry", true);
			}
			else {
				StringBuilder data = new StringBuilder(100);
				boolean entry = false;
				for( int i = 1; i < 4; i++ ) {
					if( ammo.getInt("res"+i) != 0 ) {
						if( entry ) {
							data.append(",<br />\n");
						}
						
						Forschung dat = Forschung.getInstance(ammo.getInt("res"+i));
						if( (ammo.getInt("res"+i) == -1) || 
							(!dat.isVisibile() && (!user.hasResearched(dat.getRequiredResearch(1)) || !user.hasResearched(dat.getRequiredResearch(2)) || !user.hasResearched(dat.getRequiredResearch(3)))) ) {
							
							data.append("Unbekannte Technologie");
							if( user.getAccessLevel() > 20 ) {
								data.append(" [ID:"+ammo.getInt("res"+i)+"]");
							}
							entry = true;
							continue;
						}
						
						data.append("<a class=\"nonbold\" href=\""+Common.buildUrl("default", "module", "forschinfo", "res", ammo.getInt("res"+i))+"\">");
		 				if( user.hasResearched(ammo.getInt("res"+i)) ) {
		 					data.append("<span style=\"color:green; font-size:14px\">");
		 				}
			 			else {
			 				data.append("<span style=\"color:red; font-size:14px\">");
			 			}
		 				data.append(dat.getName());
		 				data.append("</span></a>\n");
						entry = true;
					}
				}
				if( !entry ) {
					data.append("-");
				}
				
				t.setVar(	"entry.name",	"Ben&ouml;tigt",
							"entry.data",	data );
			
				t.parse("itemdetails.entrylist", "itemdetails.entry", true);
				
				Cargo buildcosts = new Cargo( Cargo.Type.STRING, ammo.getString("buildcosts") );
			
				data.setLength(0);

				ResourceList reslist = buildcosts.getResourceList();
				for( ResourceEntry res : reslist ) {
					data.append("<img src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+" ");
				}	
				
				t.setVar(	"entry.name",	"Kosten",
							"entry.data",	data );
			
				t.parse("itemdetails.entrylist", "itemdetails.entry", true);
				
				data.setLength(0);
				if( ammo.getInt("shotspershot") > 1 ) {
					data.append(ammo.getInt("shotspershot")+" Salven<br />\n");
				}
				if( ammo.getInt("damage") != 0 ) {
					data.append(ammo.getInt("damage")+" Schaden<br />\n");
				}
				if( ammo.getInt("damage") != ammo.getInt("shielddamage") ) {
					data.append(ammo.getInt("shielddamage")+" Schildschaden<br />\n");
				}
				if( ammo.getInt("subdamage") != 0 ) {
					data.append(ammo.getInt("subdamage")+" Subsystemschaden<br />\n");
					data.append(ammo.getInt("subws")+"% Subsystem-Trefferws<br />\n");
				}
				data.append(ammo.getInt("smalltrefferws")+"% Trefferws (J&auml;ger)<br />\n");
				data.append(ammo.getInt("trefferws")+"% Trefferws (Capitals)\n");
				if( ammo.getInt("torptrefferws") != 0 ) {
					data.append("<br />"+ammo.getInt("torptrefferws")+"% Trefferws (Torpedos)\n");
				}
				if( ammo.getInt("areadamage") != 0 ) {
					data.append("<br />Umgebungsschaden ("+ammo.getInt("areadamage")+")\n");
				}
				if( ammo.getInt("destroyable") > 0 ) {
					data.append("<br />Durch Abwehrfeuer zerst&ouml;rbar\n");
				}
				if( ammo.getInt("replaces") != 0 ) {
					SQLResultRow replammo = db.first("SELECT itemid,name FROM ammo WHERE id=",ammo.getInt("replaces"));
					data.append("<br />Ersetzt <a style=\"font-size:14px\" class=\"forschinfo\" href=\""+Common.buildUrl("details", "module", "iteminfo", "item", replammo.getInt("itemid"))+"\">"+replammo.getString("name")+"</a>\n");
				}
					
				t.setVar(	"entry.name",	"Daten",
							"entry.data",	data );
			
				t.parse("itemdetails.entrylist", "itemdetails.entry", true);
				
				StringBuilder weapons = new StringBuilder(50);
				for( Weapon weapon : Weapons.get() ) {
					if( weapon.getAmmoType() != ammo.get("type") ) continue;
			
					if( weapons.length() == 0 ) {
						weapons.append(weapon.getName());
					}
					else {
						weapons.append(",<br />\n"+weapon.getName());
					}
				}
				if( weapons.length() > 0 ) {
					t.setVar(	"entry.name",	"Waffe",
								"entry.data",	weapons );
					t.parse("itemdetails.entrylist", "itemdetails.entry", true);
				}
			}
			
			break;
		}
		/*
		 * 
		 *  EFFECT_MODUE_SET_META
		 * 
		 */
		case MODULE_SET_META: {
			if( user.getAccessLevel() < 15 ) {
				return;
			}
			Cargo setitemlist = new Cargo();
			
			for( Item item : Items.get() ) {
				if( (item.getEffect().getType() == ItemEffect.Type.MODULE) && (((IEModule)item.getEffect()).getSetID() == itemid) ) {
					setitemlist.addResource(new ItemID(item.getID()), 1);
				}
			}
			
			StringBuilder tmp = new StringBuilder(50);
			ResourceList reslist = setitemlist.getResourceList();
			for( ResourceEntry res : reslist ) {
				tmp.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\">"+res.getName()+"</span><br />\n");	
			}
			
			if( tmp.length() > 0 ) {
				t.setVar(	"entry.name",	"Set-Items",
							"entry.data",	tmp );
			
				t.parse("itemdetails.entrylist", "itemdetails.entry", true);
			}
			
			break;
		}
		} // Ende switch
	}
	
	/**
	 * Zeigt die Liste aller bekannten Items sowie ihren Aufenthaltsort, sofern man sie besitzt, an
	 *
	 */
	public void knownAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		SQLResultRow ownCargoRow = db.first("SELECT cargo FROM stats_user_cargo WHERE user_id=",user.getId());
		Cargo owncargo = null;
		if( !ownCargoRow.isEmpty() ) {
			owncargo = new Cargo( Cargo.Type.STRING, ownCargoRow.getString("cargo"));
		}
		else {
			owncargo = new Cargo();
		}

		t.setVar("iteminfo.knownlist", 1);

		t.setBlock("_ITEMINFO", "knownlist.listitem", "knownlist.list");
		
		Map<Integer,String[]> reslocations = new HashMap<Integer,String[]>();
		SQLQuery amodule = db.query("SELECT item_id,locations FROM stats_module_locations WHERE user_id=",user.getId());
		while( amodule.next() ) {
				reslocations.put(amodule.getInt("item_id"), StringUtils.split(amodule.getString("locations"),';'));
		}
		amodule.free();
		
		final String shipimage = "<td class='noBorderX' style='text-align:right'><img style='vertical-align:middle' src='"+Configuration.getSetting("URL")+"data/interface/schiffe/"+user.getRace()+"/icon_schiff.gif' alt='' title='Schiff' /></td>";
		final String baseimage = "<td class='noBorderX' style='text-align:right'><img style='vertical-align:middle;width:15px;height:15px' src='"+Configuration.getSetting("URL")+"data/starmap/asti/asti.png' alt='' title='Asteroid' /></td>";
		
		Map<Integer,SQLResultRow> basecache = new HashMap<Integer,SQLResultRow>();
		Map<Integer,String> shipnamecache = new HashMap<Integer,String>();
		
		for( Item aitem : Items.get() ) {
			int itemid = aitem.getID();
			
			ItemEffect itemeffect = aitem.getEffect();
			
			if( aitem.getAccessLevel() > user.getAccessLevel() ) {
				continue;	
			}
			
			if( aitem.isUnknownItem() && !user.isKnownItem(itemid) && (user.getAccessLevel() < 15) ) {
				continue;
			}
			
			String name = Common._plaintitle(aitem.getName());
			if( aitem.getQuality().color().length() > 0 ) {
				name = "<span style=\"color:"+aitem.getQuality().color()+"\">"+name+"</span>";	
			}
			String tooltip = "";
			if( reslocations.containsKey(itemid) ) {				
				StringBuilder tooltiptext = new StringBuilder(200);
				tooltiptext.append(StringUtils.replaceChars(Common.tableBegin(350, "left"), '"', '\'') );
				tooltiptext.append("<table class='noBorderX'>");
				String[] locations = reslocations.get(itemid);
				for( int i=0; i < locations.length; i++ ) {
					String alocation = locations[i];
					int objectid = Integer.parseInt(alocation.substring(1));
					
					tooltiptext.append("<tr>");
					switch( alocation.charAt(0) ) {
					case 's':
						if( !shipnamecache.containsKey(objectid) ) {
							shipnamecache.put(objectid, Common._plaintitle(db.first("SELECT name FROM ships WHERE id=",objectid).getString("name")));
						}
						tooltiptext.append(shipimage+"<td class='noBorderX'><a style='font-size:14px' class='forschinfo' href='"+Common.buildUrl("default", "module", "schiff", "ship", objectid)+"'>"+shipnamecache.get(objectid)+" ("+objectid+")</a></td>");
						break;
					case 'b':
						if( !basecache.containsKey(objectid) ) {
							basecache.put(objectid, db.first("SELECT name,x,y,system FROM bases WHERE id=",objectid));
							basecache.get(objectid).put("name", Common._plaintitle(basecache.get(objectid).getString("name")));
						}
						tooltiptext.append(baseimage+"<td class='noBorderX'><a style='font-size:14px' class='forschinfo' href='"+Common.buildUrl("default", "module", "base", "col", objectid)+"'>"+basecache.get(objectid).getString("name")+" - "+basecache.get(objectid).getInt("system")+":"+basecache.get(objectid).getInt("x")+"/"+basecache.get(objectid).getInt("y")+"</a></td>");
						break;
					case 'g':
						if( !shipnamecache.containsKey(objectid) ) {
							shipnamecache.put(objectid, Common._plaintitle(db.first("SELECT name FROM ships WHERE id=",objectid).getString("name")));
						}
						tooltiptext.append("<td colspan='2' class='noBorderX' style='font-size:14px'>"+shipnamecache.get(objectid)+"</td>");
						break;
					default:
						tooltiptext.append("<td colspan='2' class='noBorderX' style='font-size:14px'>Unbekanntes Objekt "+alocation+"</td>");
					}	
					
					tooltiptext.append("</tr>");
				}
				tooltiptext.append("</table>");
				tooltiptext.append(StringUtils.replaceChars(Common.tableEnd(), '"', '\'') );
				tooltip = StringEscapeUtils.escapeJavaScript(StringUtils.replace(StringUtils.replace(tooltiptext.toString(), ">", "&gt;"), "<", "&lt;"));
			}
		
			t.setVar(	"item.picture",	aitem.getPicture(),
						"item.id",		itemid,
						"item.name",	name,
						"item.class",	itemeffect.getType().getName(),
						"item.cargo",	Common.ln(aitem.getCargo()),
						"item.locationtext",	tooltip,
						"item.count",		Common.ln(owncargo.getResourceCount(new ItemID(itemid)) ) );
			
			t.parse("knownlist.list", "knownlist.listitem", true);
		}
	}
	
	/**
	 * Zeigt eine Itemliste an
	 * @urlparam String itemlist Die Itemliste
	 */
	@Override
	public void defaultAction() {
		TemplateEngine t = this.getTemplateEngine();
		User user = getUser();
		
		parameterString("itemlist");	
		Cargo itemlist = null;

		try {
			itemlist = new Cargo();
			itemlist.addResource(Resources.fromString(getString("itemlist")),1);
		}
		catch( Exception e ) {
			// Offenbar keine Item-ID
			// Jetzt versuchen, die Liste als Itemliste zu parsen
			itemlist = new Cargo(Cargo.Type.ITEMSTRING,getString("itemlist"));
		}

		t.setVar("iteminfo.itemlist", 1);

		t.setBlock("_ITEMINFO", "itemlist.listitem", "itemlist.list");

		List<ItemCargoEntry> myitemlist = itemlist.getItems();
		for( int i=0; i < myitemlist.size(); i++ ) {
			ItemCargoEntry item = myitemlist.get(i);
			
			Item itemobject = item.getItemObject();
			ItemEffect itemeffect = item.getItemEffect();
		
			if( itemobject == null ) {
				continue;
			}
			
			if( itemobject.getAccessLevel() > user.getAccessLevel() ) {
				continue;	
			}
			
			if( itemobject.isUnknownItem() && !user.isKnownItem(item.getItemID()) && (user.getAccessLevel() < 15) ) {
				continue;
			}
			
			String name = Common._plaintitle(itemobject.getName());
			if( itemobject.getQuality().color().length() > 0 ) {
				name = "<span style=\"color:"+itemobject.getQuality().color()+"\">"+name+"</span>";	
			}
		
			t.setVar(	"item.picture",	itemobject.getPicture(),
						"item.id",		item.getItemID(),
						"item.name",	name,
						"item.class",	itemeffect.getType().getName(),
						"item.cargo",	itemobject.getCargo(),
						"item.count",	item.getCount() );
			
			t.parse("itemlist.list", "itemlist.listitem", true);
		}
	}
}
