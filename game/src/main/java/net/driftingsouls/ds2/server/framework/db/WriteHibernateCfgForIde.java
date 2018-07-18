package net.driftingsouls.ds2.server.framework.db;

import net.driftingsouls.ds2.server.framework.Configuration;
import org.hibernate.mapping.PersistentClass;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Schreibt eine vollstaendige Hibernate-Config fuer IDEs wie Intellij. DS braucht keine solche
 * Datei, da es z.B. die Entities automatisch findet.
 */
public class WriteHibernateCfgForIde
{
	public static void main(String[] args) throws Exception
	{
		if( args.length != 2 ) {
			System.err.println("WriteHibernateCfgForIde <configdir> <output-file>");
			return;
		}
		Configuration.init(args[0]);

		String dbUrl = Configuration.getDbUrl();
		String dbUser = Configuration.getDbUser();
		String dbPassword = Configuration.getDbPassword();

		HibernateUtil.initConfiguration(Configuration.getConfigPath()+"hibernate.xml", dbUrl, dbUser, dbPassword);
		HibernateUtil.createFactories();

		try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), StandardCharsets.UTF_8)))
		{
			writer.write("<?xml version='1.0' encoding='utf-8'?>\n" +
					"<!DOCTYPE hibernate-configuration PUBLIC\n" +
					"\t\t\"-//hibernate/Hibernate Configuration DTD//EN\"\n" +
					"\t\t\"http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd\">\n" +
					"<hibernate-configuration>\n" +
					"\t<session-factory>\n");

			writer.write("<property name=\"connection.url\">"+dbUrl+"</property>\n" +
					"\t\t<property name=\"connection.driver_class\">com.mysql.jdbc.Driver</property>\n" +
					"\t\t<property name=\"connection.username\">"+dbUrl+"</property>\n" +
					"\t\t<property name=\"connection.password\">"+dbPassword+"</property>\n" +
					"\t\t<property name=\"hibernate.cache.region.factory_class\">org.hibernate.cache.ehcache.EhCacheRegionFactory</property>\n");

			for(Iterator<PersistentClass> iterator = HibernateUtil.getConfiguration().getClassMappings(); iterator.hasNext(); )
			{
				PersistentClass pc = iterator.next();
				writer.write("\t\t<mapping class=\""+pc.getMappedClass().getCanonicalName()+"\" />\n");
			}

			writer.write("\t</session-factory>\n</hibernate-configuration>");
		}
	}
}
