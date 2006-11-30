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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.IEAmmo;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

class Waffenfabrik extends DefaultBuilding {
	private static final Map<Integer,SQLResultRow> ammolist = new HashMap<Integer,SQLResultRow>();
	
	static {
		Database db = new Database();
		SQLQuery ammo = db.query("SELECT * FROM ammo");
		while( ammo.next() ) {
			SQLResultRow ammorow = ammo.getRow();
			ammorow.put("buildcosts", new Cargo( Cargo.Type.STRING, ammorow.getString("buildcosts")) );
			ammolist.put(ammo.getInt("id"), ammorow);
		}
		ammo.free();
		db.close();
	}
	
	private class ContextVars {
		Map<Integer,SQLResultRow> ownerammobase = new HashMap<Integer,SQLResultRow>();
		Map<Integer,Cargo> stats = new HashMap<Integer,Cargo>();
		Map<Integer,BigDecimal> usedcapacity = new HashMap<Integer,BigDecimal>();
		
		protected ContextVars() {
			// EMPTY
		}
	}
	
	/**
	 * Erstellt eine neue Instanz der Waffenfabrik
	 * @param row Die SQL-Ergebniszeile mit den Gebaeudedaten der Waffenfabrik
	 */
	public Waffenfabrik(SQLResultRow row) {
		super(row);
	}
	
	private String loaddata( int col ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		User user = context.createUserObject( db.first("SELECT owner FROM bases WHERE id=",col).getInt("owner") );
		
		ContextVars vars = (ContextVars)context.getVariable(getClass(), "values");
		
		List<Integer> removelist = new ArrayList<Integer>();
		
		if( vars == null ) {
			vars = new ContextVars();
			context.putVariable(getClass(), "values", vars);
			
			for( SQLResultRow ammo : ammolist.values() ) {
				if( !user.hasResearched(ammo.getInt("res1")) || !user.hasResearched(ammo.getInt("res2")) || !user.hasResearched(ammo.getInt("res3")) ) {
					continue;
				}
				vars.ownerammobase.put(ammo.getInt("id"), ammo);
				
				if( (ammo.getInt("replaces") != 0) && !removelist.contains(ammo.getInt("replaces")) ) {
					removelist.add(ammo.getInt("replaces"));
				}
			}
			
			if( user.getAlly() > 0 ) {			
				SQLResultRow allyitems = db.first("SELECT items FROM ally WHERE id='",user.getAlly(),"'");
			
				Cargo itemlist = new Cargo( Cargo.Type.ITEMSTRING, allyitems.getString("cargo") );	
				
				List<ItemCargoEntry> list = itemlist.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO ) ;
				for( ItemCargoEntry item : list ) {
					IEAmmo itemeffect = (IEAmmo)item.getItemEffect();
					vars.ownerammobase.put(itemeffect.getAmmoID(), ammolist.get(itemeffect.getAmmoID()));
					
					if( (ammolist.get(itemeffect.getAmmoID()).getInt("replaces") != 0) && removelist.contains(ammolist.get(itemeffect.getAmmoID()).getInt("replaces")) ) {
						removelist.add(ammolist.get(itemeffect.getAmmoID()).getInt("replaces"));
					}
				}
			}
			
			// Alle Raks entfernen, die durch andere Raks ersetzt wurden (DB-Feld: replaces)
			if( !removelist.isEmpty() ) {
				for( Integer removeentry : removelist ) {
					vars.ownerammobase.remove(removeentry);
				}
			}
		}
		
		StringBuilder wfreason = new StringBuilder(100);
		
		if( !vars.usedcapacity.containsKey(col) ) {
			if( !vars.stats.containsKey(col) ) {
				vars.stats.put(col, new Cargo());
			}
			
			boolean ok = true;
			Map<Integer,SQLResultRow> thisammolist = vars.ownerammobase;	
			
			Cargo cargo = new Cargo( Cargo.Type.STRING, db.first("SELECT cargo FROM bases WHERE id='",col,"'").getString("cargo"));
			
			List<ItemCargoEntry> list = cargo.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO ) ;
			for( ItemCargoEntry item : list ) {
				IEAmmo itemeffect = (IEAmmo)item.getItemEffect();
				thisammolist.put(itemeffect.getAmmoID(), ammolist.get(itemeffect.getAmmoID()));
			}
			
