/**
 * File     : CommandLineMigrationIT.java
 * License  :
 *   Original   - Copyright (c) 2015 - 2016 Contrast Security
 *   Derivative - Copyright (c) 2016 Citadel Technology Solutions Pte Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.builtamont.cassandra.migration;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CommandLineMigrationIT extends BaseIT {

    static boolean runCmdTestCompleted = false;
    static boolean runCmdTestSuccess = false;

    @Test
    public void migrations_should_run_succesfully_from_command_line() throws IOException, InterruptedException {
        String shell = "java -jar"
                + " -Dcassandra.migration.scripts.locations=filesystem:target/test-classes/migration/integ"
                + " -Dcassandra.migration.cluster.contactpoints=" + CASSANDRA_CONTACT_POINT
                + " -Dcassandra.migration.cluster.port=" + CASSANDRA_PORT
                + " -Dcassandra.migration.cluster.username=" + CASSANDRA_USERNAME
                + " -Dcassandra.migration.cluster.password=" + CASSANDRA_PASSWORD
                + " -Dcassandra.migration.keyspace.name=" + CASSANDRA_KEYSPACE
                + " target/*-jar-with-dependencies.jar" + " migrate";
        ProcessBuilder builder;
        if (isWindows()) {
            throw new IllegalStateException();
        } else {
            builder = new ProcessBuilder("bash", "-c", shell);
        }
        builder.redirectErrorStream(true);
        final Process process = builder.start();

        watch(process);

        while (!runCmdTestCompleted)
            Thread.sleep(1000L);

        assertThat(runCmdTestSuccess, is(true));
    }

    private static void watch(final Process process) {
        new Thread(new Runnable() {
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                try {
                    while ((line = input.readLine()) != null) {
                        if (line.contains("Successfully applied 4 migration(s)"))
                            runCmdTestSuccess = true;
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runCmdTestCompleted = true;
            }
        }).start();
    }

    private boolean isWindows() {
        return (System.getProperty("os.name").toLowerCase()).contains("win");
    }

}
