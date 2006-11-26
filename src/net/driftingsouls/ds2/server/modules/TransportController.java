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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Transfer von Waren zwischen Basen und Schiffen
 * @author Christopher Jung
 *
 */
public class TransportController extends DSGenerator {
	static class MultiTarget {
		private String name;
		private String targetlist;
		
		MultiTarget( String name, String targetlist ) {
			this.name = name;
			this.targetlist = targetlist;
		}
		
		/**
		 * Gibt den Namen des MultiTargets zurueck
		 * @return Der Name
		 */
		String getName() {
			return name;
		}
		
		/**
		 * Gibt eine |-separierte Liste mit Zielen zurueck
		 * @return Liste der Ziele
		 */
		String getTargetList() {
			return targetlist;
		}
	}
	
	abstract static class TransportTarget {
		protected SQLResultRow data;
		protected int role;
		protected int id;
		protected int owner;
		protected Cargo cargo;
		protected long maxCargo;
		
		static final int ROLE_SOURCE = 1;
		static final int ROLE_TARGET = 1;
		
		/**
		 * Erstellt ein neues TransportTarget
		 * @param role Die Rolle (Source oder Target)
		 * @param id Die ID
		 * @throws Exception
		 */
		void create(int role, int id) throws Exception {
			this.role = role;
			this.id = id;
		}
		
		/**
		 * Gibt den Radius des Objekts zurueck
		 * @return Der Radius
		 */
		int getSize() {
			return data.getInt("size");
		}
		
		/**
		 * Gibt die ID des Besitzers zurueck
		 * @return Die ID des Besitzers
		 */
		int getOwner() {
			return owner;
		}
		
		/**
		 * Setzt den Besitzer auf den angegebenen Wert
		 * @param owner Der neue Besitzer
		 */
		void setOwner(int owner) {
			this.owner = owner;
		}
		
		/**
		 * Gibt die SQL-Ergebniszeile zurueck
		 * @return Die SQL-Ergebniszeile
		 */
		SQLResultRow getData() {
			return data;
		}
		
		/**
		 * Setzt die SQL-Ergebniszeile auf den angegebenen Wert
		 * @param row die neue SQL-Ergebniszeile
		 */
		void setData(SQLResultRow row) {
			this.data = row;
		}
		
		/**
		 * Gibt den maximalen Cargo zurueck
		 * @return Der maximale Cargo
		 */
		long getMaxCargo() {
			return maxCargo;
		}
		
		/**
		 * Setzt den maximalen Cargo
		 * @param maxcargo der neue maximale Cargo
		 */
		void setMaxCargo(long maxcargo) {
			this.maxCargo = maxcargo;
		}
		
		/**
		 * Gibt den Cargo zurueck
		 * @return Der Cargo
		 */
		Cargo getCargo() {
			return cargo;
		}
		
		/**
		 * Setzt den Cargo auf den angegebenen Wert
		 * @param cargo der neue Cargo
		 */
		void setCargo(Cargo cargo) {
			this.cargo = cargo;
		}
		
		/**
		 * Schreibt die Daten in die Datenbank
		 */
		abstract void write();
		/**
		 * Laedt die Daten aus der Datenbank nach
		 */
		abstract void reload();
		/**
		 * Gibt die MultiTarget-Variante zurueck.
		 * Wenn keine MultiTarget-Variante verfuegbar ist, so wird <code>null</code> zurueckgegeben
		 * @return Die MultiTarget-Variante oder <code>null</code>
		 */
		abstract MultiTarget getMultiTarget();
		
		/**
		 * Gibt den Namen des Target-Typen zurueck
		 * @return Der Name
		 */
		abstract String getTargetName();
	}
	
	static class ShipTransportTarget extends TransportTarget {
		/**
		 * Konstruktor
		 */
		public ShipTransportTarget() {
			// EMPTY
		}
		
