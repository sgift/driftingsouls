package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Version;
import net.driftingsouls.ds2.server.framework.pipeline.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * <p>Ausgabehilfe fuer HTML.</p>
 * Attribute:
 * <ul>
 * <li><code>header</code> - String mit weiteren Header-Text
 * <li><code>module</code> - Das gerade ausgefuehrte Modul
 * <li><code>pagetitle</code> - Der Titel der Seite
 * <li><code>pagemenu</code> - Eine Liste von Menueeintraegen fuer die Seite
 * </ul>
 *
 */
@Component
public class HtmlOutputHandler extends OutputHandler
{
	private Version version;

	/**
	 * Injiziert die momentane DS-Version.
	 * @param version Die DS-Version
	 */
	@Autowired
	public void setVersion(Version version) {
		this.version = version;
	}

	@Override
	public void printHeader() throws IOException
	{
		Response response = getContext().getResponse();

		response.setContentType("text/html", "UTF-8");

		Writer sb = response.getWriter();

		final boolean devMode = !Configuration.isProduction();

		sb.append("<!DOCTYPE html>\n");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\" lang=\"de\">\n");
		sb.append("<head>\n");
		sb.append("<title>Drifting Souls 2</title>\n");
		sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
		sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=9\">\n");
		if( devMode )
		{
			appendDevModeCss(sb);
		}
		else
		{
			sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"./data/css/v").append(version.getVersion()).append("/format.css\" />\n");
		}
		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"./data/css/ui-darkness/00_jquery-ui-1.8.20.css\" />\n");
		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"./data/css/ui-darkness/10_jquery-ui.css\" />\n");

		sb.append("<!--[if IE]>\n");
		sb.append("<style type=\"text/css\">@import url(./data/css/v").append(version.getVersion()).append("/format_fuer_den_dummen_ie.css);</style>\n");
		sb.append("<![endif]-->\n");

		if( this.getAttribute("header") != null ) {
			sb.append(this.getAttribute("header").toString());
		}

		sb.append("</head>\n");

		sb.append("<body ").append(getAttributeString("bodyParameters")).append(" >\n");
		sb.append("<input type='hidden' name='currentDsModule' id='currentDsModule' value='").append(this.getAttributeString("module")).append("' />");

		if( devMode )
		{
			appendDevModeJavascript(sb);
		}
		else
		{
			sb.append("<script src=\"./data/javascript/v").append(version.getVersion()).append("/ds.js\" type=\"text/javascript\"></script>\n");
		}
		sb.append("<div id=\"error-placeholder\"></div>\n");
	}

	private void appendDevModeCss(Writer sb) throws IOException
	{
		File cssdir = new File(Configuration.getAbsolutePath()+"data/css/common");

		for( String filename : new TreeSet<>(Arrays.asList(cssdir.list())) )
		{
			sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"./data/css").append("/common/").append(filename).append("\" />\n");
		}
	}

	private void appendDevModeJavascript(Writer sb) throws IOException
	{
		File jsdir = new File(Configuration.getAbsolutePath()+"data/javascript/");
		File libdir = new File(jsdir.getAbsolutePath()+"/libs");
		File commondir = new File(jsdir.getAbsolutePath()+"/common");

		List<String> specialJsLibs = Arrays.asList("jquery-2.1.1.min.js", "jquery-ui-1.11.2.min.js");

		for (String specialJsLib : specialJsLibs)
		{
			sb.append("<script src=\"./data/javascript/libs/").append(specialJsLib).append("\" type=\"text/javascript\"></script>\n");
		}

		for( String filename : new TreeSet<>(Arrays.asList(libdir.list())) )
		{
			if( specialJsLibs.contains(filename) || !filename.endsWith(".js") )
			{
				continue;
			}
			sb.append("<script src=\"./data/javascript/libs/").append(filename).append("\" type=\"text/javascript\"></script>\n");
		}

		for( String filename : new TreeSet<>(Arrays.asList(commondir.list())) )
		{
			if( !filename.endsWith(".js") )
			{
				continue;
			}
			sb.append("<script src=\"./data/javascript").append("/common/").append(filename).append("\" type=\"text/javascript\"></script>\n");
		}
		if( new File(jsdir.getAbsolutePath()+"/modules/"+this.getAttribute("module")+".js").isFile() )
		{
			sb.append("<script src=\"./data/javascript").append("/modules/").append((String)this.getAttribute("module")).append(".js\" type=\"text/javascript\"></script>\n");
		}
	}

	@Override
	public void printFooter() throws IOException
	{
		Writer sb = getContext().getResponse().getWriter();
		if( this.getAttribute("pagemenu") != null ) {
			sb.append("<script type=\"text/javascript\">\n");
			sb.append("<!--\n");
			sb.append("if( parent && parent.setCurrentPage ) {\n");
			Object pagetitle = this.getAttribute("pagetitle");
			if( pagetitle == null ) {
				pagetitle = "null";
			}
			sb.append("parent.setCurrentPage('").append(this.getAttributeString("module")).append("','").append(pagetitle.toString()).append("');\n");
			PageMenuEntry[] entries = (PageMenuEntry[])this.getAttribute("pagemenu");
			if( (entries != null) && (entries.length > 0) ) {
				for (PageMenuEntry entry : entries)
				{
					sb.append("parent.addPageMenuEntry('").append(entry.title).append("','").append(entry.url.replace("&amp;", "&")).append("');");
				}
			}
			sb.append("parent.completePage();");
			sb.append("}\n");
			sb.append("// -->\n");
			sb.append("</script>\n");
		}
		sb.append("</body>");
		sb.append("</html>");
	}

	@Override
	public void printErrorList() throws IOException {
		Writer sb = getContext().getResponse().getWriter();
		sb.append("<div id=\"error-box\" align=\"center\">\n");
		sb.append("<div class='gfxbox' style='width:470px'>");
		sb.append("<div style=\"text-align:center; font-size:14px; font-weight:bold\">Es sind Fehler aufgetreten:</div><ul>\n");

		for( net.driftingsouls.ds2.server.framework.pipeline.Error error : getContext().getErrorList() ) {
			if( error.getUrl() == null ) {
				sb.append("<li><span style=\"font-size:14px; color:red\">").append(error.getDescription().replaceAll("\n", "<br />")).append("</span></li>\n");
			}
			else {
				sb.append("<li><a class=\"error\" style=\"font-size:14px; font-weight:normal\" href=\"").append(error.getUrl()).append("\">").append(error.getDescription().replaceAll("\n", "<br />")).append("</a></li>\n");
			}
		}

		sb.append("</ul>\n");
		sb.append("</div>");
		sb.append("</div>\n");
		sb.append("<script type=\"text/javascript\">\n");
		sb.append("var error = document.getElementById('error-box');\n");
		sb.append("var errorMarker = document.getElementById('error-placeholder');\n");
		sb.append("error.parentNode.removeChild(error);\n");
		sb.append("errorMarker.appendChild(error);\n");
		sb.append("</script>");
	}
}
