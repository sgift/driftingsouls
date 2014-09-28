package net.driftingsouls.ds2.server.framework.templates;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.util.ArrayList;
import java.util.List;

public class MemJavaFileManager extends
		ForwardingJavaFileManager<StandardJavaFileManager>
{
	private final List<MemJavaFileObject> files = new ArrayList<>();

	public MemJavaFileManager(JavaCompiler compiler)
	{
		super( compiler.getStandardFileManager( null, null, null ) );
	}

	@Override
	public JavaFileObject getJavaFileForOutput( Location location,
												String className,
												JavaFileObject.Kind kind,
												FileObject sibling )
	{
		MemJavaFileObject fileObject = new MemJavaFileObject( className );
		files.add( fileObject );
		return fileObject;
	}

	public List<MemJavaFileObject> getFiles() {
		return files;
	}
}
