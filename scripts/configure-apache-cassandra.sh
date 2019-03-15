#!/bin/bash

###
# File     : configure_apache-cassandra.sh
# License  :
#   Copyright (c) 2016 - 2018 cassandra-migration Contributors
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#           http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
###

set -euo pipefail

export CASS_VER="3.11.4"

sudo rm -rf /var/lib/cassandra/*
wget --content-disposition "https://www.apache.org/dyn/closer.lua?action=download&filename=/cassandra/${CASS_VER}/apache-cassandra-${CASS_VER}-bin.tar.gz"
wget "http://www.apache.org/dist/cassandra/${CASS_VER}/apache-cassandra-${CASS_VER}-bin.tar.gz.sha256"
sha256sum "apache-cassandra-${CASS_VER}-bin.tar.gz"
tar -xvzf "apache-cassandra-${CASS_VER}-bin.tar.gz"
sudo sh "apache-cassandra-${CASS_VER}/bin/cassandra" -R
