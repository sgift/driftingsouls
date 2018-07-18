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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Hilfeseite fuer BBCodes (Popup).
 * @author Christopher Jung
 * @author Christian Peltz
 *
 */
@Module(name="bbcodeview")
public class BBCodeViewController extends Controller
{
	private static List<String> codes = new ArrayList<>();
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
		codes.add("[youtube=breite,h&ouml;he]Youtube-Code[/youtube]<br />");
		codes.add("Zudem gibt es auch einige speziellere Tags:<br />");
		codes.add("[shiptype]ShiffsTyp-Id[/shiptype]<br />");
		codes.add("[ship=ID]Eine Beschreibung[/ship]<br />");
		codes.add("[map]Sys:X/Y[/map]<br />");
		codes.add("[base=BaseID]Eine Beschreibung[/base]<br />");
		codes.add("[userprofile=SpielerID]Eine Beschreibung[/userprofile]<br />");
		codes.add("[resource=iID|0|0,PARAM]Anzahl[/resource]");
		codes.add("Als PARAM kann i oder n angegeben werden, um nur den Namen oder nur das Bild anzeigen zu lassen. Das ,PARAM kann aber auch komplett weggelassen werden.");
		codes.add("Beispiel f&uuml;r 5 Nahrung: [resource=i16|0|0]5[/resource]<br />");
	}

	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public BBCodeViewController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;
	}

	/**
	 * Anzeigen der BBCode-Liste.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(){
		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("bbcode.text", Common.implode("<br />", codes));

		return t;
	}
}
