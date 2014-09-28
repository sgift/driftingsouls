package net.driftingsouls.ds2.server.framework.templates;

import java.io.IOException;

@FunctionalInterface
public interface TemplateCompilerOutputHandler
{
	public void handle(MemJavaFileObject javaFile) throws IOException;
}
