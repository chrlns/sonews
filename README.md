sonews README
=============

sonews is an Usenet News Server written in Java. It uses a relation database as
backend, currently supported is MySQL, PostgreSQL and HSQLDB.

Requirements
------------

The requirements for building and running sonews are:

* Apache Maven
* Java 6 JDK
* CouchDB or MySQL/PostgreSQL installation

Build
-----

sonews uses Apache Maven for building and dependency managing.
Use 
    $ mvn clean compile package
to build and package sonews.

Use
    $ mvn exec:java -Dexec.mainClass="org.sonews.Main" -Dexec.args="-p 9119"
to start the sonews daemon on port 9119.


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
