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
package net.driftingsouls.ds2.server.scripting;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.Database;

/**
 * Der ScriptParser
 * @author Christopher Jung
 *
 */
public class ScriptParser {
	public enum NameSpace {
		ACTION,
		QUEST
	}
	
	public enum Args {
		VARIABLE(1),
		PLAIN(2),
		REG(4),
		PLAIN_REG(PLAIN.value() | REG.value()),
		PLAIN_VARIABLE(PLAIN.value() | VARIABLE.value()),
		REG_VARIABLE(REG.value() | VARIABLE.value()),
		PLAIN_REG_VARIABLE(PLAIN.value() | REG.value() | VARIABLE.value());

		private int value = 0;
		Args(int value) {
			this.value = value;
		}
		
		public int value() {
			return value;
		}
	}
	
	public ScriptParser(NameSpace namespace) {
		super();
		
	}

	public void registerCommand( String command, String function, Class cls, Args ... args ) {
		throw new RuntimeException("STUB");
	}
	
	public void setLogFunction( String func ) {
		// TODO
		Common.stub();
	}
	
	public void log( String txt ) {
		throw new RuntimeException("STUB");
	}
	
	public void startLogging() {
		throw new RuntimeException("STUB");
	}
	
	public void stopLogging() {
		throw new RuntimeException("STUB");
	}
	
	public void setShip( Object ship ) {
		// TODO
		Common.stub();
	}
	
	public Object getShip() {
		throw new RuntimeException("STUB");
	}
	
	public void addExecutionData( String data ) {
		// TODO
		Common.stub();
	}
	
	public void setExecutionData( String data ) {
		// TODO
		Common.stub();
	}
	
	public String getExecutionData() {
		throw new RuntimeException("STUB");
	}
	
	public String getOutput() {
		// TODO
		Common.stub();
		return "";
	}
	
	public void out( String text ) {
		throw new RuntimeException("STUB");
	}
	
	public String getRegister( String reg ) {
		// TODO
		Common.stub();
		return "";
	}
	
	public void setRegister( String reg, String data ) {
		// TODO
		Common.stub();
	}
	
	public void cleanup() {
		throw new RuntimeException("STUB");
	}
	
	public String evalTerm( String term ) {
		throw new RuntimeException("STUB");
	}
	
	public String getParameter( int number ) {
		throw new RuntimeException("STUB");
	}
	
	public void executeScript( Database db, String script ) {
		executeScript(db, script, "");
	}
	
	public void executeScript( Database db, String script, String parameter ) {
		// TODO
		Common.stub();
	}
}
