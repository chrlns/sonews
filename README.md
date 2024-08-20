[![Coverty Scan](https://scan.coverity.com/projects/2030/badge.svg)](https://scan.coverity.com/projects/2030)

sonews
======

**sonews** is an Usenet News Server written in Java. It can use various 
backend types, currently supported are MySQL and PostgreSQL (CouchDB in development).

Requirements
------------

The requirements for building and running sonews are:

* Apache Maven
* Java 17 for sonews/2.1 or higher
* MySQL/PostgreSQL installation (CouchDB possible for sonews/2.1 or higher)

Note: Hibernate OGM required for CouchDB support is currently unmaintained and 
should not be used.

Build
-----

Checkout the sources of the latest stable release using Git:

    $ git clone https://github.com/cli/sonews.git
    $ git checkout tags/sonews-2.0.0

sonews uses Apache Maven for building and dependency managing.
Use the following command to build and package sonews:

    $ mvn clean compile package

If you get an error message such as

    Fatal error compiling: error: invalid target release: 17

then make sure that the default JDK of your machine is at least Java 17.

To start sonews/2.0 on port 9119:

    $ mvn exec:java -Dexec.mainClass="org.sonews.Main" -Dexec.args="-p 9119"

For sonews/2.1 or later use:

    $ mvn exec:java -pl sonews -Dexec.mainClass="org.sonews.Application" -Dexec.args="-p 9119"

You may want sonews to listen on the default NNTP port (119) without running as
root user. This can be achieved by redirecting all TCP connections on port 119
to a higher port where sonews can listen as unprivileged user:

 	# iptables -t nat -A PREROUTING -p tcp --dport 119 -j REDIRECT --to-port 9119

Setup
-----

* Create a database in your database system, e.g. named like 'sonews' and give it a
  dedicated database user
* Create the necessary table structure using the util/*.sql file
* Customize the settings within the sonews.conf file (you'll find a template in util/
  or let sonews create one on first startup)
* Start sonews as described above

Contribution
-------------

sonews is Free Software licensed under the terms of the GPLv3.

Please report any issues at https://github.com/chrlns/sonews/ or write a mail to
mail(at)sonews.org. 
