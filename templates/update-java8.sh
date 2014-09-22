#!/bin/bash

rpm -Uvh /opt/jdk.rpm

alternatives --install /usr/bin/java java /usr/java/jdk1.8.0_20/jre/bin/java 20000
alternatives --install /usr/bin/jar jar /usr/java/jdk1.8.0_20/bin/jar 20000
alternatives --install /usr/bin/javac javac /usr/java/jdk1.8.0_20/bin/javac 20000
alternatives --install /usr/bin/javaws javaws /usr/java/jdk1.8.0_20/jre/bin/javaws 20000
alternatives --set java /usr/java/jdk1.8.0_20/jre/bin/java
alternatives --set javaws /usr/java/jdk1.8.0_20/jre/bin/javaws
alternatives --set javac /usr/java/jdk1.8.0_20/bin/javac
alternatives --set jar /usr/java/jdk1.8.0_20/bin/jar
