# -*- mode: ruby -*-
# vi: set ft=ruby :

###
# File     : Vagrantfile
# License  :
#   Original   - Copyright (c) 2016 Brian Cantoni
#   Derivative - Copyright (c) 2016 Citadel Technology Solutions Pte Ltd
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
#
# Notes:
#   - Original found at: https://github.com/bcantoni/vagrant-cassandra
###

# -----------------------------------------------------------------------------
# Configuration
# -----------------------------------------------------------------------------
# Guest VM(s) cluster configuration
# ~~~~~~
CFG_MEMSIZE  = ENV['CASS_MEM']  || "2048"            # Configured memory for each guest VM
CFG_CPUCOUNT = ENV['CASS_CPU']  || "2"               # Configured CPU core for each guest VM
CFG_IP       = ENV['CASS_IP']   || "192.168.10.10"   # Configured VM's IP address
CFG_DIST     = ENV['CASS_DIST'] || "dsc30"           # Cassandra distribution type (e.g. DataStax Enterprise or Apache Cassandra)
CFG_TZ       = "UTC"                                 # Configured timezone, e.g.  US/Pacific, US/Eastern, UTC, Europe/Warsaw, etc.

# Apache Cassandra release manager's GPG public key
# ~~~~~~
# Use the following known keys corresponding to the version you choose:
#   - 37x -> 749D6EEC0353B12C
#   - 38x -> A278B781FE4B2BDA
#   - 39x -> A278B781FE4B2BDA
CFG_GPG      = ENV['CASS_GPG']  || "A278B781FE4B2BDA"

# VM host configuration
# ~~~~~~
VAGRANTFILE_API_VERSION = "2"


# -----------------------------------------------------------------------------
# Provisioning Scripts
# -----------------------------------------------------------------------------
# Debian proxy client configuration
# ~~~~~~
# Note: Only if 'DEB_CACHE_HOST' is defined
deb_cache_cmds = ""
if ENV['DEB_CACHE_HOST']
  deb_cache_host = ENV['DEB_CACHE_HOST']
  deb_cache_cmds = <<CACHE
apt-get install squid-deb-proxy-client -y
echo 'Acquire::http::Proxy "#{deb_cache_host}";' | sudo tee /etc/apt/apt.conf.d/30autoproxy
echo "Acquire::http::Proxy { debian.datastax.com DIRECT; };" | sudo tee -a /etc/apt/apt.conf.d/30autoproxy
echo "Acquire::http::Proxy { ppa.launchpad.net DIRECT; };" | sudo tee -a /etc/apt/apt.conf.d/30autoproxy
cat /etc/apt/apt.conf.d/30autoproxy
CACHE
end

# OpenJDK provisioning script
# ~~~~~~
node_script = <<SCRIPT
#!/bin/bash

# Set timezone
echo "#{CFG_TZ}" > /etc/timezone
dpkg-reconfigure -f noninteractive tzdata

#{deb_cache_cmds}

# Install base packages
echo "Task: Installing base packages"
apt-get -qq update
apt-get -qqy install vim curl zip unzip git python-pip

# Install Java
# ~~~~~
# Note: Choose either OpenJDK or Azul Zulu
echo "Task: Installing Java"

## Add OpenJDK PPA repository, and
## install Java (comment / uncomment desired version)
#add-apt-repository ppa:openjdk-r/ppa
#apt-get -qq update
##apt-get -qqy install openjdk-7-jdk
#apt-get -qqy install openjdk-8-jdk

# Add Azul Zulu repository, and
# install Java (comment / uncomment desired version)
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0x219BD9C9
echo "deb http://repos.azulsystems.com/ubuntu stable main" >> /etc/apt/sources.list.d/zulu.list
apt-get -qq update
#apt-get -qqy install zulu-7=7.14.0.5
apt-get -qqy install zulu-8=8.15.0.1

echo "Info: Base VM provisioning complete"
SCRIPT

# DataStax Enterprise Community Edition installation
# ~~~~~~
dsec_install_script = <<SCRIPT
#!/bin/bash