		@Override
		void create(int role, int shipid) throws Exception {
			super.create(role, shipid);
			Database db = ContextMap.getContext().getDatabase();
			
			SQLResultRow data = db.first("SELECT owner,name,type,cargo,status,id,x,y,system,battle,status,fleet FROM ships WHERE id>0 AND id=",shipid);
			
			if( data.isEmpty() ) {
				throw new Exception("Das angegebene Schiff (id:"+shipid+") existiert nicht");
			}
			data.put("size", 0);
			
			if( data.getInt("battle") != 0 ) {
				throw new Exception("Das Schiff (id:"+shipid+") ist in einen Kampf verwickelt");
			}

			if( role == ROLE_TARGET ) {
				if( data.getString("status").indexOf("disable_iff") > -1 ) {
					throw new Exception("Zu dem angegebenen Schiff (id:"+shipid+") k&ouml;nnen sie keine Waren transportieren");
				}
			}
			
			SQLResultRow tmptype = Ships.getShipType( data );
			
			if( Ships.hasShipTypeFlag(tmptype, Ships.SF_KEIN_TRANSFER) ) {
				throw new Exception("Sie k&ouml;nnen keine Waren zu oder von diesem Schiff (id:"+shipid+") transferieren");
			}
			
			setOwner(data.getInt("owner"));
			setMaxCargo(tmptype.getLong("cargo"));

			setCargo(new Cargo( Cargo.Type.STRING, data.getString("cargo") ));	
			setData(data);
		}
		
		@Override
		MultiTarget getMultiTarget() {
			int fleet = getData().getInt("fleet");
			if( fleet == 0 ) {
				return null;
			}
			Database db = ContextMap.getContext().getDatabase();
			
			String list = db.first("SELECT GROUP_CONCAT(id SEPARATOR '|') fleetlist FROM ships WHERE fleet='",fleet,"'").getString("fleetlist");
			
			return new MultiTarget("Flotte", list);
		}

		@Override
		String getTargetName() {
			return "Schiff";
		}

		@Override
		void reload() {
			Database db = ContextMap.getContext().getDatabase();
			
			SQLResultRow data = db.first("SELECT owner,name,type,cargo,status,id,x,y,system,battle,status FROM ships WHERE id>0 AND id=",getData().getInt("id"));
			data.put("size", 0);
			
			SQLResultRow tmptype = Ships.getShipType( data );
			
			setData( data );
			setOwner( data.getInt("owner") );
			setMaxCargo( tmptype.getLong("cargo") );
			setCargo( new Cargo( Cargo.Type.STRING, data.getString("cargo") ) );
		}

		@Override
		void write() {
			Database db = ContextMap.getContext().getDatabase();
			
			db.tUpdate(1,"UPDATE ships SET cargo='",getCargo().save(),"' WHERE id='",getData().getInt("id"),"' AND cargo='",getCargo().save(true),"'");
			
			Ships.recalculateShipStatus(getData().getInt("id"));
		}
		
	}
	
	static class BaseTransportTarget extends TransportTarget {
		/**
		 * Konstruktor
		 */
		public BaseTransportTarget() {
			// EMPTY
		}
		
		@Override
		void create(int role, int baseid) throws Exception {
			super.create(role, baseid);
			Database db = ContextMap.getContext().getDatabase();
			
			SQLResultRow data = db.first("SELECT id,x,y,system,size,owner,name,maxcargo,cargo FROM bases WHERE id=",baseid);

			if( data.isEmpty() ) {
				throw new Exception("Die angegebene Basis (id:"+baseid+") existiert nicht");
			}

			setOwner(data.getInt("owner"));
			setMaxCargo(data.getLong("maxcargo"));

			setCargo(new Cargo( Cargo.Type.STRING, data.getString("cargo") ));
			setData(data);
		}

		@Override
		MultiTarget getMultiTarget() {
			return null;
		}

		@Override
		String getTargetName() {
			return "Basis";
		}

		@Override
		void reload() {
			Database db = ContextMap.getContext().getDatabase();
			
			SQLResultRow data = db.first("SELECT id,x,y,system,size,owner,name,maxcargo,cargo FROM bases WHERE id=",getData().getInt("id"));
			setData( data );
			setOwner( data.getInt("owner") );
			setMaxCargo( data.getLong("maxcargo") );
			setCargo( new Cargo( Cargo.Type.STRING, data.getString("cargo") ) );
		}

