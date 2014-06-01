package net.driftingsouls.ds2.server.framework.pipeline.generators;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Error;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

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

	@ViewModel
	public static class ErrorResult
	{
		public static class MessageViewModel
		{
			public String type;
		}

		public static class ErrorViewModel
		{
			public String description;
			public String url;
		}

		public MessageViewModel message;
		public List<ErrorViewModel> errors = new ArrayList<>();
	}

	@Override
	protected void printErrorListOnly(ActionType type) throws IOException
	{
		ErrorResult result = new ErrorResult();
		for( Error error : this.getErrorList() )
		{
			ErrorResult.ErrorViewModel errorObj = new ErrorResult.ErrorViewModel();
			errorObj.description = error.getDescription();
			errorObj.url = error.getUrl();
			result.errors.add(errorObj);
		}

		result.message = new ErrorResult.MessageViewModel();
		result.message.type = "errorlist";
		
		getResponse().getWriter().write(new Gson().toJson(result));
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