# Add DataStax repository (incl. repository key)
echo "Task: Adding DataStax repository"
echo "deb http://debian.datastax.com/community stable main" >> /etc/apt/sources.list.d/cassandra.sources.list
curl -L https://debian.datastax.com/debian/repo_key | sudo apt-key add -
apt-get -qq update

# Install DataStax Enterprise Community Edition Cassandra distribution (incl. optional utilities)
echo "Task: Installing DataStax Enterprise Community Edition"
apt-get -qqy install #{CFG_DIST}
apt-get -qqy install cassandra-tools

echo "Info: DataStax Enterprise Community Edition provisioning complete"
SCRIPT

# Apache Cassandra installation
# ~~~~~~
cass_install_script = <<SCRIPT
#!/bin/bash

# Add Apache Cassandra repository (incl. repository key)
echo "Task: Adding Apache Cassandra repository"
echo "deb http://www.apache.org/dist/cassandra/debian #{CFG_DIST} main" >> /etc/apt/sources.list.d/cassandra.list
gpg --keyserver pgp.mit.edu --recv-keys #{CFG_GPG}
gpg --export --armor #{CFG_GPG} | sudo apt-key add -
apt-get -qq update

# Install Apache Cassandra distribution
echo "Task: Installing Apache Cassandra"
apt-get -qqy install cassandra

echo "Info: Apache Cassandra provisioning complete"
SCRIPT

# Cassandra configuration
# ~~~~~~
cass_configure_script = <<SCRIPT
echo "Task: Configuring Cassandra"
service cassandra stop
rm -rf /var/lib/cassandra/data/system/*
sudo sed -i "s|cluster_name: 'Test Cluster'|cluster_name: 'Cassandra Migration'|" /etc/cassandra/cassandra.yaml
sudo sed -i 's|seeds: "127.0.0.1"|seeds: "#{CFG_IP}"|' /etc/cassandra/cassandra.yaml
sudo sed -i "s|listen_address: localhost|listen_address: #{CFG_IP}|" /etc/cassandra/cassandra.yaml
sudo sed -i "s|start_rpc: false|start_rpc: true|" /etc/cassandra/cassandra.yaml
sudo sed -i "s|rpc_address: localhost|rpc_address: #{CFG_IP}|" /etc/cassandra/cassandra.yaml
service cassandra start

echo "Info: Cassandra configuration complete"
SCRIPT


# -----------------------------------------------------------------------------
# Vagrant Script
# -----------------------------------------------------------------------------
# VM provisioning
# ~~~~~~
Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  # Use `vagrant-cachier` to cache common packages and reduce time to provision boxes
  if Vagrant.has_plugin?("vagrant-cachier")
    # Configure cached packages to be shared between instances of the same base box.
    # More info on http://fgrehm.viewdocs.io/vagrant-cachier/usage
    config.cache.scope = :box
  end

  config.vm.define :cassandra do |node|
    # Use Ubuntu 14.04 LTS (Trusty Tahr) image from Hashicorp repository
    node.vm.box = "ubuntu/trusty64"

    # VMware Fusion host-specific configuration
    node.vm.provider "vmware_fusion" do |vm|
      vm.vmx["memsize"] = CFG_MEMSIZE
      vm.vmx["numvcpus"] = CFG_CPUCOUNT
    end

    # VirtualBox host-specific configuration
    node.vm.provider :virtualbox do |vm|
      vm.name = "cassandra"
      vm.customize ["modifyvm", :id, "--memory", CFG_MEMSIZE]
      vm.customize ["modifyvm", :id, "--cpus"  , CFG_CPUCOUNT]
    end

    # Configure guest VM IP address
    node.vm.network :private_network, ip: CFG_IP

    node.vm.hostname = "cassandra"
    node.vm.provision :shell, :inline => node_script

    if CFG_DIST.start_with?('dsc')
      # Provision DataStax Enterprise Community Edition
      node.vm.provision :shell, :inline => dsec_install_script
    else
      # Provision Apache Cassandra
      node.vm.provision :shell, :inline => cass_install_script
    end

    node.vm.provision :shell, :inline => cass_configure_script
  end
end
