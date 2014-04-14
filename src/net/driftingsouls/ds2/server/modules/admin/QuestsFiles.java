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
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.scripting.QuestXMLParser;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Ermoeglicht das Installieren und Verwalten von Quest-XMLs.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Quests", name="Quest-XMLs")
public class QuestsFiles implements AdminPlugin {
	private String basename( String file ) {
		int pos = file.lastIndexOf('/');
		if( pos > -1 ) {
			file = file.substring(pos+1);
		}
		return file;
	}
	
	private String basename( String file, String suffix ) {
		file = basename(file);
		if( file.endsWith(suffix) ) {
			file = file.substring(0, file.length() - suffix.length());
		}
		return file;
	}
	
	@Override
	public void output(AdminController controller) throws IOException {
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		
		String unlink = context.getRequest().getParameterString("unlink");
		String installfile = context.getRequest().getParameterString("installfile");
		int conf = context.getRequest().getParameterInt("conf");
		int upload = context.getRequest().getParameterInt("upload");
		String info = context.getRequest().getParameterString("info");

		echo.append("<div class='gfxbox' style='width:590px'>");
		echo.append("<form action=\"./ds\" method=\"post\">\n");
		echo.append("<input type=\"text\" name=\"installfile\" value=\"" + (installfile.length() == 0 ? "Datei" : installfile) + "\" size=\"50\" />\n");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"submit\" value=\"installieren\" />\n");
		echo.append("</form>\n");
		echo.append("</div>");
		echo.append("<br />\n");
		
		final String questpath = Configuration.getSetting("QUESTPATH");
		
		if( installfile.length() > 0 ) {
			echo.append("<div class='gfxbox' style='width:590px'>");
			echo.append("Install-Log:<br />\n");
			QuestXMLParser parser = new QuestXMLParser(QuestXMLParser.Mode.INSTALL, installfile);
			echo.append(StringUtils.replace(parser.MESSAGE.getMessage(), "\n", "<br />"));
			echo.append("</div>");
			echo.append("<br />\n");
		}
		
		if( unlink.length() > 0 ) {
			String unlinkName = basename(unlink);
			
			if( conf == 0 ) {
				echo.append("<div class='gfxbox' style='width:590px;text-align:center'>");
				echo.append("Wollen sie die Quest-XML "+unlinkName+" wirklich l&ouml;schen?<br />\n");
				echo.append("<a class=\"error\" href=\"./ds?module=admin&namedplugin="+getClass().getName()+"&unlink="+unlink+"&conf=1\">ja</a> - \n");
				echo.append("<a class=\"ok\" href=\"./ds?module=admin&namedplugin="+getClass().getName()+"\">nein</a>\n");
				
				echo.append("</div>");
				echo.append("<br />\n");
			}
			else if( new File(questpath+unlink+".xml").exists() ) {
				echo.append("<div class='gfxbox' style='width:590px'>");
				
				if( new File(questpath+unlink+".install").exists() ) {
					new File(questpath+unlink+".install").delete();
					echo.append("Entferne Install-Informationen<br />\n");
				}
				new File(questpath+unlink+".xml").delete();
				echo.append("Entferne Quest-XML<br />\n");
				
				echo.append("</div>");
				echo.append("<br />\n");
			}
		}
		
		if( (upload != 0) && (context.getRequest().getUploadedFiles().size() != 0) ) {
			echo.append("<div class='gfxbox' style='width:590px'>");
			
			FileItem afile = context.getRequest().getUploadedFiles().get(0);
			
			File targetFile = new File(questpath+basename(afile.getName()));
			if( targetFile.exists() ) {
				targetFile.delete();
				echo.append("Entferne alte Datei...<br />\n");
			}
			
			try {
				afile.write(targetFile);
			
				if( SystemUtils.IS_OS_UNIX ) {
					Runtime.getRuntime().exec("chmod 0666 "+targetFile.getAbsolutePath());
				}
				
				echo.append("Die QuestXML wurde auf dem Server gespeichert<br />");
			}
			catch( Exception e ) {
				echo.append("Upload gescheitert: "+e+"<br />");
				e.printStackTrace();
			}
			
	    	echo.append("</div>");
			echo.append("<br />\n");
		}
		
		if( info.length() > 0 ) {
			echo.append("<div class='gfxbox' style='width:590px'>");
			
			if( new File(questpath+info+".install").exists() ) {
				QuestXMLParser questXML = new QuestXMLParser(QuestXMLParser.Mode.READ, info);
				
				echo.append("<div align=\"center\">&gt;"+info+"&lt;</div>\n");
				
				Map<String,Integer> questids = questXML.getInstallData("questids");
				if( questids.size() > 0 ) {
					echo.append("Quests:<br />");
					for( Map.Entry<String, Integer> entry: questids.entrySet()) {
						String xid = entry.getKey();
						echo.append("* "+xid+" [DB: "+entry.getValue()+"]<br />");	
					}
				}
				
				Map<String,Integer> answerids = questXML.getInstallData("answerids");
				if( answerids.size() > 0 ) {
					echo.append("<br />Antworten:<br />");
					for( Map.Entry<String, Integer> entry: answerids.entrySet() ) {
						String xid = entry.getKey();
						echo.append("* "+xid+" [DB: "+entry.getValue()+"]<br />");
					}
				}
				
				Map<String,Integer> dialogids = questXML.getInstallData("dialogids");
				if( dialogids.size() > 0 ) {
					echo.append("<br />Dialoge:<br />");
					for( Map.Entry<String, Integer> entry: dialogids.entrySet() ) {
						String xid = entry.getKey();
						echo.append("* "+xid+" [DB: "+entry.getValue()+"]<br />");
					}
				}
				
				Map<String,Integer> scriptids = questXML.getInstallData("scriptids");
				if( scriptids.size() > 0 ) {
					echo.append("<br />Scripte:<br />"); 
					for( Map.Entry<String,Integer> entry : scriptids.entrySet() ) {
						echo.append("* "+entry.getKey()+" [DB: "+entry.getValue()+"]<br />");
					}
				}
			}
			else {
				echo.append("Es existieren keine Install-Informationen\n");	
			}
			echo.append("</div>");
			
			echo.append("<br />\n");
		}
		
		echo.append("<div class='gfxbox' style='width:590px'>");
		echo.append("Vorhandene Quest-XMLs:<br />\n");
		
		File questdir = new File(questpath);
		File[] childlist = questdir.listFiles();
		for (File aChildlist : childlist)
		{
			if (!aChildlist.isFile())
			{
				continue;
			}

			if (aChildlist.getName().contains(".xml"))
			{
				echo.append(basename(aChildlist.getName(), ".xml") + "\n");
				echo.append(" - <a class=\"error\" href=\"./ds?module=admin&namedplugin=" + getClass().getName() + "&unlink=" + basename(aChildlist.getName(), ".xml") + "\">X</a>\n");
				echo.append("<a class=\"forschinfo\" href=\"./ds?module=admin&namedplugin=" + getClass().getName() + "&info=" + basename(aChildlist.getName(), ".xml") + "\">info</a><br />\n");
			}
		}
		echo.append("<br />\n");
		echo.append("<form action=\"./ds\" method=\"post\" enctype=\"multipart/form-data\">\n");
		echo.append("<input type=\"file\" name=\"questfile\" size=\"40\" />\n");
		echo.append("<input type=\"hidden\" name=\"MAX_FILE_SIZE\" value=\"307200\" />\n");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"upload\" value=\"1\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"submit\" value=\"hochladen\" />\n");
		echo.append("</form>\n");
		echo.append("</div>");
	}
}
