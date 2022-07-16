package net.driftingsouls.ds2.server.framework.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.driftingsouls.ds2.server.framework.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.SQLException;

public class DBUtil {
    private final static HikariDataSource ds;

    static {
        var config = new HikariConfig();
        config.setJdbcUrl(Configuration.getDbUrl());
        config.setUsername(Configuration.getDbUser());
        config.setPassword(Configuration.getDbPassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setAutoCommit(false);

        ds = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
            return ds.getConnection();
    }

    public static DSLContext getDSLContext(Connection conn) {
        return DSL.using(conn, SQLDialect.MYSQL).dsl();
    }
}
