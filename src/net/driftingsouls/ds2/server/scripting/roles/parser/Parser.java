/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.scripting.roles.parser;

import net.driftingsouls.ds2.server.Location;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

/**
 * <h1>Der Parser fuer Rollendefinitionen.</h1>
 * Das eigendliche Parsen wird ueber eine ANTLR-Grammatik realisiert.
 * 
 * @author Christopher Jung
 *
 */
public class Parser {	
	/**
	 * Parst die Rollendefinition.
	 * @param roleDef Der Text mit der Rollendefinition
	 * @return Die geparste Rolle
	 * @throws ParsingException Bei Uebersaetzung
	 */
	public static RoleDefinition parse(String roleDef) throws ParsingException {	
		if( roleDef == null ) {
			throw new ParsingException("Die Rollendefinition darf nicht null sein");
		}

		// attach lexer to the input stream
		RoleLexer lexer = new RoleLexer(new ANTLRStringStream(roleDef));
		CommonTokenStream tokens = new CommonTokenStream(lexer);

		// Create parser attached to lexer
		RoleParser parser = new RoleParser(tokens);

		// start up the parser by calling the rule
		// at which you want to begin parsing.
		try {
			Tree tree = (Tree)parser.roleDefinition().getTree();
			return parseTree(tree);
		}
		catch( RecognitionException e ) {
			throw new ParsingException("Parse failed", e);
		}
	}
	
	private static RoleDefinition parseTree(Tree tree) {
		RoleDefinitionImpl roleDef = new RoleDefinitionImpl();

		int index = 0;
		for( ; index < tree.getChildCount(); index++ ) {
			Tree child = tree.getChild(index);
			if( child.getType() == RoleParser.Identifier ) {
				roleDef.setRoleName(child.getText());
				index++;
				break;
			}
		}
		
		// Attribute lesen
		for( ; index < tree.getChildCount(); index++ ) {
			Tree child = tree.getChild(index);
			if( child.getType() == RoleParser.Identifier ) {
				String name = child.getText();
				index += 2; // IS ignorieren und direkt zum uebernaechsten Element springen

				int type = tree.getChild(index).getType();
				String valueStr = tree.getChild(index).getText();
				Object value = null;

				switch( type ) {
				case RoleParser.Text:
					// " entfernen
					valueStr = valueStr.substring(1, valueStr.length()-1);
					// \ vor Escape-Chars entfernen
					value = valueStr.replaceAll("\\\\([\"\\\\]{1})", "$1");
					break;

				case RoleParser.Number:
					value = Long.parseLong(valueStr);
					break;

				case RoleParser.Location:
					value = Location.fromString(valueStr);
					break;
				}

				roleDef.setAttribute(name, value);
			}
		}
		
		return roleDef;
	}
}