			SQLResultRow wf = db.first( "SELECT produces FROM weaponfactory WHERE col=",col);
			if( wf.isEmpty() ) {
				LOG.warn("Basis "+col+" verfuegt ueber keinen Waffenfabrik-Eintrag, obwohl es eine Waffenfabrik hat");
			}
			String[] plist = StringUtils.split(wf.getString("produces"), ';');
			for( int i=0; i < plist.length; i++ ) {
				String[] tmp = StringUtils.split(plist[i],'=');
				int aid = Integer.parseInt(tmp[0]);
				int count = Integer.parseInt(tmp[1]);
				// Ammo ohne Plaene melden - veraltete Ammo aber ignorieren!
				if( (count > 0) && !thisammolist.containsKey(aid) && !removelist.contains(aid) ) {
					ok = false;
					wfreason.append("Es existieren nicht die n&ouml;tigen Baupl&auml;ne f&uuml;r "+ammolist.get(aid).getString("name")+"\n");
					break;
				}
				else if( (count > 0) && !thisammolist.containsKey(aid) ) {
					plist[i] = "";	
				}
			}
	         	
			if( ok ) {
				for( int i=0; i < plist.length; i++  ) {
					if( plist[i].length() == 0 ) {
						continue;
					}
					String[] tmp = StringUtils.split(plist[i],'=');
					int aid = Integer.parseInt(tmp[0]);
					int count = Integer.parseInt(tmp[1]);
					
					if( !vars.usedcapacity.containsKey(col) ) {
						vars.usedcapacity.put(col, new BigDecimal(0, MathContext.DECIMAL32));
					}
					vars.usedcapacity.put(col, vars.usedcapacity.get(col).add(new BigDecimal(ammolist.get(aid).getString("dauer")).multiply((new BigDecimal(count)))) );
					if( count > 0 ) {
						Cargo tmpcargo = (Cargo)((Cargo)ammolist.get(aid).get("buildcosts")).clone();
						if( count > 1 ) {
							tmpcargo.multiply( count, Cargo.Round.NONE );
						}
						vars.stats.get(col).substractCargo( tmpcargo );
						vars.stats.get(col).addResource( new ItemID(ammolist.get(aid).getInt("itemid")), count );
					}
				}
			}
			else {
				String basename = db.first("SELECT name FROM bases WHERE id='",col,"'").getString("name");
				wfreason.insert(0, "[b]"+basename+"[/b] - Die Arbeiten in der Waffenfabrik zeitweise eingestellt.\nGrund:\n");
			}
			
