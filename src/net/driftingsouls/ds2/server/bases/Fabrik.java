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
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.IEDraftAmmo;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.Factory;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextInstance;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Die Fabrik.
 *
 */
@Entity(name="FabrikBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.Fabrik")
@Configurable
public class Fabrik extends DefaultBuilding {
	private static final Log log = LogFactory.getLog(Fabrik.class);
	
	/**
	 * Daten von einer oder mehreren Fabriken-Typen.
	 *
	 */
	@ContextInstance(ContextInstance.Scope.REQUEST)
	public static class AllContextVars {
		Map<Integer, ContextVars> allvars = new HashMap<Integer, ContextVars>();
		
		/**
		 * Konstruktor.
		 */
		public AllContextVars() {
			// EMPTY
		}
	}
	
	/**
	 * Daten von einer oder mehreren Fabriken.
	 */
	public static class ContextVars {
		Set<FactoryEntry> owneritemsbase = new HashSet<FactoryEntry>();
		Map<Integer,Cargo> stats = new HashMap<Integer,Cargo>();
		Map<Integer,Cargo> productionstats = new HashMap<Integer,Cargo>();
		Map<Integer,Cargo> consumptionstats = new HashMap<Integer,Cargo>();
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
	 * Erstellt eine neue Instanz der Fabriken.
	 */
	public Fabrik() {
		// EMPTY
	}
	
	private String loaddata( Base base, int buildingid ) {
		Context context = ContextMap.getContext();
		
		User user = base.getOwner();
		
		AllContextVars vars = context.get(AllContextVars.class);
		Integer lastUser = (Integer)context.getVariable(getClass(), "last_user");
		
		if( !vars.allvars.containsKey(buildingid) || (vars.allvars.get(buildingid).init == false) ||(user.getId() != lastUser.intValue()) ) {
			vars.allvars.put(buildingid, new ContextVars());
			vars.allvars.get(buildingid).init = true;
			context.putVariable(getClass(), "last_user", user.getId());
			
			loadOwnerBase( user, vars, buildingid );
		}
		
		if( !vars.allvars.get(buildingid).usedcapacity.containsKey(base.getId()) ) {
			return loadAmmoTasks(base, vars, buildingid);
		}
		
		return "";
	}
	
	private String loadAmmoTasks(Base base, AllContextVars vars, int buildingid) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		StringBuilder wfreason = new StringBuilder(100);
		
		if( !vars.allvars.get(buildingid).stats.containsKey(base.getId()) ) {
			vars.allvars.get(buildingid).stats.put(base.getId(), new Cargo());
		}
		if( !vars.allvars.get(buildingid).productionstats.containsKey(base.getId()) ) {
			vars.allvars.get(buildingid).productionstats.put(base.getId(), new Cargo());
		}
		if( !vars.allvars.get(buildingid).consumptionstats.containsKey(base.getId()) ) {
			vars.allvars.get(buildingid).consumptionstats.put(base.getId(), new Cargo());
		}
		
		boolean ok = true;
		Set<FactoryEntry> thisitemslist = vars.allvars.get(buildingid).owneritemsbase;	
		
		Cargo cargo = base.getCargo();
		
		List<ItemCargoEntry> list = cargo.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO ) ;
		for( ItemCargoEntry item : list ) {
			IEDraftAmmo itemeffect = (IEDraftAmmo)item.getItemEffect();
			FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, itemeffect.getAmmo().getId());
			
