package net.driftingsouls.ds2.server.framework.db;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;

public class DBUtil {
    public static Connection getConnection(EntityManager em) throws SQLException {
        Session session = em.unwrap(Session.class);
        SessionFactoryImplementor sfi = (SessionFactoryImplementor) session.getSessionFactory();
        @SuppressWarnings("deprecation")
        ConnectionProvider cp = sfi.getConnectionProvider();
        HikariDataSource ds = cp.unwrap(HikariDataSource.class);

        return ds.getConnection();
    }

    public static DSLContext getDSLContext(Connection conn) throws SQLException {
        //Write sql without schema, e.g. SELECT * FROM ships, not SELECT * FROM ds.ships
        var settings = new Settings()
            .withRenderSchema(false);
        return DSL.using(conn, SQLDialect.MYSQL, settings).dsl();
    }
}
