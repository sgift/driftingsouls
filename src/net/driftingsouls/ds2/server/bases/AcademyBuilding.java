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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Die Akademie.
 * @author Christopher Jung
 *
 */
@Entity(name="AcademyBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.AcademyBuilding")
@Configurable
public class AcademyBuilding extends DefaultBuilding {
	private static final Log log = LogFactory.getLog(AcademyBuilding.class);
	private static final Map<Integer,String> offis = new HashMap<Integer,String>();
	private static final Map<Integer,String> attributes = new HashMap<Integer,String>();
	
	static {
		offis.put(1, "Ingenieur");
		offis.put(2, "Navigator");
		offis.put(3, "Sicherheitsexperte");
		offis.put(4, "Captain");
		
		attributes.put(1, "Technik");
		attributes.put(2, "Waffen");
		attributes.put(3, "Navigation");
		attributes.put(4, "Sicherheit");
		attributes.put(5, "Kommandoeffizienz");
	}
	
	/**
	 * Erstellt eine neue Academy-Instanz.
	 */
	public AcademyBuilding() {
		// EMPTY
	}
	
	@Override
	public void build(Base base) {
		super.build(base);
		
		Academy academy = new Academy(base);
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		db.persist(academy);
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
		super.cleanup(context, base);
		
		org.hibernate.Session db = context.getDB();
		db.createQuery("delete from Academy where base=?")
			.setEntity(0, base)
			.executeUpdate();
		
		db.createQuery("update Offizier set dest=? where dest=?")
			.setString(0, "b "+base.getId())
			.setString(1, "t "+base.getId())
			.executeUpdate();	
	}