		@Override
		void write() {
			Database db = ContextMap.getContext().getDatabase();
			
			db.tUpdate(1,"UPDATE bases SET cargo='",getCargo().save(),"' WHERE id='",getData().getInt("id"),"' AND cargo='",getCargo().save(true),"'");
		}
	}
	private String[] way;
	
	private List<TransportTarget> from;
	private List<TransportTarget> to;
	
	private int retryCount = 0;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public TransportController(Context context) {
		super(context);
		
		from = new ArrayList<TransportTarget>();
		to = new ArrayList<TransportTarget>();
		
		setTemplate("transport.html");
		
		parameterString("from");
		parameterString("to");
		parameterString("way");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		String to = getString("to");
		String from = getString("from");
		String rawway = getString("way");
		String sess = getContext().getSession();
		
		Integer[] extarlist = null;
		Integer[] exsrclist = null;

		if( StringUtils.split(to, '|').length <= 1 ) {
			to = new Integer(to).toString();
		}
		else {
			extarlist = Common.explodeToInteger("|",to);
			to = Common.implode("|", Common.explodeToInteger("|",to));
		}
		
		exsrclist = Common.explodeToInteger("|",from);
		from = Common.implode("|", Common.explodeToInteger("|",from));
				
		String[] way = StringUtils.split(rawway, "to");

		for( int i=0; i < exsrclist.length; i++ ) {
			int afrom = exsrclist[i];
			if( way[0].equals(way[1]) && ( ( extarlist == null && (Integer.parseInt(to) == afrom) ) || 
				 ( extarlist != null && Common.inArray( afrom, extarlist ) ) ) ) {
				 	
				addError("Sie k&ouml;nnen keine Waren zu sich selbst transportieren",(way[0]=="b"?"./main.php?module=base&":"./main.php?module=schiff&")+"sess="+sess+"&"+(way[0]=="b"?"col":"ship")+"="+afrom);
				return false;
			}
		}
		
		Map<String,Class<? extends TransportTarget>> wayhandler = new HashMap<String,Class<? extends TransportTarget>>();
		wayhandler.put("s", ShipTransportTarget.class);
		wayhandler.put("b", BaseTransportTarget.class);
		
		/*
			"From" bearbeiten
		*/
	
		if( wayhandler.containsKey(way[0]) ) {
			try {
				int[] fromlist = Common.explodeToInt("|", from);
				for( int i=0; i < fromlist.length; i++ ) {
					TransportTarget handler = wayhandler.get(way[0]).getConstructor().newInstance();
					handler.create( TransportTarget.ROLE_SOURCE, fromlist[i] );
					this.from.add(handler);
				}
			}
			catch( Exception e ) {
				e.printStackTrace();
				addError(e.toString());
				return false;
			}
		}
		else {
			addError("Ung&uuml;ltige Transportquelle", "./main.php?sess="+sess+"&module=ueber" );

			return false;
		}

		/*
			"To" bearbeiten
		*/
		if( wayhandler.containsKey(way[1]) ) {
			int[] tolist = Common.explodeToInt( "|", to );
			
			for( int i=0; i < tolist.length; i++ ) {
				try {
					TransportTarget handler = wayhandler.get(way[1]).getConstructor().newInstance();
					handler.create( TransportTarget.ROLE_TARGET, tolist[i] );
					this.to.add(handler);
				}
				catch( Exception e ) {
					e.printStackTrace();
					addError(e.toString());
					return false;
				}
			}
		}
		else {
			addError( "Ung&uuml;ltiges Transportziel", "./main.php?sess="+sess+"&module=ueber" );
			
			return false;
		}
		
		
		/*
			Sind die beiden Objekte auch im selben Sektor?
		*/
		if( !Location.fromResult(this.from.get(0).getData()).sameSector( this.from.get(0).getSize(), Location.fromResult(this.to.get(0).getData()), this.to.get(0).getSize()) ) {
			addError("Die angegebenen Objekte befinden sich nicht im selben Sektor" );
			
			return false;
		}

		for( TransportTarget afrom : this.from ) {
			if( afrom.getOwner() != getUser().getID() ) {
				addError("Das Schiff geh&ouml;rt ihnen nicht", Common.buildUrl(getContext(), "default", "module", "ueber") );
				
				return false;
			}
		}
		
		this.way = way;

		return true;
	}
	
