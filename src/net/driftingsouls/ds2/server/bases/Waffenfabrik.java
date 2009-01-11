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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.config.items.effects.IEAmmo;
import net.driftingsouls.ds2.server.config.items.effects.IEDraftAmmo;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WeaponFactory;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextInstance;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Immutable;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Die Waffenfabrik.
 * @author Christopher Jung
 *
 */
@Entity(name="WaffenfabrikBuilding")
@Immutable
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.Waffenfabrik")
@Configurable
public class Waffenfabrik extends DefaultBuilding {
	private static final Log log = LogFactory.getLog(Waffenfabrik.class);
	
	/**
	 * Daten von einer oder mehreren Waffenfabriken.
	 */
	@ContextInstance(ContextInstance.Scope.REQUEST)
	public static class ContextVars {
		Set<Ammo> ownerammobase = new HashSet<Ammo>();
		Map<Integer,Cargo> stats = new HashMap<Integer,Cargo>();
		Map<Integer,BigDecimal> usedcapacity = new HashMap<Integer,BigDecimal>();
		boolean init = false;
		
		/**
		 * Konstruktor.
		 */
		public ContextVars() {
			// EMPTY
		}
	}
	
	/**
	 * Erstellt eine neue Instanz der Waffenfabrik.
	 */
	public Waffenfabrik() {
		// EMPTY
	}
	
	private String loaddata( Base base ) {
		Context context = ContextMap.getContext();
		
		User user = base.getOwner();
		
		ContextVars vars = context.get(ContextVars.class);
		Integer lastUser = (Integer)context.getVariable(getClass(), "last_user");
		
		List<Ammo> removelist = new ArrayList<Ammo>();
		
		if( (vars.init == false) || (user.getId() != lastUser.intValue()) ) {
			vars.init = true;
			context.putVariable(getClass(), "last_user", user.getId());
		
			removelist = loadOwnerAmmoBase(user, vars);
		}
		
		if( !vars.usedcapacity.containsKey(base.getId()) ) {
			return loadAmmoTasks(base, vars, removelist);
		}
		
		return "";
	}

	private String loadAmmoTasks(Base base, ContextVars vars, List<Ammo> removelist) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		StringBuilder wfreason = new StringBuilder(100);
		
		if( !vars.stats.containsKey(base.getId()) ) {
			vars.stats.put(base.getId(), new Cargo());
		}
		
		boolean ok = true;
		Set<Ammo> thisammolist = vars.ownerammobase;	
		
		Cargo cargo = base.getCargo();
		