	@Override
	public boolean isActive(Base base, int status, int field) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Academy academy = (Academy)db.get(Academy.class, base.getId());
		if( (academy != null) && (academy.getRemain() > 0) ) {
			return true;
		}
		else if( academy == null ) {
			log.warn("Die Akademie auf Basis "+base.getId()+" verfuegt ueber keinen DB-Eintrag");
		}
		return false;
	}
	
	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();

		StringBuilder result = new StringBuilder(200);
		
		Academy acc = (Academy)db.get(Academy.class, base.getId());
		if( acc != null ) {
			if( acc.getRemain() == 0 ) {
				result.append("<a class=\"back\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[A]</a>");
			} 
			else {	
				StringBuilder popup = new StringBuilder(200);
				popup.append(Common.tableBegin(300, "left").replace('"', '\''));
				if( acc.getTrain() != 0 ) {
					popup.append("Bildet aus: ");
					popup.append(offis.get(acc.getTrain()));
					popup.append("<br />");
				}
				else if( acc.getUpgrade().length() != 0 ) {											 
					String[] upgrade = StringUtils.split(acc.getUpgrade(), ' ');
					Offizier offi = Offizier.getOffizierByID(Integer.parseInt(upgrade[0]));
					
					popup.append("Bildet aus: ");
					popup.append(offi.getName());
					popup.append(" (");
					popup.append(attributes.get(Integer.parseInt(upgrade[1])));
					popup.append(")<br />");
				}
				popup.append("Dauer: <img style='vertical-align:middle' src='");
				popup.append(this.config.get("URL"));
				popup.append("data/interface/time.gif' alt='noch ' />");
				popup.append(acc.getRemain());
				popup.append("<br />");
				popup.append(Common.tableEnd().replace('"', '\''));
				
				String popupStr = StringEscapeUtils.escapeJavaScript(popup.toString());
				
				result.append("<a name=\"p");
				result.append(base.getId());
				result.append("_");
				result.append(field);
				result.append("\" id=\"p");
				result.append(base.getId());
				result.append("_");
				result.append(field);
				result.append("\" class=\"error\" onmouseover=\"return overlib('<span style=\\'font-size:13px\\'>");
				result.append(popupStr);
				result.append("</span>',REF,'p");
				result.append(base.getId());
				result.append("_");
				result.append(field);
				result.append("',REFY,22,NOJUSTY,TIMEOUT,0,DELAY,150,WIDTH,300,BGCLASS,'gfxtooltip',FGCLASS,'gfxtooltip',TEXTFONTCLASS,'gfxtooltip');\" onmouseout=\"return nd();\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[A]<span style=\"font-weight:normal\">");
				result.append(acc.getRemain());
				result.append("</span></a>");
			}
		}
		else {
			result.append("WARNUNG: Akademie ohne Akademieeintrag gefunden<br />\n");
		}
	
		return result.toString();
	}

	@Override
	public String output(Context context, TemplateEngine t, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();
		User user = (User)context.getActiveUser();
		
		int newo = context.getRequest().getParameterInt("newo");
		int train = context.getRequest().getParameterInt("train");
		int off = context.getRequest().getParameterInt("off");
		String conf = context.getRequest().getParameterString("conf");
		
		if( !t.setFile( "_BUILDING", "buildings.academy.html" ) ) {
			context.addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}

		Academy academy = (Academy)db.get(Academy.class, base.getId());
		if( academy == null ) {
			context.addError("Diese Akademie verf&uuml;gt &uuml;ber keinen Akademie-Eintrag in der Datenbank");
			return "";
		}
		
		t.setVar(	
				"base.name",	base.getName(),
				"base.id",		base.getId(),
				"base.field",	field);
		
		//---------------------------------
		// Einen neue Offiziere ausbilden
		//---------------------------------
		
		if( newo != 0 ) {
			if( (academy.getTrain() == 0) && (academy.getUpgrade().length() == 0)) {
				t.setVar("academy.show.trainnewoffi", 1);
				
				Cargo cargo = new Cargo(base.getCargo());
			
				boolean ok = true;
				if( cargo.getResourceCount( Resources.SILIZIUM ) < 25 ) {
					t.setVar("trainnewoffi.error", "Nicht genug Silizium");
					ok = false;
				}
				Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo() );
				if( cargo.getResourceCount( Resources.NAHRUNG )+usercargo.getResourceCount( Resources.NAHRUNG ) < 35 ) {
					t.setVar("trainnewoffi.error", "Nicht genug Nahrung");
					ok = false;
				}
		
				if( ok ) {
					t.setVar("trainnewoffi.train", 1);
		
					cargo.substractResource( Resources.SILIZIUM, 25 );
					usercargo.substractResource( Resources.NAHRUNG, 35 );
					if( usercargo.getResourceCount( Resources.NAHRUNG ) < 0 ) {
						cargo.substractResource( Resources.NAHRUNG, -usercargo.getResourceCount( Resources.NAHRUNG ) );
						usercargo.setResource( Resources.NAHRUNG, 0 );	
					}
		
					user.setCargo(usercargo.save());
					academy.setTrain(newo);
					academy.setRemain(8);
					base.setCargo(cargo);
				} 
			}
		}
	
		//--------------------------------------
		// "Upgrade" eines Offiziers durchfuehren
		//--------------------------------------
		
		if( (train != 0) && (off != 0) ) {
			if( (academy.getTrain() == 0) && (academy.getUpgrade().length() == 0) ) {				
				Offizier offizier = Offizier.getOffizierByID(off);
				if( offizier.getDest()[0].equals("b") && offizier.getDest()[1].equals(Integer.toString(base.getId())) ) {					
					Map<Integer,Offizier.Ability> dTrain = new HashMap<Integer,Offizier.Ability>();
					dTrain.put(1, Offizier.Ability.ING);
					dTrain.put(2, Offizier.Ability.WAF);
					dTrain.put(3, Offizier.Ability.NAV);
					dTrain.put(4, Offizier.Ability.SEC);
					dTrain.put(5, Offizier.Ability.COM);
									 
					int sk = offizier.getAbility(dTrain.get(train))+1;
					int nk = (int)(offizier.getAbility(dTrain.get(train))*1.5d)+1;
					int dauer = (int)(offizier.getAbility(dTrain.get(train))/4d)+1;
					
					t.setVar(
							"academy.show.trainoffi", 1,
							"trainoffi.id",			offizier.getID(),
							"trainoffi.trainid",	train,
							"offizier.name",		Common._plaintext(offizier.getName()),
							"offizier.train.dauer",		dauer,
							"offizier.train.nahrung", 	nk,
							"offizier.train.silizium",	sk,
							"resource.nahrung.image",	Cargo.getResourceImage(Resources.NAHRUNG),
							"resource.silizium.image",	Cargo.getResourceImage(Resources.SILIZIUM));
					
					if( train == 1 ) {
						t.setVar("offizier.train.ability", "Technik");
					} 
					else if( train == 2 ) {
						t.setVar("offizier.train.ability", "Waffen");
					} 
					else if( train == 3 ) {
						t.setVar("offizier.train.ability", "Navigation");
					}
					else if( train == 4 ) {
						t.setVar("offizier.train.ability", "Sicherheit");
					} 
					else if( train == 5 ) {
						t.setVar("offizier.train.ability", "Kommandoeffizienz");
					}
					
					Cargo cargo = new Cargo(base.getCargo());

					boolean ok = true;
					if( cargo.getResourceCount( Resources.SILIZIUM ) < sk) {
						t.setVar("trainoffi.error", "Nicht genug Silizium"); 
						ok = false;
					}
					Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo() );
					if( cargo.getResourceCount( Resources.NAHRUNG )+usercargo.getResourceCount( Resources.NAHRUNG ) < nk ) {
						t.setVar("trainoffi.error", "Nicht genug Nahrung"); 
						ok = false;
					}
		
					if( !conf.equals("ok") ) {
						t.setVar("trainoffi.conf",	1);
						t.parse( "OUT", "_BUILDING" );	
						return t.getVar("OUT");
					}
		
					if( ok ) {
						t.setVar("trainoffi.train", 1);
		
						cargo.substractResource( Resources.SILIZIUM, sk );
						usercargo.substractResource( Resources.NAHRUNG, nk );
						if( usercargo.getResourceCount( Resources.NAHRUNG ) < 0 ) {
							cargo.substractResource( Resources.NAHRUNG, -usercargo.getResourceCount( Resources.NAHRUNG ) );
							usercargo.setResource( Resources.NAHRUNG, 0 );	
						}
		
						user.setCargo( usercargo.save() );
						academy.setUpgrade(offizier.getID()+" "+train);
						academy.setRemain(dauer);
						
						offizier.setDest("t", base.getId());
						base.setCargo(cargo);
						
						t.parse( "OUT", "_BUILDING" );	
						return t.getVar("OUT");
					}
				}
			}
		}
		boolean allowActions = true;
		
		//-----------------------------------------------
		// werden gerade Offiziere ausgebildet? Welche?
		//-----------------------------------------------
		
		if( (academy.getTrain() != 0) || (academy.getUpgrade().length() != 0) ) {
			t.setVar(	
					"academy.show.training", 1,
					"training.remain",	academy.getRemain());
			
					
			if( academy.getTrain() != 0 ) {
				t.setVar("trainoffizier.name", Common._plaintitle(Offiziere.LIST.get(academy.getTrain()).getString("name")));
			}
			else {
				String[] upgradeData = StringUtils.split(academy.getUpgrade(), ' ' );
				Offizier offizier = Offizier.getOffizierByID(Integer.parseInt(upgradeData[0]));
				
				t.setVar(
						"trainoffizier.picture",	offizier.getPicture(),
						"trainoffizier.id",		offizier.getID(),
						"trainoffizier.name",	Common._plaintitle(offizier.getName()));
			}

			allowActions = false;
		}
		
		//---------------------------------
		// Liste: Neue Offiziere ausbilden
		//---------------------------------
		if( allowActions ) {
			t.setVar(
					"academy.show.trainnew",	1,
					"resource.silizium.image",	Cargo.getResourceImage(Resources.SILIZIUM),
					"resource.nahrung.image",	Cargo.getResourceImage(Resources.NAHRUNG));
			
			t.setBlock("_BUILDING", "academy.trainnew.listitem", "academy.trainnew.list");
			
			for( SQLResultRow offi : Offiziere.LIST.values() ) {
				t.setVar( 
						"offizier.id",		offi.getInt("id"),
						"offizier.name",	Common._title(offi.getString("name")),
						"offizier.ing",		offi.getInt("ing"),
						"offizier.waf",		offi.getInt("waf"),
						"offizier.nav",		offi.getInt("nav"),
						"offizier.sec",		offi.getInt("sec"),
						"offizier.com",		offi.getInt("com"));
				
				t.parse("academy.trainnew.list", "academy.trainnew.listitem", true);
			}
		}
		
		//---------------------------------
		// Liste: "Upgrade" von Offizieren
		//---------------------------------
		
		t.setVar(
				"academy.show.offilist", 1,
				"offilist.allowactions", allowActions);
		
		t.setBlock("_BUILDING", "academy.offilist.listitem", "academy.offilist.list");
		
		List<Offizier> offiziere = Offizier.getOffiziereByDest('b', base.getId());
		for( Offizier offi : offiziere ) {
			
			t.setVar(
					"offizier.picture",	offi.getPicture(),
					"offizier.id",		offi.getID(),
					"offizier.name",	Common._plaintitle(offi.getName()),
					"offizier.ing",		offi.getAbility(Offizier.Ability.ING),
					"offizier.waf",		offi.getAbility(Offizier.Ability.WAF),
					"offizier.nav",		offi.getAbility(Offizier.Ability.NAV),
					"offizier.sec",		offi.getAbility(Offizier.Ability.SEC),
					"offizier.com",		offi.getAbility(Offizier.Ability.COM),
					"offizier.special",	offi.getSpecial().getName() );
			
			t.parse("academy.offilist.list", "academy.offilist.listitem", true);
		}
		
		t.parse( "OUT", "_BUILDING" );	
		return t.getVar("OUT");
	}
}