	private long transferSingleResource(TransportTarget fromItem, TransportTarget toItem, ResourceEntry res, long count, Cargo newfromc, Cargo newtoc, MutableLong cargofrom, MutableLong cargoto, StringBuilder msg, char mode) {
		TemplateEngine t = getTemplateEngine();
		
		if( count > newfromc.getResourceCount( res.getId() ) ) {
			t.set_var(	"transfer.notenoughcargo",	1,
						"transfer.from.cargo",		Common.ln(newfromc.getResourceCount(res.getId()) ) );
						
			count = newfromc.getResourceCount( res.getId() );
			if( count < 0 ) {
				count = 0;	
			}
		}
					
		if( cargoto.longValue() - Cargo.getResourceMass( res.getId(), count ) < 0 ) {
			count = cargoto.longValue() / Cargo.getResourceMass( res.getId(), 1 );
			
			if( count < 0 ) {
				Common.writeLog("transport.error.log", Common.date("d.m.y H:i:s")+": "+getUser().getID()+" -> "+toItem.getOwner()+" | "+getString("from")+" -> "+getString("to")+" ["+getString("way")+"] : "+mode+res.getId()+"@"+count+" ; "+msg+"\n---------\n");
				count = 0;
			}
						
			t.set_var(	"transfer.notenoughspace",	1,
						"transfer.count.new",		Common.ln(count) );
		}
		
		newtoc.addResource( res.getId(), count );
		newfromc.substractResource( res.getId(), count );
			
		msg.append("[resource="+res.getId()+"]"+count+"[/resource] umgeladen\n");
		
		if( mode == 't' ) {
			cargofrom.setValue(fromItem.getMaxCargo() - newfromc.getMass());
			cargoto.setValue(toItem.getMaxCargo() - newtoc.getMass());
		}
		else {
			cargofrom.setValue(toItem.getMaxCargo() - newfromc.getMass());
			
			cargoto.setValue(fromItem.getMaxCargo() - newtoc.getMass());
		}
			
		if( (fromItem.getOwner() == toItem.getOwner()) || (toItem.getOwner() == 0) ) {
			t.set_var(	"transfer.reportnew",		1,
						"transfer.count.complete",	Common.ln(newtoc.getResourceCount(res.getId()) ) );
		}	
		
		return count;
	}
	
	public void transferAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		t.set_block( "_TRANSPORT", "transfer.listitem", "transfer.list" );

		boolean transfer = false;
		List<TransportTarget> tolist = this.to;

		if( this.to.size() == 1 ) {
			t.set_var( "transfer.multitarget", 0 );		
		}			
			
		List<Cargo> newtoclist = new ArrayList<Cargo>();
		List<Long> cargotolist = new ArrayList<Long>();
		Cargo totaltocargo = new Cargo();
		
		List<Cargo> newfromclist = new ArrayList<Cargo>();
		List<Long> cargofromlist = new ArrayList<Long>();
		Cargo totalfromcargo = new Cargo();
		
		// TODO: rewrite
		for( int k=0; k < tolist.size(); k++ ) {
			newtoclist.add(k, (Cargo)tolist.get(k).getCargo().clone());
			totaltocargo.addCargo( tolist.get(k).getCargo() );
			cargotolist.add(k, tolist.get(k).getMaxCargo() - tolist.get(k).getCargo().getMass());
		}
		
		for( int k=0; k < from.size(); k++ ) {
			newfromclist.add(k, (Cargo)from.get(k).getCargo().clone());
			totalfromcargo.addCargo( from.get(k).getCargo() );
			cargofromlist.add(k, from.get(k).getMaxCargo() - from.get(k).getCargo().getMass());
		}