		List<ItemCargoEntry> list = cargo.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO ) ;
		for( ItemCargoEntry item : list ) {
			IEDraftAmmo itemeffect = (IEDraftAmmo)item.getItemEffect();
			Ammo ammo = itemeffect.getAmmo();
			
			thisammolist.add(ammo);
		}
		
		WeaponFactory wf = (WeaponFactory)db.get(WeaponFactory.class, base.getId());
		if( wf == null ) {
			vars.usedcapacity.put(base.getId(), BigDecimal.valueOf(-1));
			
			log.warn("Basis "+base.getId()+" verfuegt ueber keinen Waffenfabrik-Eintrag, obwohl es eine Waffenfabrik hat");
			return "Basis "+base.getId()+" verfuegt ueber keinen Waffenfabrik-Eintrag, obwohl es eine Waffenfabrik hat";
		}
		WeaponFactory.Task[] plist = wf.getProduces();
		for( int i=0; i < plist.length; i++ ) {
			Ammo ammo = plist[i].getAmmo();
			int count = plist[i].getCount();
			
			if( ammo == null ) {
				plist = (WeaponFactory.Task[])ArrayUtils.remove(plist, i);
				i--;
				continue;
			}
			
			// Ammo ohne Plaene melden - veraltete Ammo aber ignorieren!
			if( (count > 0) && !thisammolist.contains(ammo) && !removelist.contains(ammo) ) {
				ok = false;
				wfreason.append("Es existieren nicht die n&ouml;tigen Baupl&auml;ne f&uuml;r "+ammo.getName()+"\n");
				break;
			}
			else if( (count > 0) && !thisammolist.contains(ammo) ) {
				plist = (WeaponFactory.Task[])ArrayUtils.remove(plist, i);
				i--;
			}
		}
		 	
		if( ok ) {
			for( int i=0; i < plist.length; i++  ) {
				Ammo ammo = plist[i].getAmmo();
				int count = plist[i].getCount();
				
				if( !vars.usedcapacity.containsKey(base.getId()) ) {
					vars.usedcapacity.put(base.getId(), new BigDecimal(0, MathContext.DECIMAL32));
				}
				vars.usedcapacity.put(base.getId(), vars.usedcapacity.get(base.getId()).add(ammo.getDauer().multiply((new BigDecimal(count)))) );
				if( count > 0 ) {
					Cargo tmpcargo = new Cargo(ammo.getBuildCosts());
					if( count > 1 ) {
						tmpcargo.multiply( count, Cargo.Round.NONE );
					}
					vars.stats.get(base.getId()).substractCargo( tmpcargo );
					vars.stats.get(base.getId()).addResource( new ItemID(ammo.getItemId()), count );
				}
			}
		}
		else {
			String basename = base.getName();
			wfreason.insert(0, "[b]"+basename+"[/b] - Die Arbeiten in der Waffenfabrik zeitweise eingestellt.\nGrund:\n");
		}
		
		if( !vars.usedcapacity.containsKey(base.getId()) || (vars.usedcapacity.get(base.getId()).doubleValue() <= 0) ) {
			vars.usedcapacity.put(base.getId(), new BigDecimal(-1));
		}
		
		return wfreason.toString();
	}

	private List<Ammo> loadOwnerAmmoBase(User user, ContextVars vars) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		List<Ammo> removelist = new ArrayList<Ammo>();
		
		// Iterator, da sich die Ammo-Objekte sich mit hoher Wahrscheinlichkeit
		// bereits im Cache befinden
		Iterator<?> ammoIter = db.createQuery("from Ammo").list().iterator();
		for( ; ammoIter.hasNext(); ) {
			Ammo ammo = (Ammo)ammoIter.next();
			if( !user.hasResearched(ammo.getRes1()) || !user.hasResearched(ammo.getRes2()) || !user.hasResearched(ammo.getRes3()) ) {
				continue;
			}
			vars.ownerammobase.add(ammo);
			
			if( (ammo.getReplaces() != null) && !removelist.contains(ammo.getReplaces()) ) {
				removelist.add(ammo.getReplaces());
			}
		}
		
		if( user.getAlly() != null ) {
			Cargo itemlist = new Cargo( Cargo.Type.ITEMSTRING, user.getAlly().getItems() );	
			
			List<ItemCargoEntry> list = itemlist.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO ) ;
			for( ItemCargoEntry item : list ) {
				IEDraftAmmo itemeffect = (IEDraftAmmo)item.getItemEffect();
				Ammo ammo = itemeffect.getAmmo();
				
				vars.ownerammobase.add(ammo);
				
				if( (ammo.getReplaces() != null) && removelist.contains(ammo.getReplaces()) ) {
					removelist.add(ammo.getReplaces());
				}
			}
		}
		
		// Alle Raks entfernen, die durch andere Raks ersetzt wurden (DB-Feld: replaces)
		if( !removelist.isEmpty() ) {
			for( Ammo removeentry : removelist ) {
				vars.ownerammobase.remove(removeentry);
			}
		}
		
		return removelist;
	}

	@Override
	public void build(Base base) {
		super.build(base);
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		WeaponFactory wf = (WeaponFactory)db.get(WeaponFactory.class, base.getId());
		
		if( wf != null ) {
			wf.setCount(wf.getCount()+1);
		} 
		else {
			wf = new WeaponFactory(base);
			db.persist(wf);
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
	public void cleanup(Context context, Base base) {
		org.hibernate.Session db = context.getDB();
		
		WeaponFactory wf = (WeaponFactory)db.get(WeaponFactory.class, base.getId());
		if( wf == null ) {
			return;
		}
		
		if( wf.getCount() > 1 ) {	
			BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
	
			WeaponFactory.Task[] plist = wf.getProduces();
			for( int i=0; i < plist.length; i++ ) {
				Ammo ammo = plist[i].getAmmo();
				int ammoCount = plist[i].getCount();
				
				usedcapacity = usedcapacity.add(ammo.getDauer().multiply(new BigDecimal(ammoCount)));
			}
	
			if( usedcapacity.compareTo(new BigDecimal(wf.getCount()-1)) > 0 ) {
				BigDecimal targetCapacity = new BigDecimal(wf.getCount()-1);
				
				for( int i=0; i < plist.length; i++ ) {
					Ammo ammo = plist[i].getAmmo();
					int ammoCount = plist[i].getCount();
					
					BigDecimal capUsedByAmmo = new BigDecimal(ammoCount).multiply(ammo.getDauer());
					
					if( usedcapacity.subtract(capUsedByAmmo).compareTo(targetCapacity) < 0 ) {
						BigDecimal capLeftForAmmo = capUsedByAmmo.subtract(usedcapacity.subtract(targetCapacity));
						plist[i] = new WeaponFactory.Task(ammo, capLeftForAmmo.divide(ammo.getDauer(), BigDecimal.ROUND_DOWN).intValue());
						break;
					}
					plist = (WeaponFactory.Task[])ArrayUtils.remove(plist, i);
					i--;
					
					usedcapacity = usedcapacity.subtract(capUsedByAmmo);
						
					if( usedcapacity.compareTo(targetCapacity) <= 0 ) break;
				}
				wf.setProduces(plist);
			}
			
			wf.setCount(wf.getCount()-1);
		} 
		else {
			db.delete(wf);
		}
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();

		StringBuilder result = new StringBuilder(200);
		
		loaddata( base );
		ContextVars vars = ContextMap.getContext().get(ContextVars.class);
	
		if( vars.usedcapacity.get(base.getId()).doubleValue() > 0 ) {
			WeaponFactory wf = (WeaponFactory)db.get(WeaponFactory.class, base.getId());
			WeaponFactory.Task[] prodlist = wf.getProduces();
			
			StringBuilder popup = new StringBuilder(200);
			popup.append(Common.tableBegin(350, "left").replace('"', '\'') );
				
			for( int i=0; i < prodlist.length; i++ ) {
				Ammo ammo = prodlist[i].getAmmo();
				int count = prodlist[i].getCount();
				if( (count > 0) && vars.ownerammobase.contains(ammo) ) {
					popup.append(count+"x <img style='vertical-align:middle' src='"+Items.get().item(ammo.getItemId()).getPicture()+"' alt='' />"+ammo.getName()+"<br />");
				}
			}
		
			popup.append(Common.tableEnd().replace('"', '\'') );
							
			result.append("<a name=\"p"+base.getId()+"_"+field+"\" id=\"p"+base.getId()+"_"+field+"\" " +
					"class=\"error\" " +
					"onmouseover=\"return overlib('<span style=\\'font-size:13px\\'>"+StringEscapeUtils.escapeJavaScript(popup.toString())+"</span>',REF,'p"+base.getId()+"_"+field+"',REFY,22,NOJUSTY,TIMEOUT,0,DELAY,150,WIDTH,260,BGCLASS,'gfxtooltip',FGCLASS,'gfxtooltip',TEXTFONTCLASS,'gfxtooltip');\" " +
					"onmouseout=\"return nd();\" " +
					"href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"\">[WF]</a>");
		} 
		else {
			result.append("<a class=\"back\" href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"\">[WF]</a>");
		}
	
		return result.toString();
	}

	@Override
	public boolean isActive(Base base, int status, int field) {
		loaddata( base );
		ContextVars vars = ContextMap.getContext().get(ContextVars.class);
		if( vars.usedcapacity.get(base.getId()).doubleValue() > 0 ) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String modifyStats(Base base, Cargo stats) {
		String msg = loaddata( base );
		
		Context context = ContextMap.getContext();
		ContextVars vars = context.get(ContextVars.class);
		Map<Integer,Boolean> colcomplete = (Map<Integer,Boolean>)context.getVariable(getClass(), "colcomplete");
		if( colcomplete == null ) {
			colcomplete = new HashMap<Integer,Boolean>();
			context.putVariable(getClass(), "colcomplete", colcomplete);
		}
		
		if( (vars.usedcapacity.get(base.getId()).compareTo(new BigDecimal(0)) > 0) && !colcomplete.containsKey(base.getId()) ) {
			stats.addCargo( vars.stats.get(base.getId()) );
			colcomplete.put(base.getId(), true);
		}
	
		return msg;
	}

	@Override
	public String output(Context context, TemplateEngine t, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();
		User user = (User)context.getActiveUser();

		int produce = context.getRequest().getParameterInt("produce");
		int count = context.getRequest().getParameterInt("count");
		
		StringBuilder echo = new StringBuilder(2000);
		
		WeaponFactory wf = (WeaponFactory)db.get(WeaponFactory.class, base.getId());
		
		if( wf == null ) {
			echo.append("<div style=\"color:red\">FEHLER: Diese Waffenfabrik besitzt keinen Eintrag<br /></div>\n");
			return echo.toString();
		}
		/*
			Liste der baubaren Munition zusammenstellen
		*/
		
		Set<Ammo> ammolist = new HashSet<Ammo>();
		Map<Ammo, String> bPlanMap = new HashMap<Ammo, String>();
		List<Ammo> removelist = new ArrayList<Ammo>();
		
		Iterator<?> ammoIter = db.createQuery("from Ammo").list().iterator();
		for( ; ammoIter.hasNext(); ) {
			Ammo ammo = (Ammo)ammoIter.next();

			if( !user.hasResearched(ammo.getRes1()) || !user.hasResearched(ammo.getRes2()) || !user.hasResearched(ammo.getRes3()) )
				continue;
			
			ammolist.add(ammo);
			
			if( (ammo.getReplaces() != null) && !removelist.contains(ammo.getReplaces()) ) {
				removelist.add(ammo.getReplaces());	
			}
		}
		
		Cargo cargo = base.getCargo();
		
		// Lokale Ammobauplaene ermitteln
		List<ItemCargoEntry> itemlist = cargo.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO );
		for( ItemCargoEntry item : itemlist ) {
			Item itemobject = item.getItemObject();
			final Ammo ammo = ((IEDraftAmmo)itemobject.getEffect()).getAmmo();
			
			if( !ammolist.contains(ammo) ) {
				bPlanMap.put(ammo, "<span class=\"smallfont\" style=\"color:#EECC44\">[Item]</span> ");
				ammolist.add(ammo);
				if( (ammo.getReplaces() != null) && !removelist.contains(ammo.getReplaces()) ) {
					removelist.add(ammo.getReplaces());
				}
			}
		}
		
		// Moegliche Allybauplaene ermitteln
		if( user.getAlly() != null ) {
			Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, user.getAlly().getItems() );
			
			itemlist = allyitems.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO );
			for( ItemCargoEntry item : itemlist ) {
				Item itemobject = item.getItemObject();
				final Ammo ammo = ((IEDraftAmmo)itemobject.getEffect()).getAmmo();
				
				if( !ammolist.contains(ammo) ) {
					bPlanMap.put(ammo, "<span class=\"smallfont\" style=\"color:#44EE44\">[Item]</span> ");
					ammolist.add(ammo);
					if( (ammo.getReplaces() != null) && !removelist.contains(ammo.getReplaces()) ) {
						removelist.add(ammo.getReplaces());
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
			final Ammo ammo = (Ammo)db.get(Ammo.class, produce);
			
			if( ammo == null ) {
				echo.append("<span style=\"color:red\">Fehler: Der angegebene Munitionstyp existiert nicht</span>\n");
				return echo.toString();
			}
			if( Items.get().item(ammo.getItemId()).getEffect().getType() != ItemEffect.Type.AMMO ) {
				echo.append("<span style=\"color:red\">Fehler: Das angegebene Item ist keine Munition</span>\n");
				return echo.toString();
			}
		
			if( ammolist.contains(ammo) ) {
				BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
		
				WeaponFactory.Task[] plist = wf.getProduces();
				for( int i=0; i < plist.length; i++ ) {
					final Ammo aAmmo = plist[i].getAmmo();
					final int ammoCount = plist[i].getCount();
					
					usedcapacity = usedcapacity.add(aAmmo.getDauer().multiply(new BigDecimal(ammoCount)));
				}
				if( usedcapacity.add(new BigDecimal(count).multiply(ammo.getDauer())).doubleValue() > wf.getCount() ) {
					BigDecimal availableCap = usedcapacity.multiply(new BigDecimal(-1)).add(new BigDecimal(wf.getCount()));
					count = availableCap.divide(ammo.getDauer(), BigDecimal.ROUND_DOWN).intValue();
				}
			
				if( count != 0 ) {
					boolean entry = false;
					List<WeaponFactory.Task> producelist = new ArrayList<WeaponFactory.Task>(
							Arrays.asList(wf.getProduces())
					);
					
					for( int i=0; i < producelist.size(); i++ ) {
						Ammo aAmmo = producelist.get(i).getAmmo();
						int ammoCount = producelist.get(i).getCount();
						
						// Veraltete Ammo automatisch entfernen
						if( removelist.contains(aAmmo) ) {
							producelist.remove(i);
							i--;
							continue;	
						}
						
						if( (aAmmo == null) || (ammoCount <= 0) ) {
							producelist.remove(i);
							i--;
							continue;
						}
						
						if( aAmmo == ammo ) {
							if( (count < 0) && (ammoCount+count < 0) ) {
								count = -ammoCount;
							}
							ammoCount += count;
							entry = true;
						}
						if( ammoCount > 0 ) {
							producelist.set(i, new WeaponFactory.Task(aAmmo, ammoCount));
						}
						else {
							producelist.remove(i);
							i--;
						}
					}
					if( !entry && (count > 0) ) {
						producelist.add(new WeaponFactory.Task(ammo, count));
					}
					
					wf.setProduces(producelist.toArray(new WeaponFactory.Task[producelist.size()]));
			
					echo.append(Math.abs(count)+" "+Items.get().item(ammo.getItemId()).getName()+" wurden "+(count>=0 ? "hinzugef&uuml;gt":"abgezogen")+"<br /><br />");
				}
			} 
			else {
				echo.append("Sie haben nicht alle ben&ouml;tigten Forschungen f&uuml;r "+ammo.getName()+"<br /><br />");
			}
		}
		
		/*
			Aktuelle Bauauftraege ermitteln
		*/
		// Warum BigDecimal? Weil 0.05 eben nicht 0.05000000074505806 ist (Ungenauigkeit von double/float)....
		BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
		Map<Ammo,Integer> productlist = new HashMap<Ammo,Integer>();
		Cargo consumes = new Cargo();
		
		if( wf.getProduces().length > 0 ) {
			WeaponFactory.Task[] plist = wf.getProduces();
			for( int i=0; i < plist.length; i++ ) {
				final Ammo ammo = plist[i].getAmmo();
				final int ammoCount = plist[i].getCount();
				
				if( !ammolist.contains(ammo) ) {
					echo.append("WARNUNG: Ungueltige Ammo >"+ammo.getId()+"< (count: "+ammoCount+") in der Produktionsliste entdeckt<br />\n");
					continue;	
				}
				
				usedcapacity = usedcapacity.add(ammo.getDauer().multiply(new BigDecimal(ammoCount)));
			
				// Veraltete Ammo ignorieren
				if( removelist.contains(ammo) ) {
					continue;	
				}
			
				if( ammoCount > 0 ) {
					Cargo tmpcargo = new Cargo(ammo.getBuildCosts());
					if( ammoCount > 1 ) {
						tmpcargo.multiply( ammoCount, Cargo.Round.NONE );
					}
					consumes.addCargo( tmpcargo );
				}
				productlist.put(ammo, ammoCount);
			}
		}
		echo.append("</div>\n");
		
		/*
			Ausgabe: Verbrauch, Auftraege, Liste baubarer Munitionstypen
		*/
		echo.append(Common.tableBegin(760, "left"));
		
		echo.append("<img style=\"vertical-align:middle\" src=\""+config.get("URL")+"data/interface/time.gif\" alt=\"Zeiteinheiten\" />"+usedcapacity+"/"+wf.getCount()+" ausgelastet<br />\n");
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
			
			final Ammo ammo = ((IEAmmo)item.getEffect()).getAmmo();
			
			if( !ammolist.contains(ammo) ) {
				continue;
			}
		
			echo.append("<tr>\n");
			if( productlist.containsKey(ammo) ) {
				echo.append("<td class=\"noBorderX\" valign=\"top\">"+productlist.get(ammo)+"x</td>\n");
			} 
			else {
				echo.append("<td class=\"noBorderX\" valign=\"top\">-</td>\n");
			}
			
			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");
			if( bPlanMap.containsKey(ammo) ) {
				echo.append(bPlanMap.get(ammo));
			}
			echo.append("<img style=\"vertical-align:middle\" src=\""+item.getPicture()+"\" alt=\"\" /><a class=\"forschinfo\" href=\"./ds?module=iteminfo&amp;action=details&amp;item="+item.getID()+"\">"+item.getName()+"</a>");
			echo.append("</td>\n");
			
			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");
			echo.append("<img style=\"vertical-align:middle\" src=\""+config.get("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+ammo.getDauer()+" \n");
			
			reslist = ammo.getBuildCosts().getResourceList();
			for( ResourceEntry res : reslist ) {
				echo.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+"</span>\n");
			}
		
			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:130px\">\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"text\" size=\"2\" value=\"0\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\""+ammo.getId()+"\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\""+base.getId()+"\" />\n");
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
