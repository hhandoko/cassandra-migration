package migration.integ.java;


import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.hhandoko.cassandra.migration.api.migration.java.JavaMigration;

public class V3_0_1__Three_zero_one implements JavaMigration {
    @Override
    public void migrate(CqlSession session) throws Exception {

        session.execute(QueryBuilder.insertInto("test1")
                .value("space",QueryBuilder.literal("web"))
                .value("key",QueryBuilder.literal("google"))
                .value("value",QueryBuilder.literal( "facebook.com")).build());

//        Insert insert = QueryBuilder.insertInto("test1");
//        insert.value("space", "web");
//        insert.value("key", "facebook");
//        insert.value("value", "facebook.com");

//        session.execute(insert);
    }
}