		Map<Integer,StringBuilder> msg = new HashMap<Integer,StringBuilder>();
		
		if( (tolist.size() > 1) || (from.size() > 1) ) {
			t.set_block("_TRANSPORT", "transfer.multitarget.listitem", "transfer.multitarget.list" );
		}

		ResourceList reslist = totalfromcargo.compare( totaltocargo, true );
		for( ResourceEntry res : reslist ) {
			parameterNumber(res.getId()+"to");
			int transt = getInteger(res.getId()+"to");
			
			parameterNumber(res.getId()+"from");
			int transf = getInteger(res.getId()+"from");
	
			if( transt > 0 ) {
				t.set_var(	"transfer.count",		Common.ln(transt),
							"transfer.mode.to",		1,
							"transfer.res.image",	res.getImage() );
				
				for( int k=0; k < from.size(); k++ ) {
					TransportTarget from = this.from.get(k);
					t.set_var("transfer.source.name", Common._plaintitle(from.getData().getString("name")));
					
					for( int j=0; j < tolist.size(); j++ ) {
						TransportTarget to = tolist.get(j);
						if( (tolist.size() > 1) || (this.from.size() > 1) ) {
							t.start_record();
						}
						
						t.set_var("transfer.target.name", Common._plaintitle(to.getData().getString("name")) );
				
						if( !msg.containsKey(to.getOwner()) ) {
							msg.put(to.getOwner(), new StringBuilder());
						}
						
						MutableLong mCargoFrom = new MutableLong(cargofromlist.get(k));
						MutableLong mCargoTo = new MutableLong(cargotolist.get(j));
						if( transferSingleResource( from, to, res, transt, newfromclist.get(k), newtoclist.get(j), mCargoFrom, mCargoTo, msg.get(to.getOwner()), 't') != 0 ) {
							transfer = true;
							
							// Evt unbekannte Items bekannt machen
							if( res.getId().isItem() && (getUser().getID() != to.getOwner()) ) {
								if( Items.get().item(res.getId().getItemID()).isUnknownItem() ) {
									User auser = getContext().createUserObject( to.getOwner() );
									auser.addKnownItem(res.getId().getItemID());
								}
							}
						}
						cargofromlist.set(k, mCargoFrom.longValue());
						cargotolist.set(j, mCargoTo.longValue());
					
						if( (tolist.size() > 1) || (this.from.size() > 1) ) {
							t.parse("transfer.multitarget.list", "transfer.multitarget.listitem", true);
						
							t.stop_record();
							t.clear_record();
						}
					}
				}
				t.parse("transfer.list", "transfer.listitem", true);
			}
			else if( transf > 0 ) {		
				t.set_var(	"transfer.count",		Common.ln(transf),
							"transfer.res.image",	res.getImage(),
							"transfer.mode.to",		0 );
				for( int k=0; k < from.size(); k++ ) {
					TransportTarget from = this.from.get(k);				
					t.set_var("transfer.source.name", Common._plaintitle(from.getData().getString("name")));
					
					for( int j=0; j < tolist.size(); j++ ) {
						TransportTarget to = tolist.get(j);
						if( (tolist.size() > 1) || (this.from.size() > 1) ) {
							t.start_record();
						}
						
						if( (to.getOwner() != getUser().getID()) && (to.getOwner() != 0) ) {
							addError("Das geh&ouml;rt dir nicht!");
							
							redirect();
							return;
						} 
						
						t.set_var("transfer.target.name", Common._plaintitle(to.getData().getString("name")) );
				
						if( !msg.containsKey(to.getOwner()) ) {
							msg.put(to.getOwner(), new StringBuilder());
						}
						MutableLong mCargoFrom = new MutableLong(cargofromlist.get(k));
						MutableLong mCargoTo = new MutableLong(cargotolist.get(j));
						if( transferSingleResource( from, to, res, transf, newtoclist.get(j), newfromclist.get(k), mCargoTo, mCargoFrom, msg.get(to.getOwner()), 'f') != 0 ) {
							transfer = true;
						}
						cargofromlist.set(k, mCargoFrom.longValue());
						cargotolist.set(j, mCargoTo.longValue());
						
						if( (tolist.size() > 1) || (this.from.size() > 1) ) {
							t.parse("transfer.multitarget.list", "transfer.multitarget.listitem", true);
						
							t.stop_record();
							t.clear_record();
						}
					}
				}
				t.parse("transfer.list", "transfer.listitem", true);
			}
		
			Map<Integer, String> ownerpmlist = new HashMap<Integer,String>();
			
			List<String> sourceshiplist = new ArrayList<String>();;
			for( int i=0; i < this.from.size(); i++ ) {
				sourceshiplist.add(from.get(i).getData().getString("name")+" ("+from.get(i).getData().getInt("id")+")");	
			}
			
			for( int j=0; j < tolist.size(); j++  ) {
				TransportTarget to = tolist.get(j);
				if( getUser().getID() != to.getOwner() ) {
					if( msg.containsKey(to.getOwner()) && (msg.get(to.getOwner()).length() > 0) && !ownerpmlist.containsKey(to.getOwner()) ) {
						Common.writeLog("transport.log", Common.date("d.m.y H:i:s")+": "+getUser().getID()+" -> "+to.getOwner()+" | "+getString("from")+" -> "+getString("to")+" ["+getString("way")+"] : "+"\n"+msg+"---------\n");
					
						t.set_var( "transfer.pm", 1 );

						List<String> shiplist = new ArrayList<String>();
						
						// TODO: check if this works (foreach+for)
						for( int k=j; k < tolist.size(); k++ ) {
							if( this.to.get(j).getOwner() == tolist.get(k).getOwner() ) {
								shiplist.add(tolist.get(k).getData().getString("name")+" ("+tolist.get(k).getData().getInt("id")+")");	
							}
						}
						
						String tmpmsg = Common.implode(",",sourceshiplist)+" l&auml;dt Waren auf "+Common.implode(",",shiplist)+"\n"+msg.get(to.getOwner());
						PM.send(getContext(), getUser().getID(), to.getOwner(), "Waren transferiert", tmpmsg);
						
						ownerpmlist.put(to.getOwner(), msg.get(to.getOwner()).toString());
					}
				}
			}
		}
		
