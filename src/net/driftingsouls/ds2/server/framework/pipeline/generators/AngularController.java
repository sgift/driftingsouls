package net.driftingsouls.ds2.server.framework.pipeline.generators;

import net.driftingsouls.ds2.server.framework.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

/**
 * Generator fuer auf AngularJS aufbauende DS-Module.
 * @author Christopher Jung
 *
 */
public abstract class AngularController extends Controller
{	
	public AngularController()
	{
		super();
		
		this.addBodyParameter("ng-app", "ds.application");
		this.setDisableDebugOutput(true);
	}

	@Override
	@Action(ActionType.DEFAULT)
	public final void defaultAction() throws IOException
	{
		Writer echo = getResponse().getWriter();

		File tmpl = new File(Configuration.getAbsolutePath()+"data/cltemplates/ds.html");
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
