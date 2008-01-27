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
import java.util.List;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Hilfeseite fuer BBCodes (Popup)
 * @author Christopher Jung
 * @author Christian Peltz
 *
 */
public class BBCodeViewController extends DSGenerator {
	private static List<String> codes = new ArrayList<String>();
	static {
		codes.add("[url]http://www.dieGew&uuml;nschteSeite.de[/url]<br />");
		codes.add("[url=http://www.dieGew&uuml;nschteSeite.de]Eine Beschreibung[/url<br />");
		codes.add("[img]AdresseDesBildes[/img]<br />");
		codes.add("[email]Irgendeine@EmailAdresse.de[/email]<br />");
		codes.add("[email=Irgendeine@EmailAdresse.de]Eine Beschreibung[/email]<br />");
		codes.add("[size=SchriftGr&ouml;&szlig;e]Irgendein Text[/size]<br />");
		codes.add("Eine Auflistung erzeugt man so:");
		codes.add("[list=ListenTyp]");
		codes.add("[*]Eintrag1");
		codes.add("[*]Eintrag2");
		codes.add("[/list]");
		codes.add("ListenTyp kann z.B. circle,I,i,A usw. sein");
		codes.add("=ListTyp kann aber auch einfach weggelassen werden<br />");
		codes.add("[color=TextFarbe]Irgendein Text[/color]<br />");
		codes.add("[font=Schriftart]Irgendein Text[/font]<br />");
		codes.add("[align=Textausrichtung]Irgendein Text[/align]<br />");
		codes.add("[mark=HintergrundFarbe]Irgendein Text[/mark]<br />");
		codes.add("[b] text [/b]<br />");
		codes.add("[i] text [/i]<br />");
		codes.add("[u] text [/u]<br />");
	}

	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public BBCodeViewController(Context context) {
		super(context);
		
		setTemplate("bbcodeview.html");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}

	/**
	 * Anzeigen der BBCode-Liste
	 */
	@Override
	public void defaultAction(){
		TemplateEngine t = getTemplateEngine();

		t.setVar("bbcode.text", Common.implode("<br />", codes));
	}
}
