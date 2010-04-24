package net.driftingsouls.ds2.server.framework.db;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;

/**
 * Eine einfache Erweiterung der Hibernate Konfiguration, die mehrere in DS notwendige SQL-Funktionen ergänzt.
 * 
 * @author Sebastian Gift
 */
public class DSAnnotationConfiguration extends AnnotationConfiguration 
{
	public DSAnnotationConfiguration()
	{
		super();
		addSqlFunction("pow", new StandardSQLFunction("pow", Hibernate.DOUBLE));
		addSqlFunction("floor", new StandardSQLFunction("floor", Hibernate.LONG));
		addSqlFunction("ncp", new NullCompFunction());
		addSqlFunction("bit_and", new SQLFunctionTemplate(Hibernate.INTEGER, "?1 & ?2"));
		addSqlFunction("bit_or", new SQLFunctionTemplate(Hibernate.INTEGER, "?1 | ?2"));
	}
	
	private static final long serialVersionUID = 6679197865208602824L;
}