			if( !vars.usedcapacity.containsKey(col) || (vars.usedcapacity.get(col).doubleValue() <= 0) ) {
				vars.usedcapacity.put(col, new BigDecimal(-1));
			}
		}
		
		return wfreason.toString();
	}

	@Override
	public void build(int col) {
		super.build(col);
		
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow wfentry = db.first("SELECT id FROM weaponfactory WHERE col=",col);
		if( !wfentry.isEmpty() ) {
			db.update("UPDATE weaponfactory SET count=count+1 WHERE id=",wfentry.getInt("id"));
		} 
		else {
			db.update("INSERT INTO weaponfactory (count,col) VALUES (1,",col,")");
		}
	}

	@Override
	public boolean classicDesign() {
		return true;
	}
	
	@Override
	public boolean printHeader() {
		return true;
	}

	@Override
	public void cleanup(Context context, int col) {
		Database db = context.getDatabase();
		
		SQLResultRow wf = db.first("SELECT count,produces FROM weaponfactory WHERE col=",col);
		if( wf.getInt("count") > 1 ) {	
			BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
	
			String[] plist = StringUtils.split(wf.getString("produces"), ';');
			for( int i=0; i < plist.length; i++ ) {
				String[] tmp = StringUtils.split(plist[i], '=');
				int aid = Integer.parseInt(tmp[0]);
				int ammoCount = Integer.parseInt(tmp[1]);
				usedcapacity = usedcapacity.add(new BigDecimal(ammolist.get(aid).getString("dauer")).multiply(new BigDecimal(ammoCount)));
			}
	
			if( usedcapacity.compareTo(new BigDecimal(wf.getInt("count")-1)) > 0 ) {
				for( int i=0; i < plist.length; i++ ) {
					String[] tmp = StringUtils.split(plist[i], '=');
					int aid = Integer.parseInt(tmp[0]);
					int ammoCount = Integer.parseInt(tmp[1]);
					
					if( usedcapacity.subtract(new BigDecimal(ammoCount).multiply(new BigDecimal(ammolist.get(aid).getString("dauer")))).compareTo(new BigDecimal(wf.getInt("count")-1)) < 0 ) {
						plist[i] = aid+"="+
							(usedcapacity.subtract(new BigDecimal(wf.getInt("count")-1)).divide(new BigDecimal(ammolist.get(aid).getString("dauer"))));
						break;
					}
					plist[i] = aid+"=0";
					usedcapacity = usedcapacity.subtract(new BigDecimal(ammoCount).multiply(new BigDecimal(ammolist.get(aid).getString("dauer"))));
						
					if( usedcapacity.compareTo(new BigDecimal(wf.getInt("count")-1)) <= 0 ) break;
				}
				wf.put("produces", Common.implode(";",plist));
			}
				
			db.update("UPDATE weaponfactory SET count=count-1,produces='"+wf.getString("produces")+"' WHERE col=",col);
		} 
		else {
			db.update("DELETE FROM weaponfactory WHERE col=",col);
		}
	}

	@Override
	public String echoShortcut(Context context, int col, int field, int building) {
		Database db = context.getDatabase();
		
		String sess = context.getSession();
		
		StringBuilder result = new StringBuilder(200);
		
		loaddata( col );
		ContextVars vars = (ContextVars)ContextMap.getContext().getVariable(getClass(), "values");
	
		if( vars.usedcapacity.get(col).doubleValue() > 0 ) {
			SQLResultRow wf = db.first("SELECT produces FROM weaponfactory WHERE col=",col);
			String[] prodlist = StringUtils.split(wf.getString("produces"), ';');
			
			StringBuilder popup = new StringBuilder(200);
			popup.append(Common.tableBegin(350, "left").replace('"', '\'') );
				
			for( int i=0; i < prodlist.length; i++ ) {
				String[] prod = StringUtils.split(prodlist[i], '=');
				int aid = Integer.parseInt(prod[0]);
				int count = Integer.parseInt(prod[1]);
				if( (count > 0) && vars.ownerammobase.containsKey(aid) ) {
					popup.append(count+"x <img style='vertical-align:middle' src='"+Items.get().item(ammolist.get(aid).getInt("itemid")).getPicture()+"' alt='' />"+ammolist.get(aid).getString("name")+"<br />");
				}
			}
		
			popup.append(Common.tableEnd().replace('"', '\'') );
							
			result.append("<a name=\"p"+col+"_"+field+"\" id=\"p"+col+"_"+field+"\" class=\"error\" onmouseover=\"return overlib('<span style=\\'font-size:13px\\'>"+StringEscapeUtils.escapeJavaScript(popup.toString())+"</span>',REF,'p"+col+"_"+field+"',REFY,22,NOJUSTY,TIMEOUT,0,DELAY,150,WIDTH,260,BGCLASS,'gfxtooltip',FGCLASS,'gfxtooltip',TEXTFONTCLASS,'gfxtooltip');\" onmouseout=\"return nd();\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"\">[WF]</a>");
		} 
		else {
			result.append("<a class=\"back\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"\">[WF]</a>");
		}
	
		return result.toString();
	}

	@Override
	public boolean isActive(int col, int status, int field) {
		loaddata( col );
		ContextVars vars = (ContextVars)ContextMap.getContext().getVariable(getClass(), "values");
		if( vars.usedcapacity.get(col).doubleValue() > 0 ) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String modifyStats(int col, Cargo stats) {
		String msg = loaddata( col );
		
		Context context = ContextMap.getContext();
		ContextVars vars = (ContextVars)context.getVariable(getClass(), "values");
		Map<Integer,Boolean> colcomplete = (Map<Integer,Boolean>)context.getVariable(getClass(), "colcomplete");
		if( colcomplete == null ) {
			colcomplete = new HashMap<Integer,Boolean>();
			context.putVariable(getClass(), "colcomplete", colcomplete);
		}
		
		if( (vars.usedcapacity.get(col).compareTo(new BigDecimal(0)) > 0) && !colcomplete.containsKey(col) ) {
			stats.addCargo( vars.stats.get(col) );
			colcomplete.put(col, true);
		}
	
		return msg;
	}

	@Override
	public String output(Context context, TemplateEngine t, int col, int field, int building) {
		Database db = context.getDatabase();
		User user = context.getActiveUser();
		
		String sess = context.getSession();
		
		int produce = context.getRequest().getParameterInt("produce");
		int count = context.getRequest().getParameterInt("count");
		
		StringBuilder echo = new StringBuilder(2000);
		
		SQLResultRow wf = db.first("SELECT * FROM weaponfactory WHERE col=",col);
		
		if( wf.isEmpty() ) {
			echo.append("<div style=\"color:red\">FEHLER: Diese Waffenfabrik besitzt keinen Eintrag<br /></div>\n");
			return echo.toString();
		}
		/*
			Liste der baubaren Munition zusammenstellen
		*/
		
		Map<Integer,SQLResultRow> ammolist = new HashMap<Integer,SQLResultRow>();
		List<Integer> removelist = new ArrayList<Integer>();
		
		for( SQLResultRow ammoRow : Waffenfabrik.ammolist.values() ) {
			SQLResultRow ammo = new SQLResultRow();
			ammo.putAll(ammoRow);

			if( !user.hasResearched(ammo.getInt("res1")) || !user.hasResearched(ammo.getInt("res2")) || !user.hasResearched(ammo.getInt("res3")) )
				continue;
			
			ammo.put("_bauplan", "");
			ammolist.put(ammo.getInt("id"), ammo );
			
			if( (ammo.getInt("replaces") != 0) && !removelist.contains(ammo.getInt("replaces")) ) {
				removelist.add(ammo.getInt("replaces"));	
			}
		}
		
		Cargo cargo = new Cargo( Cargo.Type.STRING, db.first("SELECT cargo FROM bases WHERE id=",col).getString("cargo"));
		
		// Lokale Ammobauplaene ermitteln
		List<ItemCargoEntry> itemlist = cargo.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO );
		for( ItemCargoEntry item : itemlist ) {
			Item itemobject = item.getItemObject();
			final int ammoid = ((IEAmmo)itemobject.getEffect()).getAmmoID();
			
			if( !ammolist.containsKey(ammoid) ) {
				SQLResultRow ammo = Waffenfabrik.ammolist.get(ammoid);
				ammo.put("_bauplan", "<span class=\"smallfont\" style=\"color:#EECC44\">[Item]</span> ");
				ammolist.put(ammoid, ammo);
				if( (ammo.getInt("replaces") != 0) && !removelist.contains(ammo.getInt("replaces")) ) {
					removelist.add(ammo.getInt("replaces"));
				}
			}
		}
		
		// Moegliche Allybauplaene ermitteln
		if( user.getAlly() > 0 ) {
			Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, db.first("SELECT items FROM ally WHERE id=",user.getAlly()).getString("items") );
			
			itemlist = allyitems.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO );
			for( ItemCargoEntry item : itemlist ) {
				Item itemobject = item.getItemObject();
				final int ammoid = ((IEAmmo)itemobject.getEffect()).getAmmoID();
				
				if( !ammolist.containsKey(ammoid) ) {
					SQLResultRow ammo = Waffenfabrik.ammolist.get(ammoid);
					ammo.put("_bauplan", "<span class=\"smallfont\" style=\"color:#44EE44\">[Item]</span> ");
					ammolist.put(ammoid, ammo);
					if( (ammo.getInt("replaces") != 0) && !removelist.contains(ammo.getInt("replaces")) ) {
						removelist.add(ammo.getInt("replaces"));
					}
				}
			}
		}
		
		// Alle Raks entfernen, die durch andere Raks ersetzt wurden (DB-Feld: replaces)
		if( !removelist.isEmpty() ) {
			for( int i=0; i < removelist.size(); i++ ) {
				ammolist.remove(removelist.get(i));	
			}	
		}
		
		/*
			Neue Bauauftraege behandeln
		*/
		
		echo.append("<div class=\"smallfont\">");
		if( (produce != 0) && (count != 0) ) {
			SQLResultRow ammo = Waffenfabrik.ammolist.get(produce);
			
			if( ammo == null ) {
				echo.append("<span style=\"color:red\">Fehler: Der angegebene Munitionstyp existiert nicht</span>\n");
				return echo.toString();
			}
			if( Items.get().item(ammo.getInt("itemid")).getEffect().getType() != ItemEffect.Type.AMMO ) {
				echo.append("<span style=\"color:red\">Fehler: Das angegebene Item ist keine Munition</span>\n");
				return echo.toString();
			}
		
			if( ammolist.containsKey(ammo.getInt("id")) ) {
				BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
				Map<Integer,Integer> productlist = new HashMap<Integer,Integer>();
		
				String[] plist = StringUtils.split(wf.getString("produces"), ';');
				for( int i=0; i < plist.length; i++ ) {
					String[] tmp = StringUtils.split(plist[i], '=');
					int aid = Integer.parseInt(tmp[0]);
					int ammoCount = Integer.parseInt(tmp[1]);
					
					usedcapacity = usedcapacity.add(new BigDecimal(ammolist.get(aid).getString("dauer")).multiply(new BigDecimal(ammoCount)));
					productlist.put(aid, ammoCount);
				}
				if( usedcapacity.add(new BigDecimal(count*ammo.getDouble("dauer"))).doubleValue() > wf.getInt("count") ) {
					count = usedcapacity.multiply(new BigDecimal(-1)).add(new BigDecimal(wf.getInt("count"))).divide(new BigDecimal(ammo.getString("dauer"))).intValue();
				}
			
				boolean entry = false;
				List<String> producelist = Arrays.asList(StringUtils.split(wf.getString("produces"), ';'));
				
				for( int i=0; i < producelist.size(); i++ ) {
					String[] tmp = StringUtils.split(producelist.get(i), '=');
					int aid = Integer.parseInt(tmp[0]);
					int ammoCount = Integer.parseInt(tmp[1]);
					
					// Veraltete Ammo automatisch entfernen
					if( removelist.contains(aid) ) {
						producelist.remove(i);
						continue;	
					}
					
					if( (aid == 0) || (ammoCount <= 0) ) {
						producelist.remove(i);
						continue;
					}
					
					if( aid == ammo.getInt("id") ) {
						if( (count < 0) && (ammoCount+count < 0) ) {
							count = -ammoCount;
						}
						ammoCount += count;
						entry = true;
					}
					producelist.set(i, aid+"="+ammoCount);
				}
				if( !entry ) {
					if( count < 0 ) {
						count = 0;
					}
					producelist.add(ammo.getInt("id")+"="+count);
				}
				
				wf.put("produces", Common.implode(";",producelist));
				
				db.update("UPDATE weaponfactory SET produces='"+wf.getString("produces")+"' WHERE id="+wf.getInt("id"));
		
				echo.append(Math.abs(count)+" "+Items.get().item(ammo.getInt("itemid")).getName()+" wurden "+(count>=0 ? "hinzugef&uuml;gt":"abgezogen")+"<br /><br />");
			} 
			else {
				echo.append("Sie haben nicht alle ben&ouml;tigten Forschungen f&uuml;r "+ammo.getString("name")+"<br /><br />");
			}
		}
		
		/*
			Aktuelle Bauauftraege ermitteln
		*/
		// Warum BigDecimal? Weil 0.05 eben nicht 0.05000000074505806 ist (Ungenauigkeit von double/float)....
		BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
		Map<Integer,Integer> productlist = new HashMap<Integer,Integer>();
		Cargo consumes = new Cargo();
		
		if( wf.getString("produces").length() != 0 ) {
			String[] plist = StringUtils.split(wf.getString("produces"), ';');
			for( int i=0; i < plist.length; i++ ) {
				String[] tmp = StringUtils.split(plist[i], '=');
				int aid = Integer.parseInt(tmp[0]);
				int ammoCount = Integer.parseInt(tmp[1]);
				
				if( !ammolist.containsKey(aid) ) {
					echo.append("WARNUNG: Ungueltige Ammo >"+aid+"< (count: "+ammoCount+") in der Produktionsliste entdeckt<br />\n");
					continue;	
				}
				
				usedcapacity = usedcapacity.add(new BigDecimal(ammolist.get(aid).getString("dauer")).multiply(new BigDecimal(ammoCount)));
			
				// Veraltete Ammo ignorieren
				if( removelist.contains(aid) ) {
					continue;	
				}
			
				if( ammoCount > 0 ) {
					Cargo tmpcargo = (Cargo)((Cargo)ammolist.get(aid).get("buildcosts")).clone();
					if( ammoCount > 1 ) {
						tmpcargo.multiply( ammoCount, Cargo.Round.NONE );
					}
					consumes.addCargo( tmpcargo );
				}
				productlist.put(aid, ammoCount);
			}
		}
		echo.append("</div>\n");
		
		/*
			Ausgabe: Verbrauch, Auftraege, Liste baubarer Munitionstypen
		*/
		echo.append(Common.tableBegin(760, "left"));
		
		echo.append("<img style=\"vertical-align:middle\" src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"Zeiteinheiten\" />"+usedcapacity+"/"+wf.getInt("count")+" ausgelastet<br />\n");
		echo.append("Verbrauch: ");
		ResourceList reslist = consumes.getResourceList();
		for( ResourceEntry res : reslist ) {
			echo.append("<img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+"&nbsp;");
		}
		echo.append("<br /><br />\n");
		echo.append("<table class=\"noBorderX\" cellpadding=\"2\">");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:20px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"font-weight:bold\">Objekt</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"font-weight:bold\">Kosten</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:130px\"></td>\n");
		echo.append("</tr>");
		
		for( Item item : Items.get() ) {
			if( item.getEffect().getType() != ItemEffect.Type.AMMO ) continue;
			
			final int ammoid = ((IEAmmo)item.getEffect()).getAmmoID();
			
			if( !ammolist.containsKey(ammoid) ) continue;
		
			echo.append("<tr>\n");
			if( productlist.containsKey(ammoid) ) {
				echo.append("<td class=\"noBorderX\" valign=\"top\">"+productlist.get(ammoid)+"x</td>\n");
			} 
			else {
				echo.append("<td class=\"noBorderX\" valign=\"top\">-</td>\n");
			}
			
			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");
			echo.append(ammolist.get(ammoid).getString("_bauplan"));
			echo.append("<img style=\"vertical-align:middle\" src=\""+item.getPicture()+"\" alt=\"\" /><a class=\"forschinfo\" href=\"./main.php?module=iteminfo&amp;sess="+sess+"&amp;action=details&amp;item="+item.getID()+"\">"+item.getName()+"</a>");
			echo.append("</td>\n");
			
			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");
			echo.append("<img style=\"vertical-align:middle\" src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+ammolist.get(ammoid).getString("dauer")+" \n");
			
			reslist = ((Cargo)ammolist.get(ammoid).get("buildcosts")).getResourceList();
			for( ResourceEntry res : reslist ) {
				echo.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+"</span>\n");
			}
		
			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:130px\">\n");
			echo.append("<form action=\"./main.php\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"text\" size=\"2\" value=\"0\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\""+ammoid+"\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\""+col+"\" />\n");
			echo.append("<input name=\"sess\" type=\"hidden\" value=\""+sess+"\" />\n");
			echo.append("<input name=\"field\" type=\"hidden\" value=\""+field+"\" />\n");
			echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
			echo.append("<input type=\"submit\" value=\"herstellen\" />\n");
			echo.append("</div>\n");
			echo.append("</form></td></tr>\n");
		}
		
		echo.append("</table><br />\n");
		echo.append(Common.tableEnd());
		echo.append( "<div><br /></div>\n");
		
		return echo.toString();
	}	
}
