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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.ComNetEntry;
import net.driftingsouls.ds2.server.entities.ComNetVisit;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.bbcode.Smilie;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Das ComNet - Alle Funktionalitaeten des ComNets befinden sich in 
 * dieser Klasse 
 * @author Christopher Jung
 *
 * @urlparam Integer channel Die ID des ausgewaehlten ComNet-Kanals (0, falls keiner ausgewaehlt wurde)
 */
public class ComNetController extends TemplateGenerator {
	private int activeChannel = 1;
	private ComNetChannel activeChannelObj = null;
	
	private static final Logger log = Logger.getLogger(ComNetController.class);
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ComNetController(Context context) {
		super(context);
		
		setTemplate("comnet.html");
		
		parameterNumber("channel");
		
		setPageTitle("Com-Net");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		
		if( getInteger("channel") > activeChannel ) {
			activeChannel = getInteger("channel");
		}
		
		ComNetChannel tmp = (ComNetChannel)db.get(ComNetChannel.class, activeChannel);
		if( tmp == null ) {
			addError("Die angegebene Frequenz existiert nicht - es wird die Standardfrequenz benutzt");
			
			tmp = (ComNetChannel)db.get(ComNetChannel.class, 1);
			activeChannel = 1;
		}
		
		activeChannelObj = tmp;
		
		t.setVar(	"channel.id",	activeChannel,
					"channel.name",	Common._title(activeChannelObj.getName()) );
		
		return true;
	}
	
