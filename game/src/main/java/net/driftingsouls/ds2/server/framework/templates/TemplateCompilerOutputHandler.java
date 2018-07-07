package net.driftingsouls.ds2.server.framework.templates;

import java.io.IOException;

@FunctionalInterface
public interface TemplateCompilerOutputHandler
{
	void handle(MemJavaFileObject javaFile) throws IOException;
}
