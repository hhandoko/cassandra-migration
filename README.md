[![License](https://img.shields.io/badge/license-Apache--2.0-brightgreen.svg)](LICENSE)
[![Master Build Status](https://travis-ci.org/hhandoko/cassandra-migration.svg?branch=master)](https://travis-ci.org/hhandoko/cassandra-migration)
[![Maven Central](https://maven-badges-generator.herokuapp.com/image/com.hhandoko/cassandra-migration)](https://maven-badges-generator.herokuapp.com/dependency/com.hhandoko/cassandra-migration)
[![Downloads](https://img.shields.io/badge/downloads-jar-brightgreen.svg)](https://github.com/hhandoko/cassandra-migration/releases/download/cassandra-migration-0.14/cassandra-migration-0.14.jar)
[![Downloads](https://img.shields.io/badge/downloads-jar--with--dependencies-brightgreen.svg)](https://github.com/hhandoko/cassandra-migration/releases/download/cassandra-migration-0.14/cassandra-migration-0.14-jar-with-dependencies.jar)

### !!! IMPORTANT NOTICE !!!

Dear cassandra-migration users, I will be making some significant changes to this project over the next couple of months:

  - ~~The project repository will be moved under my own account (i.e. `hhandoko/cassandra-migration`)~~
  - ~~The project organisation namespace will be updated to `com.hhandoko`~~

I have joined a new company, but unfortunately there were no other developers in my previous company able to pick up the responsibility of maintaining this project. Rather than abandoning it, I will continue to develop and maintain it but in my personal capacity.

I apologise for the inconvenience and thank you for your understanding.

# Cassandra Migration

`cassandra-migration` is a simple and lightweight Apache Cassandra database schema migration tool.

It is a Kotlin fork of [Contrast Security's Cassandra Migration] project, which has been manually re-based to closely follow [Axel Fontaine / BoxFuse Flyway] project.
 
It is designed to work similar to Flyway, supporting plain CQL and Java-based migrations. Both CQL execution and Java migration interface uses [DataStax's Cassandra Java driver].

### Limitations

**The tool does not roll back the database upon migration failure.** You're expected to manually restore from backup.

## Resources

* [Project Overview](https://github.com/hhandoko/cassandra-migration/wiki)
* [Releases](https://github.com/hhandoko/cassandra-migration/releases)
* [Getting Started](https://github.com/hhandoko/cassandra-migration/wiki/Getting-Started)
* [Migrations](https://github.com/hhandoko/cassandra-migration/wiki/Migrations)
  * [Configuration / Options](https://github.com/hhandoko/cassandra-migration/wiki/Configuration-Options)
  * [CQL and Java migrations](https://github.com/hhandoko/cassandra-migration/wiki/Script-Types)
  * [Standalone migration](https://github.com/hhandoko/cassandra-migration/wiki/Standalone-Migration)
  * [Library API migration](https://github.com/hhandoko/cassandra-migration/wiki/API-Migration)

Refer to the [Project Wiki] for the full documentation.

## Contributing

We follow the "[fork-and-pull]" Git workflow.

1. Fork the repo on GitHub
1. Commit changes to a branch in your fork (use `snake_case` convention):
   * For technical chores, use `chore/` prefix followed by the short description, e.g. `chore/do_this_chore`
   * For new features, use `feature/` prefix followed by the feature name, e.g. `feature/feature_name`
   * For bug fixes, use `bug/` prefix followed by the short description, e.g. `bug/fix_this_bug`
1. Ensure relevant test(s) are added: for bugs, or if existing behaviour are changed or updated
1. Rebase or merge from "upstream"
1. Submit a PR "upstream" with your changes

Please read [CONTRIBUTING] for more details.

## License

```
  Copyright (c) 2016 - 2018 cassandra-migration Contributors

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

             http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
```

`cassandra-migration` project is released under the Apache 2 license. See the [LICENSE] file for further details.
 
[Contrast Security's Cassandra Migration] project is released under the Apache 2 license. See [Contrast Security Cassandra Migration project license page] for further details.

[Flyway] project is released under the Apache 2 license. See [Flyway's project license page] for further details.

[Axel Fontaine / BoxFuse Flyway]: https://github.com/flyway/flyway
[Contrast Security's Cassandra Migration]: https://github.com/Contrast-Security-OSS/cassandra-migration
[Contrast Security Cassandra Migration project license page]: https://github.com/Contrast-Security-OSS/cassandra-migration/blob/master/LICENSE
[CONTRIBUTING]: CONTRIBUTING.md
[DataStax's Cassandra Java driver]: http://datastax.github.io/java-driver/
[Flyway]: https://flywaydb.org/
[Flyway's project license page]: https://github.com/flyway/flyway/blob/master/LICENSE
[fork-and-pull]: https://help.github.com/articles/using-pull-requests
[LICENSE]: LICENSE
[Project Wiki]: https://github.com/hhandoko/cassandra-migration/wiki
