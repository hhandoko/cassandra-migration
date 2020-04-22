package com.hhandoko.cassandra.migration.internal.resolver

import com.hhandoko.cassandra.migration.api.resolver.MigrationResolver
import com.hhandoko.cassandra.migration.internal.resolver.cql.CqlMigrationResolver
import com.hhandoko.cassandra.migration.internal.resolver.java.JavaMigrationResolver
import com.hhandoko.cassandra.migration.internal.util.Locations
import java.util.*

/**
 * Helper for creating default resolvers
 */
object MigrationResolverFactory {

    /**
     * Creates the default list of resolvers (Cql and Java) for each of the locations
     *
     * @param classLoader The ClassLoader for loading migrations on the classpath.
     * @param encoding The encoding of the .cql file(s).
     * @param timeout The read script timeout duration in seconds.
     * @param locations The Locations to create the default resolvers for.
     * @return An array of default migration resolvers
     */
    fun createDefaultResolvers(
            classLoader: ClassLoader,
            encoding: String,
            timeout: Int,
            locations: Locations): Array<MigrationResolver> {
        val migrationResolvers = ArrayList<MigrationResolver>(locations.getLocations().size * 2)
        locations.getLocations().forEach {
            migrationResolvers.add(CqlMigrationResolver(classLoader, it, encoding, timeout))
            migrationResolvers.add(JavaMigrationResolver(classLoader, it))
        }
        return migrationResolvers.toTypedArray()
    }
}