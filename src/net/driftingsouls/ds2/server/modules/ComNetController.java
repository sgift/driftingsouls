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
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Das ComNet - Alle Funktionalitaeten des ComNets befinden sich in 
 * dieser Klasse 
 * @author Christopher Jung
 *
 * @urlparam Integer channel Die ID des ausgewaehlten ComNet-Kanals (0, falls keiner ausgewaehlt wurde)
 */
public class ComNetController extends DSGenerator {
	private int activeChannel = 1;
	private Channel activeChannelObj = null;
	
	/**
	 * Repraesentiert einen ComNet-Kanal
	 *
	 */
	private static class Channel {
		final int id;
		final int allyOwner;
		final String name;
		final boolean writeall;
		final boolean readall;
		final boolean writenpc;
		final boolean readnpc;
		final int writeally;
		final int readally;
		final String readplayer;
		final String writeplayer;
		
		Channel( SQLResultRow row ) {
			this.id = row.getInt("id");
			this.allyOwner = row.getInt("allyowner");
			this.name = row.getString("name");
			this.writeall = row.getBoolean("writeall");
			this.readall = row.getBoolean("readall");
			this.writenpc = row.getBoolean("writenpc");
			this.readnpc = row.getBoolean("readnpc");
			this.writeally = row.getInt("writeally");
			this.readally = row.getInt("readally");
			this.writeplayer = row.getString("writeplayer");
			this.readplayer = row.getString("readplayer");
		}
		
		/**
		 * Prueft, ob der ComNet-Kanal fuer den angegebenen Benutzer lesbar ist
		 * @param user Der Benutzer
		 * @return <code>true</code>, falls er lesbar ist
		 */
		boolean isReadable( User user ) {
			if( readall || ((user.getID() < 0) && readnpc) || 
				((user.getAlly() != 0) && (readally == user.getAlly())) || 
				(user.getAccessLevel() >= 100) ) {
					
				return true;
			}
			
			if( writeall || ((user.getID() < 0) && writenpc) || 
				((user.getAlly() != 0) && (writeally == user.getAlly())) || 
				(user.getAccessLevel() >= 100) ) {
					
				return true;
			}
			
			if( readplayer.length() != 0 ) {
				Integer[] playerlist = Common.explodeToInteger(",",readplayer);
				if( Common.inArray(user.getID(), playerlist) ) {
					return true;
				}
			}
			
			if( writeplayer.length() != 0 ) {
				Integer[] playerlist = Common.explodeToInteger(",",writeplayer);
				if( Common.inArray(user.getID(), playerlist) ) {
					return true;
				}
			}
			
			return false;
		}
		
		/**
		 * Prueft, ob der ComNet-Kanal fuer den angegebenen Benutzer schreibbar ist
		 * @param user Der Benutzer
		 * @return <code>true</code>, falls er schreibbar ist
		 */
		boolean isWriteable( User user ) {
			if( writeall || ((user.getID() < 0) && writenpc) || 
				((user.getAlly() != 0) && (writeally == user.getAlly())) || 
				(user.getAccessLevel() >= 100) ) {
					
				return true;
			}

			if( writeplayer.length() != 0 ) {
				Integer[] playerlist = Common.explodeToInteger(",",writeplayer);
				if( Common.inArray(user.getID(), playerlist) ) {
					return true;
				}
			}
			
			return false;
		}
	}
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ComNetController(Context context) {
		super(context);
		
		setTemplate("comnet.html");
		
		parameterNumber("channel");		
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		if( getInteger("channel") > activeChannel ) {
			activeChannel = getInteger("channel");
		}
		
		SQLResultRow tmp = db.first("SELECT * FROM skn_channels WHERE id=",activeChannel);
		if( tmp.isEmpty() ) {
			addError("Die angegebene Frequenz existiert nicht - es wird die Standardfrequenz benutzt");
			
			tmp = db.first("SELECT * FROM skn_channels WHERE id=1");
			activeChannel = 1;
		}
		
		Channel channel = new Channel(tmp);
		
		
		activeChannelObj = channel;
		
		t.set_var(	"channel.id",	activeChannel,
					"channel.name",	Common._title(channel.name) );
		
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
	public void searchAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
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
			t.set_var("show.searchform", 1);
			return;
		}
		
