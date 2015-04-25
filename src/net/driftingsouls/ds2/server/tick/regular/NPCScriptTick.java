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
package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.scripting.EngineIdentifier;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.tick.TickController;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.util.List;

/**
 * Fuehrt NPC-Aktionsscripte aus.
 * @author Christopher Jung
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class NPCScriptTick extends TickController {
	class TickLogger extends Writer {
		private boolean first = true;

		@Override
		public void close() {
			NPCScriptTick.this.slog("#########################ENDE#############################\n");

			first = true;
		}

		@Override
		public void flush() {
			// EMPTY
		}

		@Override
		public void write(@Nonnull char[] cbuf, int off, int len) {
			if( first ) {
				NPCScriptTick.this.slog("###################Scriptparser [Debug]###################\n");
				first = false;
			}

			NPCScriptTick.this.slog(new String(cbuf, off, len));
		}
	}

	@Override
	protected void prepare() {
		// EMPTY
	}

	@Override
	protected void tick() {
		org.hibernate.Session db = getContext().getDB();
		this.log("Fuehre automatische NPC-Aktionen durch");

		Transaction transaction = db.beginTransaction();
		List<User> users = Common.cast(db.createQuery("from User where locate('execnotes',flags)!=0").list());
		for( User user : users )
		{
			Writer logger = new NullLogger();
			if( user.hasFlag( UserFlag.SCRIPT_DEBUGGING ) )
			{
				logger = new TickLogger();
			}

			this.log("+++++++++ User: "+user.getId()+" +++++++++");
			List<Ship> ships = Common.cast(db
					.createQuery("from Ship where id>0 and owner=:owner and battle is null and scriptData is not null and scriptData.script is not null")
					.setEntity("owner", user)
					.list());
			for(Ship ship: ships)
			{
				try
				{
					this.log("+++ Ship "+ship.getId()+" +++");

					final String engineName = EngineIdentifier.identifyEngine(ship.getScript());
					if( engineName == null )
					{
						this.log("Unbekannte ScriptEngine");
						continue;
					}

					final ScriptEngine scriptparser = getContext().get(ContextCommon.class)
						.getScriptParser(engineName);

					byte[] scriptExecData = ship.getScriptExeData();
					if( (scriptExecData != null) && (scriptExecData.length > 0) )
					{
						scriptparser.setContext(
								ScriptParserContext.fromStream(new ByteArrayInputStream(scriptExecData))
						);
					}
					else
					{
						scriptparser.setContext(new ScriptParserContext());
					}

					scriptparser.getContext().setErrorWriter(logger);

					scriptparser.getContext().setAttribute("_SHIP", ship, ScriptContext.ENGINE_SCOPE);
					scriptparser.eval( ship.getScript() );

					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ScriptParserContext.toStream(scriptparser.getContext(), out);
					scriptExecData = out.toByteArray();

					// Pruefen, ob das Script nicht via !RESETSCRIPT geloescht wurde
					if( ship.getScript() != null )
					{
						ship.setScriptExeData(scriptExecData);
					}

					transaction.commit();
					transaction = db.beginTransaction();
				}
				catch( Exception e )
				{
					transaction.rollback();
					transaction = db.beginTransaction();
					this.log("[FEHLER] Kann Script auf Schiff "+ship.getId()+" nicht ausfuehren: "+e);
					e.printStackTrace();
					Common.mailThrowable(e, "[DS2J] NPCScriptTick Exception", "Schiff: "+ship.getId()+"\nUser: "+ship.getOwner().getId());

					if( e instanceof StaleObjectStateException ) {
						StaleObjectStateException sose = (StaleObjectStateException)e;
						db.evict(db.get(sose.getEntityName(), sose.getIdentifier()));
					}
					else {
						db.evict(ship);
					}
				}
			}
		}

		transaction.commit();
	}

	@Override
	public void slog(String string)
	{
		super.slog(string);
	}
}