	/**
	 * Sucht im aktuell ausgewaehlten ComNet-Kanal Posts nach bestimmten Kriterien.
	 * Sollten keine Kriterien angegeben sein, so wird das Eingabefenster fuer die Suche angezeigt
	 * 
	 * @urlparam Integer searchtype der Suchmodus. 
	 * 		1 - Suchen nach Teilen eines Titels
	 * 		2 - Suchen nach Teilen eines Posts
	 * 		3 - Suchen nach Posts eines bestimmten Spielers auf Basis der Spieler-ID
	 * @urlparam String/Integer search Der Suchbegriff, abhaengig vom Suchmodus
	 *  @urlparam Integer back Der Offset der anzuzeigenden Posts. Ein Offset 
	 * 	von 0 bedeutet der neuste Post. Je groesser der Wert umso aelter der Post
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void searchAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = (User)getUser();
		
		parameterString("search");
		parameterNumber("searchtype");
		parameterNumber("back");
		
		String search = getString("search");
		int searchtype = getInteger("searchtype");
		int back = getInteger("back");
		
		if( back < 0 ) { 
			back = 0;
		}
		
		if( searchtype == 0 ) {
			t.setVar("show.searchform", 1);
			return;
		}
		
		t.setVar("show.read",1);
		if( !activeChannelObj.isReadable(user) ) {
			addError( "Sie sind nicht berechtigt diesee Frequenz zu empfangen", Common.buildUrl("default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
		
		if( activeChannelObj.isWriteable(user) ) {
			t.setVar("channel.writeable",1);
		}

		db.update("UPDATE skn_visits SET time='",Common.time(),"' WHERE user=",user.getId()," AND channel=",activeChannel);
	
		t.setBlock("_COMNET","posts.listitem","posts.list");

		t.setVar(	"posts.action",		"search",
					"search.string",	search,
					"search.type",		searchtype );

		StringBuilder countstring = new StringBuilder("SELECT count(*) count FROM skn WHERE channel=? AND ");
		StringBuilder querystring = new StringBuilder("SELECT * FROM skn WHERE channel=? AND ");
 
		if( searchtype == 1 ) {
			querystring.append("head LIKE ?");
			countstring.append("head LIKE ?");
		}
		else if( searchtype == 2 ) {
			querystring.append("text LIKE ?");
			countstring.append("text LIKE ?");
		}
		else if( searchtype == 3 ) {
			querystring.append("userid=?");
			countstring.append("userid=?");
		}
		querystring.append(" ORDER BY post DESC LIMIT ?, 10");

		int channelPostCount = db.prepare(countstring.toString())
			.first(activeChannel, (searchtype == 3 ? Integer.parseInt(search) : "%"+search+"%"))
			.getInt("count");
		 
		if( channelPostCount == 0 ) {
			t.setVar("show.read", 0);
			t.setVar("show.searcherror", 1);
		}

		int b = back + 10;
		int v = back - 10;
		
		if( b > channelPostCount ) {
			b = 0;	
		}
				
		t.setVar(	"show.vor",		v,
					"show.back",	b );
 
		if( back > 0 ) {
			t.setVar("read.nextpossible",1);
		}

		int i = 0;
		
		SQLQuery ref = db.prepare(querystring.toString())
			.query(activeChannel, (searchtype == 3 ? Integer.parseInt(search) : "%"+search+"%"), back);
		while( ref.next() ) {
			t.start_record();
			int post = channelPostCount - back - i;
			String head = ref.getString("head");
			String text = ref.getString("text");

			text = Smilie.parseSmilies(Common._text(text));

			if( head.length() == 0 ) {
				head = "-";
			}
			else {
				head = Common._title(head);
			}

			t.setVar(	"post.pic",			ref.getInt("pic"),
						"post.postid",		post,
						"post.id",			ref.getInt("userid"),
						"post.name",		Common._title(ref.getString("name")),
						"post.time",		Common.date("d.m.Y H:i:s",ref.getLong("time")),
						"post.title",		head,
						"post.text",		text,
						"post.allypic",		ref.getInt("allypic"),
						"post.ingametime",	Common.getIngameTime(ref.getInt("tick")) );

			i++;

			t.parse("posts.list","posts.listitem",true);
			t.stop_record();
			t.clear_record();
		}
		ref.free();
	}

	/**
	 * Zeigt den Inhalt des ausgewaehlten ComNet-Kanals an.
	 * Es werden immer nur 10 Posts ab einem angegebenen Offset angezeigt.
	 * @urlparam Integer back Der Offset der anzuzeigenden Posts. Ein Offset 
	 * 	von 0 bedeutet der neuste Post. Je groesser der Wert umso aelter der Post
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void readAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		
		parameterNumber("back");
		int back = getInteger("back");
		
		t.setVar("show.read",1);
		if( !activeChannelObj.isReadable(user) ) {
			addError( "Sie sind nicht berechtigt diesee Frequenz zu empfangen", Common.buildUrl("default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
	
		if( activeChannelObj.isWriteable(user) ) {
			t.setVar("channel.writeable",1);
		}
	
		db.createQuery("update ComNetVisit set time= :time where user= :user and channel= :channel")
			.setLong("time", Common.time())
			.setEntity("user", user)
			.setEntity("channel", this.activeChannelObj)
			.executeUpdate();
		
		if( back < 0 ) {
			back = 0;
		}
		
		int channelPostCount = this.activeChannelObj.getPostCount();
		
		int b = back + 10;
		int v = back - 10;
		
		if( b > channelPostCount ) {
			b = 0;	
		}
		
		t.setVar(	"show.vor",		v,
					"show.back",	b );

		if( back > 0 ) {
			t.setVar("read.nextpossible",1);
		}
		
		t.setVar("posts.action","read");

		t.setBlock("_COMNET","posts.listitem","posts.list");

		int i = 0;
		
		List<?> postList = db.createQuery("from ComNetEntry where channel= :channel order by post desc")
			.setEntity("channel", this.activeChannelObj)
			.setFirstResult(back)
			.setMaxResults(10)
			.list();
		for( Iterator<?> iter=postList.iterator(); iter.hasNext(); ) {
			ComNetEntry post = (ComNetEntry)iter.next();
			
			t.start_record();
			int postNumber = channelPostCount - back - i;
			String head = post.getHead();
			String text = post.getText();

			text = Smilie.parseSmilies(Common._text(text));

			if( head.length() == 0 ) {
				head = "-";
			}
			else {
				head = Common._title(head);
			}

			t.setVar(	"post",				post,
						"post.user.rang.name", Medals.get().rang(post.getUser().getRang()).getName(),
						"post.postid",		postNumber,
						"post.name",		Common._title(post.getName()),
						"post.time",		Common.date("d.m.Y H:i:s",post.getTime()),
						"post.title",		head,
						"post.text",		text,
						"post.ingametime",	Common.getIngameTime(post.getTick()) );

			i++;

			t.parse("posts.list","posts.listitem",true);
			t.stop_record();
			t.clear_record();
		}
	}
	
	/**
	 * Postet einen ComNet-Post im aktuell ausgewaehlten ComNet-Kanal.
	 * @urlparam String text Der Text des Posts
	 * @urlparam String head Der Titel des Posts
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void sendenAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		if( !activeChannelObj.isWriteable(user) ) {
			addError( "Sie sind nicht berechtigt auf dieser Frequenz zu senden", Common.buildUrl("default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
		
		parameterString("text");
		parameterString("head");

		String text = getString("text");
		String head = getString("head");

		//In die DB eintragen
		ComNetEntry entry = new ComNetEntry(user, activeChannelObj);
		entry.setHead(head);
		entry.setText(text);
		db.persist(entry);
		
		t.setVar("show.submit",1);
	}
	
	/**
	 * Zeigt die Seite zum Verfassen eines neuen ComNet-Posts, im aktuell 
	 * ausgewaehlten ComNet-Kanal, an.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void writeAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		if( !activeChannelObj.isWriteable(user) ) {
			addError( "Sie sind nicht berechtigt auf dieser Frequenz zu senden", Common.buildUrl("default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
		
		t.setVar(	"show.inputform",	1,
					"post.raw.title",	"",
					"post.raw.text",	"" );
	}
	
	/**
	 * Zeigt eine Vorschau fuer einen geschriebenen, jedoch noch nicht geposteten, ComNet-Post an.
	 * Nach einer Vorschau kann der Post im aktuell ausgewaehlten ComNet-Kanal gepostet werden.
	 * @urlparam String text Der Text des Posts
	 * @urlparam String head Der Titel des Posts
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void vorschauAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		if( !activeChannelObj.isWriteable(user) ) {
			addError( "Sie sind nicht berechtigt auf dieser Frequenz zu senden", Common.buildUrl("default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
		
		parameterString("text");
		parameterString("head");

		String text = getString("text");
		String head = getString("head");
		
		String tmpText = Smilie.parseSmilies(Common._text(text));
		String tmpHead = Common._title(head);
		
		//Aktuellen Tick ermitteln
		int tick = getContext().get(ContextCommon.class).getTick();
		
		Rang userRank = Medals.get().rang(user.getRang());
		String userRankName = "";
		if(userRank != null)
		{
			userRankName = userRank.getName();
		}
		else
		{
			log.debug("Illegal user rank for user " + user.getId() + " rankid: " + user.getRang());
		}

		t.setVar(	"show.vorschau",	1,
					"show.inputform",	1,
					"post.title",		tmpHead,
					"post.text",		tmpText,
					"post.raw.title",	head,
					"post.raw.text",	text,
					"post.postid",		1,
					"user",		user,
					"user.rang.name", 	userRankName,
					"post.name",		Common._title(user.getName()),
					"post.id",			user.getId(),
					"post.pic",			user.getId(),
					"post.allypic",		user.getAlly().getId(),
					"post.time",		Common.date("Y-m-d H:i:s"),
					"post.ingametime",	Common.getIngameTime(tick) );	
	}
	
	/**
	 * Zeigt die Liste aller lesbaren ComNet-Kanaele an
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		
		ComNetChannel channel = activeChannelObj;
		
		t.setVar("show.channellist",1);

		if( channel.isWriteable(user) ) {	
			t.setVar("channel.writeable", 1);
		}

		if( channel.isReadable(user) ) {
			t.setVar("channel.readable",1);
		}

		// Letzte "Besuche" auslesen
		Map<ComNetChannel,ComNetVisit> visits = new HashMap<ComNetChannel,ComNetVisit>();

		List<?> visitList = db.createQuery("from ComNetVisit where user= :user")
			.setEntity("user", user)
			.list();
		for( Iterator<?> iter = visitList.iterator(); iter.hasNext(); ) {
			ComNetVisit avisit = (ComNetVisit)iter.next();
			visits.put(avisit.getChannel(), avisit);
		}

		t.setBlock("_COMNET","channels.listitem","channels.list");

		int lastowner = 0;
		
		Iterator<?> chnlIter = db.createQuery( "from ComNetChannel order by allyOwner" ).iterate();
		while( chnlIter.hasNext() ) {
			ComNetChannel achannel = (ComNetChannel)chnlIter.next();
			
			if( !achannel.isReadable(user) ) {
				continue;
			}
			
			t.start_record();
			
			if( (lastowner == 0) && (lastowner != achannel.getAllyOwner()) ) {
				t.setVar("thischannel.showprivateinfo",1);
				lastowner = achannel.getAllyOwner();
			} 
		
			ComNetVisit visit = visits.get(achannel);
		
			if( visit == null ) {
				visit = new ComNetVisit(user, achannel);
				visit.setTime(0);
				db.persist(visit);
			}	

			t.setVar(	"thischannel.id",	achannel.getId(),
						"thischannel.name",	Common._title(achannel.getName()) );

			Long lastpost = (Long)db.createQuery("select max(time) from ComNetEntry where channel= :channel")
				.setEntity("channel", achannel)
				.iterate().next();

			if( lastpost == null ) {
				lastpost = 0L;
			}
			
			if( achannel.getId() == channel.getId() ) {
				t.setVar("thischannel.isactive",1);
			}

			if( lastpost > visit.getTime() ) {
				t.setVar("thischannel.newposts",1);
			}

			t.parse("channels.list","channels.listitem",true);
			t.stop_record();
			t.clear_record();
		}
	}
}