		t.set_var("show.read",1);
		if( !activeChannelObj.isReadable(user) ) {
			addError( "Sie sind nicht berechtigt diesee Frequenz zu empfangen", Common.buildUrl(getContext(), "default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
		
		if( activeChannelObj.isWriteable(user) ) {
			t.set_var("channel.writeable",1);
		}

		db.update("UPDATE skn_visits SET time='",Common.time(),"' WHERE user=",user.getID()," AND channel=",activeChannel);
	
		t.set_block("_COMNET","posts.listitem","posts.list");

		t.set_var(	"posts.action",		"search",
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
			t.set_var("show.read", 0);
			t.set_var("show.searcherror", 1);
		}

		int b = back + 10;
		int v = back - 10;
		
		if( b > channelPostCount ) {
			b = 0;	
		}
				
		t.set_var(	"show.vor",		v,
					"show.back",	b );
 
		if( back > 0 ) {
			t.set_var("read.nextpossible",1);
		}

		int i = 0;
		
		SQLQuery ref = db.prepare(querystring.toString())
			.query(activeChannel, (searchtype == 3 ? Integer.parseInt(search) : "%"+search+"%"), back);
		while( ref.next() ) {
			t.start_record();
			int post = channelPostCount - back - i;
			String head = ref.getString("head");
			String text = ref.getString("text");

			text = Common.smiliesParse(Common._text(text));

			if( head.length() == 0 ) {
				head = "-";
			}
			else {
				head = Common._title(head);
			}

			t.set_var(	"post.pic",			ref.getInt("pic"),
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
	public void readAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		parameterNumber("back");
		int back = getInteger("back");
		
		t.set_var("show.read",1);
		if( !activeChannelObj.isReadable(user) ) {
			addError( "Sie sind nicht berechtigt diesee Frequenz zu empfangen", Common.buildUrl(getContext(), "default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
	
		if( activeChannelObj.isWriteable(user) ) {
			t.set_var("channel.writeable",1);
		}
	
		db.update("UPDATE skn_visits SET time='",Common.time(),"' WHERE user=",user.getID()," AND channel=",activeChannel);

		if( back < 0 ) {
			back = 0;
		}
		
		int channelPostCount = db.first("SELECT count(*) count FROM skn WHERE channel=",activeChannel).getInt("count");
		
		int b = back + 10;
		int v = back - 10;
		
		if( b > channelPostCount ) {
			b = 0;	
		}
		
		t.set_var(	"show.vor",		v,
					"show.back",	b );

		if( back > 0 ) {
			t.set_var("read.nextpossible",1);
		}
		
		t.set_var("posts.action","read");

		t.set_block("_COMNET","posts.listitem","posts.list");

		int i = 0;
		
		SQLQuery ref = db.query("SELECT * FROM skn WHERE channel=",activeChannel," ORDER BY post DESC LIMIT ",back,", 10");
		while( ref.next() ) {
			t.start_record();
			int post = channelPostCount - back - i;
			String head = ref.getString("head");
			String text = ref.getString("text");

			text = Common.smiliesParse(Common._text(text));

			if( head.length() == 0 ) {
				head = "-";
			}
			else {
				head = Common._title(head);
			}

			t.set_var(	"post.pic",			ref.getInt("pic"),
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
	 * Postet einen ComNet-Post im aktuell ausgewaehlten ComNet-Kanal.
	 * @urlparam String text Der Text des Posts
	 * @urlparam String head Der Titel des Posts
	 *
	 */
	public void sendenAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		if( !activeChannelObj.isWriteable(user) ) {
			addError( "Sie sind nicht berechtigt auf dieser Frequenz zu senden", Common.buildUrl(getContext(), "default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
		
		parameterString("text");
		parameterString("head");

		String text = getString("text");
		String head = getString("head");
	
		//Logo ermitteln
		int pic = user.getID();
		int allypic = 0;
		if( user.getAlly() != 0 ) {
			allypic = user.getAlly();
		}

		//Aktuellen Tick ermitteln
		int tick = getContext().get(ContextCommon.class).getTick();

		//In die DB eintragen
		db.prepare("INSERT INTO skn " +
				"(userid,name,head,text,time,pic,allypic,channel,tick) " +
				"VALUES " +
				"( ?, ?, ?, ?, ?, ?, ?, ?, ?)")
			.update(user.getID(), user.getName(), head, text, Common.time(), pic, allypic, activeChannel, tick);
		
		t.set_var("show.submit",1);
	}
	
	/**
	 * Zeigt die Seite zum Verfassen eines neuen ComNet-Posts, im aktuell 
	 * ausgewaehlten ComNet-Kanal, an.
	 *
	 */
	public void writeAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		if( !activeChannelObj.isWriteable(user) ) {
			addError( "Sie sind nicht berechtigt auf dieser Frequenz zu senden", Common.buildUrl(getContext(), "default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
		
		t.set_var(	"show.inputform",	1,
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
	public void vorschauAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		if( !activeChannelObj.isWriteable(user) ) {
			addError( "Sie sind nicht berechtigt auf dieser Frequenz zu senden", Common.buildUrl(getContext(), "default", "channel", activeChannel) );
			setTemplate("");
			
			return;			
		}
		
		parameterString("text");
		parameterString("head");

		String text = getString("text");
		String head = getString("head");
		
		String tmpText = Common.smiliesParse(Common._text(text));
		String tmpHead = Common._title(head);
		
		//Aktuellen Tick ermitteln
		int tick = getContext().get(ContextCommon.class).getTick();

		t.set_var(	"show.vorschau",	1,
					"show.inputform",	1,
					"post.title",		tmpHead,
					"post.text",		tmpText,
					"post.raw.title",	head,
					"post.raw.text",	text,
					"post.postid",		1,
					"post.name",		Common._title(user.getName()),
					"post.id",			user.getID(),
					"post.pic",			user.getID(),
					"post.allypic",		user.getAlly(),
					"post.time",		Common.date("Y-m-d H:i:s"),
					"post.ingametime",	Common.getIngameTime(tick) );	
	}
	
	/**
	 * Zeigt die Liste aller lesbaren ComNet-Kanaele an
	 */
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		Channel channel = activeChannelObj;
		
		t.set_var("show.channellist",1);

		if( channel.isWriteable(user) ) {	
			t.set_var("channel.writeable", 1);
		}

		if( channel.isReadable(user) ) {
			t.set_var("channel.readable",1);
		}

		// Letzte "Besuche" auslesen
		Map<Integer,SQLResultRow> visits = new HashMap<Integer,SQLResultRow>();

		SQLQuery avisit = db.query("SELECT id,time,channel FROM skn_visits WHERE user='",user.getID(),"'");
		while( avisit.next() ) {
			visits.put(avisit.getInt("channel"), avisit.getRow());
		}
		avisit.free();


		t.set_block("_COMNET","channels.listitem","channels.list");

		int lastowner = 0;
		
		SQLQuery chnl = db.query( "SELECT * FROM skn_channels ORDER BY allyowner" );
		while( chnl.next() ) {
			Channel achannel = new Channel(chnl.getRow());
			
			if( !achannel.isReadable(user) ) {
				continue;
			}
			
			t.start_record();
			
			if( (lastowner == 0) && (lastowner != achannel.allyOwner) ) {
				t.set_var("thischannel.showprivateinfo",1);
				lastowner = achannel.allyOwner;
			} 
		
			SQLResultRow visit = visits.get(achannel.id);
		
			if( visit == null ) {
				db.update("INSERT INTO skn_visits (user,channel,time) VALUES (",user.getID(),",",achannel.id,",0)");
				visit = new SQLResultRow();
				visit.put("id", db.insertID());
				visit.put("time", 0);
			}	

			t.set_var(	"thischannel.id",	achannel.id,
						"thischannel.name",	Common._title(achannel.name) );

			long lastpost = db.first("SELECT max(time) maxtime FROM skn WHERE channel=",achannel.id).getLong("maxtime");


			if( achannel.id == channel.id ) {
				t.set_var("thischannel.isactive",1);
			}

			if( lastpost > visit.getInt("time") ) {
				t.set_var("thischannel.newposts",1);
			}

			t.parse("channels.list","channels.listitem",true);
			t.stop_record();
			t.clear_record();
		}
		chnl.free();
	}
}
