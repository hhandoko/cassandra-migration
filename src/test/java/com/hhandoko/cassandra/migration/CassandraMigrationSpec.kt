package com.hhandoko.cassandra.migration

import com.hhandoko.cassandra.migration.api.resolver.MigrationResolver
import com.hhandoko.cassandra.migration.api.resolver.ResolvedMigration
import com.hhandoko.cassandra.migration.internal.resolver.CompositeMigrationResolver
import com.hhandoko.cassandra.migration.internal.resolver.ResolvedMigrationImpl
import com.hhandoko.cassandra.migration.internal.resolver.cql.CqlMigrationResolver
import com.hhandoko.cassandra.migration.internal.resolver.java.JavaMigrationResolver
import io.kotlintest.matchers.be
import io.kotlintest.specs.FreeSpec

class CassandraMigrationSpec : FreeSpec() {


    init {
        "Cassandra migration" - {

            "resolvers" - {
                val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                val cm = CassandraMigration()
                cm.locations = scriptsLocations

                "defaults to cql and java" {
                    val resolver = cm.createMigrationResolver()

                    // should be a composite resolver
                    resolver should be an CompositeMigrationResolver::class

                    // 1 each for the defaults
                    val resolvers = (resolver as CompositeMigrationResolver).migrationResolvers
                    resolvers.size shouldBe 2
                    resolvers[0] should be an CqlMigrationResolver::class
                    resolvers[1] should be an JavaMigrationResolver::class
                }

                "can be added" {

                    cm.resolvers = arrayOf(object : MigrationResolver {
                        override fun resolveMigrations(): List<ResolvedMigration> {
                            val resolvedMigration = ResolvedMigrationImpl()
                            resolvedMigration.description = "custom"
                            return arrayListOf(resolvedMigration).toList()
                        }
                    });

                    val resolver = cm.createMigrationResolver()

                    // should be a composite resolver
                    resolver should be an CompositeMigrationResolver::class

                    // 1 each for the defaults and 1 for our custom
                    val resolvers = (resolver as CompositeMigrationResolver).migrationResolvers
                    resolvers.size shouldBe 3

                    // should be the last in the list
                    val resolvedMigration = resolvers.last().resolveMigrations().first()
                    resolvedMigration.description shouldBe "custom"
                }

                "can be replaced" {
                    cm.isSkipDefaultResolvers = true
                    cm.resolvers = arrayOf(object : MigrationResolver {
                        override fun resolveMigrations(): List<ResolvedMigration> {
                            val resolvedMigration = ResolvedMigrationImpl()
                            resolvedMigration.description = "custom"
                            return arrayListOf(resolvedMigration).toList()
                        }
                    });

                    val resolver = cm.createMigrationResolver()

                    // should be a composite resolver
                    resolver should be an CompositeMigrationResolver::class

                    // just 1 for our custom
                    val resolvers = (resolver as CompositeMigrationResolver).migrationResolvers
                    resolvers.size shouldBe 1

                    // should be the last in the list
                    val resolvedMigration = resolvers[0].resolveMigrations().first()
                    resolvedMigration.description shouldBe "custom"
                }
            }
        }
    }
}