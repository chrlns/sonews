[![Build Status](https://travis-ci.org/cli/sonews.svg?branch=master)](https://travis-ci.org/cli/sonews)
[![Coverty Scan](https://scan.coverity.com/projects/2030/badge.svg)](https://scan.coverity.com/projects/2030)
[![Gitter chat](https://badges.gitter.im/dscape/sonews.png)](https://gitter.im/cli/sonews)

sonews
======

**sonews** is an Usenet News Server written in Java. It can use various 
backend types, currently supported are CouchDB (very experimental), MySQL and PostgreSQL.

Requirements
------------

The requirements for building and running sonews are:

* Apache Maven
* Java 7 JDK (or newer)
* CouchDB or MySQL/PostgreSQL installation

Build
-----

sonews uses Apache Maven for building and dependency managing.
Use the following command to build and package sonews:

    $ mvn clean compile package


To start sonews on port 9119:

    $ mvn exec:java -Dexec.mainClass="org.sonews.Main" -Dexec.args="-p 9119"


Setup
-----

* Create a database in your DBMS, e.g. named like 'sonews'
* (MySQL/PostgreSQL only) Create the necessary table structure using the 
  helpers/*.sql file (you may use the experimental helper application:
   java -cp sonews.jar:<jdbcdriver.jar> DatabaseSetup )
* Customize the settings within the sonews.conf file.
* Invoke 'helpers/sonews start' to start the daemon.

Bugs and other Issues
----------------------

Please mail them to mail(at)sonews.org or better issue them
into the bugtracker at https://github.com/cli/sonews/ .