			thisitemslist.add(entry);
		}
		
		Factory wf = loadFactoryEntity(base, buildingid);
		
		if( wf == null ) {
			vars.allvars.get(buildingid).usedcapacity.put(base.getId(), BigDecimal.valueOf(-1));
			
			log.warn("Basis "+base.getId()+" verfuegt ueber keinen Fabrik-Eintrag, obwohl es eine Fabrik hat");
			return "Basis "+base.getId()+" verfuegt ueber keinen Fabrik-Eintrag, obwohl es eine Fabrik hat";
		}
		
		Factory.Task[] plist = wf.getProduces();
		for( int i=0; i < plist.length; i++ ) {
			int id = plist[i].getId();
			int count = plist[i].getCount();
			
			FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, id);
			if( entry == null ) {
				plist = (Factory.Task[])ArrayUtils.remove(plist, i);
				i--;
				continue;
			}
			
			// Items ohne Plaene melden
			if( (count > 0) && !thisitemslist.contains(entry)) {
				ok = false;
				wfreason.append("Es existieren nicht die n&ouml;tigen Baupl&auml;ne f&uuml;r "+entry.getName()+"\n");
				break;
			}
		}
		
		if( ok ) {
			for( int i=0; i < plist.length; i++  ) {
				int id = plist[i].getId();
				int count = plist[i].getCount();
				
				FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, id);
				
				if( !vars.allvars.get(buildingid).usedcapacity.containsKey(base.getId()) ) {
					vars.allvars.get(buildingid).usedcapacity.put(base.getId(), new BigDecimal(0, MathContext.DECIMAL32));
				}
				vars.allvars.get(buildingid).usedcapacity.put(base.getId(), vars.allvars.get(buildingid).usedcapacity.get(base.getId()).add(entry.getDauer().multiply((new BigDecimal(count)))) );
				if( count > 0 ) {
					Cargo tmpcargo = new Cargo(entry.getBuildCosts());
					if( count > 1 ) {
						tmpcargo.multiply( count, Cargo.Round.NONE );
					}
					vars.allvars.get(buildingid).consumptionstats.get(base.getId()).addCargo( tmpcargo );
					vars.allvars.get(buildingid).stats.get(base.getId()).substractCargo( tmpcargo );
					Cargo addCargo = entry.getProduce();
					addCargo.multiply(count, Cargo.Round.FLOOR);
					vars.allvars.get(buildingid).stats.get(base.getId()).addCargo( addCargo );
					vars.allvars.get(buildingid).productionstats.get(base.getId()).addCargo( addCargo );
				}
			}
		}
		else {
			String basename = base.getName();
			wfreason.insert(0, "[b]"+basename+"[/b] - Die Arbeiten in der Fabrik zeitweise eingestellt.\nGrund:\n");
		}
		
		if( !vars.allvars.get(buildingid).usedcapacity.containsKey(base.getId()) || (vars.allvars.get(buildingid).usedcapacity.get(base.getId()).doubleValue() <= 0) ) {
			vars.allvars.get(buildingid).usedcapacity.put(base.getId(), new BigDecimal(-1));
		}
		
		return wfreason.toString();
	}

	private Factory loadFactoryEntity(Base base, int buildingid)
	{
		for( Factory factory : base.getFactories() )
		{
			if( factory.getBuildingID() == buildingid )
			{
				return factory;
			}
		}
		return null;
	}

	private void loadOwnerBase(User user, AllContextVars vars, int buildingid) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		List<FactoryEntry> entrylist = Common.cast(db.createQuery("from FactoryEntry").list());
		for( FactoryEntry entry : entrylist ) {
			if( !user.hasResearched(entry.getRes1()) || !user.hasResearched(entry.getRes2()) || !user.hasResearched(entry.getRes3()) || !entry.hasBuildingId(buildingid)) {
				continue;
			}
			vars.allvars.get(buildingid).owneritemsbase.add(entry);
		}
		
		if( user.getAlly() != null ) {
			Cargo itemlist = new Cargo( Cargo.Type.ITEMSTRING, user.getAlly().getItems() );	
			
			List<ItemCargoEntry> list = itemlist.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO ) ;
			for( ItemCargoEntry item : list ) {
				IEDraftAmmo itemeffect = (IEDraftAmmo)item.getItemEffect();
				FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, itemeffect.getAmmo().getId());
				
				vars.allvars.get(buildingid).owneritemsbase.add(entry);
			}
		}
	}

	@Override
	public void build(Base base, int buildingid) {
		super.build(base,buildingid);
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		Factory wf = loadFactoryEntity(base, buildingid);
		
		if( wf != null ) {
			wf.setCount(wf.getCount()+1);
		} 
		else {
			wf = new Factory(base, buildingid);
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
	public void cleanup(Context context, Base base, int buildingid) {
		org.hibernate.Session db = context.getDB();
		
		Factory wf = loadFactoryEntity(base, buildingid);
		if( wf == null ) {
			return;
		}
		
		if( wf.getCount() > 1 ) {	
			BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
	
			Factory.Task[] plist = wf.getProduces();
			for( int i=0; i < plist.length; i++ ) {
				int id = plist[i].getId();
				int count = plist[i].getCount();
				
				FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, id);
				
				usedcapacity = usedcapacity.add(entry.getDauer().multiply(new BigDecimal(count)));
			}
	
			if( usedcapacity.compareTo(new BigDecimal(wf.getCount()-1)) > 0 ) {
				BigDecimal targetCapacity = new BigDecimal(wf.getCount()-1);
				
				for( int i=0; i < plist.length; i++ ) {
					int id = plist[i].getId();
					int count = plist[i].getCount();
					
					FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, id);
					
					BigDecimal capUsed = new BigDecimal(count).multiply(entry.getDauer());
					
					if( usedcapacity.subtract(capUsed).compareTo(targetCapacity) < 0 ) {
						BigDecimal capLeft = capUsed.subtract(usedcapacity.subtract(targetCapacity));
						plist[i] = new Factory.Task(id, capLeft.divide(entry.getDauer(), BigDecimal.ROUND_DOWN).intValue());
						break;
					}
					plist = (Factory.Task[])ArrayUtils.remove(plist, i);
					i--;
					
					usedcapacity = usedcapacity.subtract(capUsed);
						
					if( usedcapacity.compareTo(targetCapacity) <= 0 )
					{
						break;
					}
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
		
		loaddata( base, building );
		AllContextVars vars = ContextMap.getContext().get(AllContextVars.class);
		
		if( vars.allvars.get(building).usedcapacity.get(base.getId()).doubleValue() > 0)
		{
			Factory wf = loadFactoryEntity(base, building);
			Factory.Task[] prodlist = wf.getProduces();
			
			StringBuilder popup = new StringBuilder(200);
			
			popup.append(Common.tableBegin(350, "left").replace('"', '\'') );
			
			Building buildingobj = (Building)db.get(Building.class, building);
			popup.append(buildingobj.getName()+"<br /><br />");
			
			for( int i=0; i < prodlist.length; i++ ) {
				int id = prodlist[i].getId();
				int count = prodlist[i].getCount();
				FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, id);
				
				if( (count > 0) && vars.allvars.get(building).owneritemsbase.contains(entry) ) {
					Cargo production = new Cargo(entry.getProduce());
					production.setOption(Cargo.Option.SHOWMASS, false);
					ResourceList reslist = production.getResourceList();
					popup.append("["+count+"x]  ");
					for(ResourceEntry res : reslist)
					{
						popup.append("<img style='vertical-align:middle' src='"+res.getImage()+"' alt='' />"+res.getCount1());
					}
					popup.append("<br />");
				}
			}
			
			popup.append(Common.tableEnd().replace('"', '\'') );
			
			result.append("<a name=\"p"+base.getId()+"_"+field+"\" id=\"p"+base.getId()+"_"+field+"\" " +
					"class=\"error\" " +
					"onmouseover=\"return overlib('<span style=\\'font-size:13px\\'>"+StringEscapeUtils.escapeJavaScript(popup.toString())+"</span>',REF,'p"+base.getId()+"_"+field+"',REFY,22,NOJUSTY,TIMEOUT,0,DELAY,150,WIDTH,260,BGCLASS,'gfxtooltip',FGCLASS,'gfxtooltip',TEXTFONTCLASS,'gfxtooltip');\" " +
					"onmouseout=\"return nd();\" " +
					"href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"\">[FA]</a>");
		}
		else
		{
			result.append("<a class=\"back\" href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"\">[FA]</a>");
		}
		
		return result.toString();
	}

	@Override
	public boolean isActive(Base base, int status, int field) {
		loaddata( base, base.getBebauung()[field] );
		AllContextVars vars = ContextMap.getContext().get(AllContextVars.class);
		if( vars.allvars.get(base.getBebauung()[field]).usedcapacity.get(base.getId()).doubleValue() > 0 ) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String modifyStats(Base base, Cargo stats, int building) {
		String msg = loaddata( base, building );
		
		Context context = ContextMap.getContext();
		AllContextVars vars = context.get(AllContextVars.class);
		Map<String,Boolean> colcomplete = (Map<String,Boolean>)context.getVariable(getClass(), "colcomplete");
		if( colcomplete == null ) {
			colcomplete = new HashMap<String,Boolean>();
			context.putVariable(getClass(), "colcomplete", colcomplete);
		}
		
		if( (vars.allvars.get(building).usedcapacity.get(base.getId()).compareTo(new BigDecimal(0)) > 0) && !colcomplete.containsKey(base.getId()+":"+building) ) {
			stats.addCargo( vars.allvars.get(building).stats.get(base.getId()) );
			colcomplete.put(base.getId()+":"+building, true);
		}
		
		return msg;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String modifyProductionStats(Base base, Cargo stats, int building) {
		String msg = loaddata( base, building );
		
		Context context = ContextMap.getContext();
		AllContextVars vars = context.get(AllContextVars.class);
		Map<String,Boolean> colcomplete = (Map<String,Boolean>)context.getVariable(getClass(), "colprodcomplete");
		if( colcomplete == null ) {
			colcomplete = new HashMap<String,Boolean>();
			context.putVariable(getClass(), "colprodcomplete", colcomplete);
		}
		
		if( (vars.allvars.get(building).usedcapacity.get(base.getId()).compareTo(new BigDecimal(0)) > 0) && !colcomplete.containsKey(base.getId()+":"+building) ) {
			stats.addCargo( vars.allvars.get(building).productionstats.get(base.getId()) );
			colcomplete.put(base.getId()+":"+building, true);
		}
	
		return msg;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String modifyConsumptionStats(Base base, Cargo stats, int building) {
		String msg = loaddata( base, building );
		
		Context context = ContextMap.getContext();
		AllContextVars vars = context.get(AllContextVars.class);
		Map<String,Boolean> colcomplete = (Map<String,Boolean>)context.getVariable(getClass(), "colconscomplete");
		if( colcomplete == null ) {
			colcomplete = new HashMap<String,Boolean>();
			context.putVariable(getClass(), "colconscomplete", colcomplete);
		}
		
		if( (vars.allvars.get(building).usedcapacity.get(base.getId()).compareTo(new BigDecimal(0)) > 0) && !colcomplete.containsKey(base.getId()+":"+building) ) {
			stats.addCargo( vars.allvars.get(building).consumptionstats.get(base.getId()) );
			colcomplete.put(base.getId()+":"+building, true);
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
		
		Factory wf = loadFactoryEntity(base, building);
		
		if( wf == null ) {
			echo.append("<div style=\"color:red\">FEHLER: Diese Fabrik besitzt keinen Eintrag<br /></div>\n");
			return echo.toString();
		}
		/*
			Liste der baubaren Items zusammenstellen
		*/
		
		Set<FactoryEntry> itemslist = new HashSet<FactoryEntry>();
		Map<FactoryEntry, String> bPlanMap = new HashMap<FactoryEntry, String>();
		
		Iterator<?> itemsIter = db.createQuery("from FactoryEntry").list().iterator();
		for( ; itemsIter.hasNext(); )
		{
			FactoryEntry entry = (FactoryEntry)itemsIter.next();

			if( !user.hasResearched(entry.getRes1()) || !user.hasResearched(entry.getRes2()) || !user.hasResearched(entry.getRes3()) || !entry.hasBuildingId(building))
			{
				continue;
			}
			
			itemslist.add(entry);
		}
		
		Cargo cargo = base.getCargo();
		
		// Lokale Ammobauplaene ermitteln
		List<ItemCargoEntry> itemlist = cargo.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO );
		for( ItemCargoEntry item : itemlist ) {
			Item itemobject = item.getItemObject();
			final FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class,((IEDraftAmmo)itemobject.getEffect()).getAmmo().getId());
			
			if( !itemslist.contains(entry) ) {
				bPlanMap.put(entry, "<span class=\"smallfont\" style=\"color:#EECC44\">[Item]</span> ");
				itemslist.add(entry);
			}
		}
		
		// Moegliche Allybauplaene ermitteln
		if( user.getAlly() != null ) {
			Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, user.getAlly().getItems() );
			
			itemlist = allyitems.getItemsWithEffect( ItemEffect.Type.DRAFT_AMMO );
			for( ItemCargoEntry item : itemlist ) {
				Item itemobject = item.getItemObject();
				final FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class,((IEDraftAmmo)itemobject.getEffect()).getAmmo().getId());
				
				if( !itemslist.contains(entry) ) {
					bPlanMap.put(entry, "<span class=\"smallfont\" style=\"color:#EECC44\">[Item]</span> ");
					itemslist.add(entry);
				}
			}
		}
		
		/*
			Neue Bauauftraege behandeln
		*/
		
		echo.append("<div class=\"smallfont\">");
		if( (produce != 0) && (count != 0) ) {
			final FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, produce);
			
			if( entry == null ) {
				echo.append("<span style=\"color:red\">Fehler: Der angegebene Bauplan existiert nicht</span>\n");
				return echo.toString();
			}
		
			if( itemslist.contains(entry) ) {
				BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
		
				Factory.Task[] plist = wf.getProduces();
				for( int i=0; i < plist.length; i++ ) {
					final int aId = plist[i].getId();
					final int aCount = plist[i].getCount();
					
					final FactoryEntry aEntry = (FactoryEntry)db.get(FactoryEntry.class, aId);
					usedcapacity = usedcapacity.add(aEntry.getDauer().multiply(new BigDecimal(aCount)));
				}
				if( usedcapacity.add(new BigDecimal(count).multiply(entry.getDauer())).doubleValue() > wf.getCount() ) {
					BigDecimal availableCap = usedcapacity.multiply(new BigDecimal(-1)).add(new BigDecimal(wf.getCount()));
					count = availableCap.divide(entry.getDauer(), BigDecimal.ROUND_DOWN).intValue();
				}
			
				if( count != 0 ) {
					boolean gotit = false;
					List<Factory.Task> producelist = new ArrayList<Factory.Task>(
							Arrays.asList(wf.getProduces())
					);
					
					for( int i=0; i < producelist.size(); i++ ) {
						int aId = producelist.get(i).getId();
						int ammoCount = producelist.get(i).getCount();
						
						FactoryEntry aEntry = (FactoryEntry)db.get(FactoryEntry.class, aId);
						
						if( (aEntry == null) || (ammoCount <= 0) ) {
							producelist.remove(i);
							i--;
							continue;
						}
						
						if( aEntry == entry ) {
							if( (count < 0) && (ammoCount+count < 0) ) {
								count = -ammoCount;
							}
							ammoCount += count;
							gotit = true;
						}
						if( ammoCount > 0 ) {
							producelist.set(i, new Factory.Task(aEntry.getId(), ammoCount));
						}
						else {
							producelist.remove(i);
							i--;
						}
					}
					if( !gotit && (count > 0) ) {
						producelist.add(new Factory.Task(entry.getId(), count));
					}
					
					wf.setProduces(producelist.toArray(new Factory.Task[producelist.size()]));
			
					echo.append(Math.abs(count)+" "+entry.getName()+" wurden "+(count>=0 ? "hinzugef&uuml;gt":"abgezogen")+"<br /><br />");
				}
			} 
			else {
				echo.append("Sie haben nicht alle ben&ouml;tigten Forschungen f&uuml;r "+entry.getName()+"<br /><br />");
			}
		}
		
		/*
			Aktuelle Bauauftraege ermitteln
		*/
		// Warum BigDecimal? Weil 0.05 eben nicht 0.05000000074505806 ist (Ungenauigkeit von double/float)....
		BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
		Map<FactoryEntry,Integer> productlist = new HashMap<FactoryEntry,Integer>();
		Cargo consumes = new Cargo();
		Cargo produceCargo = new Cargo();
		consumes.setOption(Cargo.Option.SHOWMASS, false);
		produceCargo.setOption(Cargo.Option.SHOWMASS, false);
		
		if( wf.getProduces().length > 0 ) {
			Factory.Task[] plist = wf.getProduces();
			for( int i=0; i < plist.length; i++ ) {
				final int id = plist[i].getId();
				final int ammoCount = plist[i].getCount();
				
				FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, id);
				
				if( !itemslist.contains(entry) ) {
					echo.append("WARNUNG: Ungueltiges Item >"+entry.getId()+"< (count: "+ammoCount+") in der Produktionsliste entdeckt<br />\n");
					continue;	
				}
				
				usedcapacity = usedcapacity.add(entry.getDauer().multiply(new BigDecimal(ammoCount)));
			
				if( ammoCount > 0 ) {
					Cargo tmpcargo = new Cargo(entry.getBuildCosts());
					Cargo prodcargo = new Cargo(entry.getProduce());
					if( ammoCount > 1 ) {
						tmpcargo.multiply( ammoCount, Cargo.Round.NONE );
						prodcargo.multiply( ammoCount, Cargo.Round.NONE);
					}
					consumes.addCargo( tmpcargo );
					produceCargo.addCargo( prodcargo );
				}
				productlist.put(entry, ammoCount);
			}
		}
		echo.append("</div>\n");
		
		/*
			Ausgabe: Verbrauch, Auftraege, Liste baubarer Munitionstypen
		*/
		echo.append(Common.tableBegin(1060, "left"));
		
		echo.append("<img style=\"vertical-align:middle\" src=\""+config.get("URL")+"data/interface/time.gif\" alt=\"Zeiteinheiten\" />"+usedcapacity+"/"+wf.getCount()+" ausgelastet<br />\n");
		echo.append("Verbrauch: ");
		ResourceList reslist = consumes.getResourceList();
		for( ResourceEntry res : reslist ) {
			echo.append("<img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+"&nbsp;");
		}
		echo.append("<br/>");
		echo.append("Produktion: ");
		reslist = produceCargo.getResourceList();
		for( ResourceEntry res : reslist ) {
			echo.append("<img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+"&nbsp;");
		}
		echo.append("<br /><br />\n");
		echo.append("<table class=\"noBorderX\" cellpadding=\"2\">");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:20px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"font-weight:bold\">Kosten</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"font-weight:bold\">Produktion</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:130px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("</tr>");
		
		List<FactoryEntry> entries = Common.cast(db.createQuery("from FactoryEntry").list());
		
		for( FactoryEntry entry : entries )
		{			
			if( !itemslist.contains(entry) )
			{
				continue;
			}
		
			
			echo.append("<tr>\n");
			if( productlist.containsKey(entry) )
			{
				echo.append("<td class=\"noBorderX\" valign=\"top\">"+productlist.get(entry)+"x</td>\n");
			} 
			else
			{
				echo.append("<td class=\"noBorderX\" valign=\"top\">-</td>\n");
			}
			
			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");
			echo.append("<img style=\"vertical-align:middle\" src=\""+config.get("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+entry.getDauer()+" \n");
			
			Cargo buildcosts = new Cargo(entry.getBuildCosts());
			buildcosts.setOption(Cargo.Option.SHOWMASS, false);
			reslist = buildcosts.getResourceList();
			for( ResourceEntry res : reslist )
			{
				echo.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+"</span>\n");
			}
			
			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");
			
			Cargo produceCosts = new Cargo(entry.getProduce());
			produceCosts.setOption(Cargo.Option.SHOWMASS, false);
			reslist = produceCosts.getResourceList();
			for( ResourceEntry res : reslist )
			{
				echo.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+"</span>\n");
			}
		
			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:130px\">\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"text\" size=\"2\" value=\"0\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\""+entry.getId()+"\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\""+base.getId()+"\" />\n");
			echo.append("<input name=\"field\" type=\"hidden\" value=\""+field+"\" />\n");
			echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
			echo.append("<input type=\"submit\" value=\"herstellen\" />\n");
			echo.append("</div>\n");
			echo.append("</form></td>\n");
			
			/* auf null */
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:30px\">\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"hidden\" size=\"2\" value=\"-"+productlist.get(entry)+"\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\""+entry.getId()+"\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\""+base.getId()+"\" />\n");
			echo.append("<input name=\"field\" type=\"hidden\" value=\""+field+"\" />\n");
			echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
			echo.append("<input type=\"submit\" value=\"reset\" />\n");
			echo.append("</div>\n");
			echo.append("</form></td>\n");
			
			/* plus 1 */
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:30px\">\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"hidden\" size=\"2\" value=\"1\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\""+entry.getId()+"\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\""+base.getId()+"\" />\n");
			echo.append("<input name=\"field\" type=\"hidden\" value=\""+field+"\" />\n");
			echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
			echo.append("<input name=\"plus 1\" type=\"submit\" value=\"+1\" />\n");
			echo.append("</div>\n");
			echo.append("</form></td>\n");

			/* plus 5 */
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:30px\">\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"hidden\" size=\"2\" value=\"5\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\""+entry.getId()+"\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\""+base.getId()+"\" />\n");
			echo.append("<input name=\"field\" type=\"hidden\" value=\""+field+"\" />\n");
			echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
			echo.append("<input name=\"plus 5\" type=\"submit\" value=\"+ 5\" />\n");
			echo.append("</div>\n");
			echo.append("</form></td>\n");

			/* minus 1 */
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:30px\">\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"hidden\" size=\"2\" value=\"-1\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\""+entry.getId()+"\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\""+base.getId()+"\" />\n");
			echo.append("<input name=\"field\" type=\"hidden\" value=\""+field+"\" />\n");
			echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
			echo.append("<input name=\"minus 1\" type=\"submit\" value=\"- 1\" />\n");
			echo.append("</div>\n");
			echo.append("</form></td>\n");

			/* minus 5 */
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:30px\">\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"hidden\" size=\"2\" value=\"-5\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\""+entry.getId()+"\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\""+base.getId()+"\" />\n");
			echo.append("<input name=\"field\" type=\"hidden\" value=\""+field+"\" />\n");
			echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
			echo.append("<input name=\"minus 5\" type=\"submit\" value=\"- 5\" />\n");
			echo.append("</div>\n");
			
			echo.append("</form></td></tr>\n");
		}
		
		echo.append("</table><br />\n");
		echo.append(Common.tableEnd());
		echo.append( "<div><br /></div>\n");
		
		return echo.toString();
	}	
}
