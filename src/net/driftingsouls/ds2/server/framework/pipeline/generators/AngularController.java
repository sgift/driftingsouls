package net.driftingsouls.ds2.server.framework.pipeline.generators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;

/**
 * Generator fuer auf AngularJS aufbauende DS-Module.
 * @author Christopher Jung
 *
 */
public abstract class AngularController extends Controller
{	
	public AngularController(Context context)
	{
		super(context);
		
		this.addBodyParameter("ng-app", "ds.application");
		this.setDisableDebugOutput(true);
	}
	
	@Override
	protected void printErrorListOnly(ActionType type) throws IOException
	{
		JsonObject result = new JsonObject();
		JsonArray errorListObj = new JsonArray();
		for( Error error : this.getErrorList() )
		{
			JsonObject errorObj = new JsonObject();
			errorObj.addProperty("description", error.getDescription());
			errorObj.addProperty("url", error.getUrl());
			errorListObj.add(errorObj);
		}
		result.add("errors", errorListObj);
		JsonObject msg = new JsonObject();
		msg.addProperty("type", "errorlist");
		result.add("message", msg);
		
		getResponse().getWriter().write(result.toString());
	}

	@Override
	@Action(ActionType.DEFAULT)
	public final void defaultAction() throws IOException
	{
		Writer echo = getResponse().getWriter();

		File tmpl = new File(Configuration.getSetting("ABSOLUTE_PATH")+"data/cltemplates/ds.html");
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(tmpl), "UTF-8")))
		{
			char[] buffer = new char[8192];
			int cnt;
			while ((cnt = reader.read(buffer)) != -1)
			{
				echo.write(buffer, 0, cnt);
			}
		}
	}
}
