#!/bin/bash

#check the newest version here: http://www.versioneye.com/package/com~datomic--datomic-free
VERSION="0.8.3889"
DEST_PATH=$HOME
JAVA_PATH=/Library/Java/JavaVirtualMachines/jdk1.7.0_13.jdk/Contents/Home
$UBUNTU_JAVA_PATH=/usr/lib/jvm/java-7-oracle

function ubuntu_install_java(){
	sudo apt-get install software-properties-common -y --force-yes
	sudo add-apt-repository ppa:webupd8team/java
	sudo apt-get update
	sudo apt-get install oracle-java7-installer -y --force-yes
}

function setup_datomic(){
	echo "Going to download datomic"
	CWD=`pwd`
	mkdir ~/temp1010 && cd ~/temp1010
	wget -O datomic-free.zip "http://downloads.datomic.com/$VERSION/datomic-free-$VERSION.zip"
	unzip datomic-free.zip -d $DEST_PATH
	#create folder for datomic
	rm -r ~/temp1010

	echo "Initializing environment variables"
	export JAVA_HOME=$JAVA_PATH
	export PATH=$JAVA_HOME/bin:$PATH

	cd $CWD
	echo "Datomic-free is deployed to directory $HOME/datomic-free-$VERSION"
}

setup_datomic