		if( !transfer ) {
			redirect();
			
			return;
		}
		
		db.tBegin();
		
		/*
			"from" bearbeiten
		*/
		for( int k=0; k < newfromclist.size(); k++ ) {
			Cargo newfromc = newfromclist.get(k);
			if( newfromc.save().equals(from.get(k).getCargo().save(true)) ) {
				continue;	
			}
			from.get(k).setCargo(newfromc);
			from.get(k).write();
		}
	
		/*
			"to" bearbeiten 
		*/
		for( int k=0; k < newtoclist.size(); k++ ) {
			Cargo newtoc = newtoclist.get(k);
			if( newtoc.save().equals(to.get(k).getCargo().save(true)) ) {
				continue;	
			}
			to.get(k).setCargo(newtoc);
			to.get(k).write();
		}
		
		if( !db.tCommit() ) {
			t.set_var(	"transfer.list",				"",
						"transfer.multitarget.list",	"" );
								
			if( retryCount < 3 ) {									
				retryCount++;
				
				for( TransportTarget from : this.from ) {
					from.reload();
				}
				for( TransportTarget to : this.to ) {
					to.reload();
				}
									
				redirect("transfer");
				
				return;
			}
			
			addError("Transfer der Waren nicht konnte nicht erfolgreich durchgeführt werden. Bitte versuchen sie es erneut");	
		}
		
		redirect();	
	}

	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		t.set_block("_TRANSPORT", "target.targets.listitem", "target.targets.list" );
		t.set_block("_TRANSPORT", "source.sources.listitem", "source.sources.list" );
		
		t.set_var(	"global.rawway",	getString("way"),
					"source.isbase",	way[0].equals("b"),
					"target.isbase",	way[1].equals("b") );

		// Die Quelle(n) ausgeben
		if( from.size() == 1 ) {
			TransportTarget first = from.get(0);
			
			t.set_var(	"sourceobj.name",	first.getData().getString("name"),
						"sourceobj.id",		first.getData().getInt("id"),
						"source.cargo",		Common.ln(first.getMaxCargo() - first.getCargo().getMass()) );
			
			t.set_var("source.id", first.getData().getInt("id"));
		}
		else if( from.size() < 10 ){			
			long cargo = 0;
			for( TransportTarget afromd : from ) {
				cargo = Math.max(afromd.getMaxCargo() - afromd.getCargo().getMass(), cargo);
				t.set_var(	"sourceobj.name",	afromd.getData().getString("name"),
							"sourceobj.id",		afromd.getData().getInt("id") );
				
				t.parse( "source.sources.list", "source.sources.listitem", true );
			}
			
			t.set_var(	"source.id",	getString("from"),
						"sourceobj.id",	from.get(0).getData().getInt("id"),
						"source.cargo",	"max "+Common.ln(cargo) );
		}
		else {
			long cargo = 0;
			for( TransportTarget afromd : from ) {
				cargo = Math.max(afromd.getMaxCargo() - afromd.getCargo().getMass(), cargo);
			}
			TransportTarget first = from.get(0);
			
			t.set_var(	"sourceobj.name",		first.getData().getString("name"),
						"sourceobj.id",			first.getData().getInt("id"),
						"sourceobj.addinfo",	"und "+(from.size()-1)+" weiteren Schiffen",
						"source.cargo",			"max "+Common.ln(cargo) );
								
			t.set_var("source.id", getString("from"));
		}
		
		// Das Ziel / die Ziele ausgeben
		if( to.size() == 1 ) {
			t.set_var(	"targetobj.name",	to.get(0).getData().getString("name"),
						"targetobj.id",		to.get(0).getData().getInt("id"),
						"target.cargo",		Common.ln(to.get(0).getMaxCargo() - to.get(0).getCargo().getMass()) );
			
			t.set_var("target.id", to.get(0).getData().getInt("id"));
		} 
		else if( to.size() < 10 ){		
			long cargo = 0;
			for( TransportTarget atod : to ) {
				cargo = Math.max(atod.getMaxCargo() - atod.getCargo().getMass(), cargo);
				t.set_var(	"targetobj.name",	atod.getData().getString("name"),
							"targetobj.id",		atod.getData().getInt("id") );
				
				t.parse( "target.targets.list", "target.targets.listitem", true );
			}
			
			t.set_var(	"target.id",	getString("to"),
						"targetobj.id",	to.get(0).getData().getInt("id"),
						"target.cargo",	"max "+Common.ln(cargo) );
		}
		else {
			long cargo = 0;
			for( TransportTarget atod : to ) {
				cargo = Math.max(atod.getMaxCargo() - atod.getCargo().getMass(), cargo);
			}
			TransportTarget first = to.get(0);
			
			t.set_var(	"targetobj.name",	first.getData().getString("name"),
						"targetobj.id",		first.getData().getInt("id"),
						"targetobj.addinfo",	"und "+(to.size()-1)+" weiteren Schiffen",
						"target.cargo",			"max "+Common.ln(cargo) );
								
			t.set_var("target.id", getString("to"));
		}
		
		// Transfermodi ausgeben
		t.set_block( "_TRANSPORT","transfermode.listitem", "transfermode.list" );
		if( (to.size() > 1) || (from.size() > 1) || (to.get(0).getMultiTarget() != null) ||
			(from.get(0).getMultiTarget() != null) ) {
			TransportTarget first = to.get(0);
			TransportTarget second = from.get(0);
			
			MultiTarget multiTo = null;
			if( to.size() > 1 ) {
				multiTo = first.getMultiTarget();
				if( (multiTo == null) || !multiTo.getTargetList().equals(getString("to")) ) {
					multiTo = new MultiTarget("Gruppe", getString("to"));
				}
			}
			else {
				multiTo = first.getMultiTarget();
			}
			
			MultiTarget multiFrom = null;
			if( from.size() > 1 ) {
				multiFrom = second.getMultiTarget();
				if( (multiFrom == null) || !multiFrom.getTargetList().equals(getString("from")) ) {
					multiFrom = new MultiTarget("Gruppe", getString("from"));
				}
			}
			else {
				multiFrom = second.getMultiTarget();
			}
			
			// Single to Single
			t.set_var(	"transfermode.from.name",	second.getTargetName(),
						"transfermode.from",		second.getData().getInt("id"),
						"transfermode.to.name",		first.getTargetName(),
						"transfermode.to",			first.getData().getInt("id"),
						"transfermode.selected",	to.size() == 1 && (from.size() <= 1) );
			t.parse("transfermode.list", "transfermode.listitem", true);
								
			
			// Single to Multi
			if( multiTo != null ) {
				t.set_var(	"transfermode.from.name",	second.getTargetName(),
							"transfermode.from",		second.getData().getInt("id"),
							"transfermode.to.name",		multiTo.getName(),
							"transfermode.to",			multiTo.getTargetList(),
							"transfermode.selected",	to.size() > 1 && (from.size() <= 1) );
				t.parse("transfermode.list", "transfermode.listitem", true);
			}
			
			// Multi to Single
			if( multiFrom != null ) {
				t.set_var(	"transfermode.to.name",		first.getTargetName(),
							"transfermode.to",			first.getData().getInt("id"),
							"transfermode.from.name",	multiFrom.getName(),
							"transfermode.from",		multiFrom.getTargetList(),
							"transfermode.selected",	(from.size() > 1) && to.size() == 1 );
				t.parse("transfermode.list", "transfermode.listitem", true);
			}
			
			// Multi to Multi
			if( (multiFrom != null) && (multiTo != null)  ) {
				t.set_var(	"transfermode.to.name",		multiTo.getName(),
							"transfermode.to",			multiTo.getTargetList(),
							"transfermode.from.name",	multiFrom.getName(),
							"transfermode.from",		multiFrom.getTargetList(),
							"transfermode.selected",	(from.size() > 1) && to.size() > 1 );
				t.parse("transfermode.list", "transfermode.listitem", true);
			}
		}
		
		t.set_block( "_TRANSPORT","res.listitem", "res.list" );

		// Soll der Zielcargo gezeigt werden?
		boolean showtarget = false;
		Cargo tocargo = new Cargo();
		
		for( TransportTarget to : this.to ) {
			if( (getUser().getID() != to.getOwner()) && (to.getOwner() != 0) ) {
				continue;
			}
			ResourceList reslist = to.getCargo().getResourceList();
			for( ResourceEntry res : reslist ) {
				if( res.getCount1() > tocargo.getResourceCount(res.getId()) ) {
					tocargo.setResource(res.getId(), res.getCount1());
				}
			}
		}
			
		if( !tocargo.isEmpty() ) {
			showtarget = true;
		}
		
		t.set_var("target.show", showtarget);
		
		Cargo fromcargo = new Cargo();
		for( TransportTarget afrom : from ) {
			ResourceList reslist = afrom.getCargo().getResourceList();
			for( ResourceEntry res : reslist ) {
				if( res.getCount1() > fromcargo.getResourceCount(res.getId()) ) {
					fromcargo.setResource(res.getId(), res.getCount1());
				}
			}
		}

		// Muss verglichen werden oder reicht unsere eigene Resliste?
		ResourceList reslist = null;
		if( !showtarget ) {
			reslist = fromcargo.getResourceList();
		}
		else {
			reslist = fromcargo.compare( tocargo, true );
		}
		
		for( ResourceEntry res : reslist ) {
			t.set_var(	"res.name",		res.getName(),
						"res.image",	res.getImage(),
						"res.id",		res.getId(),
						"res.cargo.source",	(from.size() > 1 ? "max " : "")+res.getCargo1(),
						"res.cargo.target",	showtarget ? (to.size() > 1 ? "max " : "" )+res.getCargo2() : 0 );
								
			t.parse( "res.list", "res.listitem", true );
		}
	}
}
