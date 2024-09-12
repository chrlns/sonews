sonews Usenet News Server
=========================

**sonews** is a NNTP server that can provide access to both local and global
Usenets newsgroups. It is written in Java and has modular backend API.

Introduction
------------

sonews aims to be a RCF3977 compliant NNTP Usenet server. It is written in 
modern Java and uses a database management system as backend (currently 
`PostgreSQL <http://www.postgresql.com/>`_ and `MySQL <http://www.mysql.com/>`_, 
CouchDB is in development). sonews is highly multithreaded and uses Java 
Virtual Threads to handle thousands of concurrent connections with ease.

sonews is Free and Open Source Software (FOSS) licensed under the terms of the
`GNU General Public License <http://www.gnu.org/licenses/gpl.html>`_ Version 3 (or later).

Ancestor of sonews is probably the Neat NNTP Daemon (n3tpd) although there is 
very little code in sonews that can be identified as direct derivation. 
sonews started as diploma thesis project of Christian Lins at 
`StarOffice development <http://de.sun.com/>`_ in Hamburg and is a Free 
Software project since.

Installation and initial setup
------------------------------

Download & Installation
~~~~~~~~~~~~~~~~~~~~~~~

Build from source
^^^^^^^^^^^^^^^^^

See `sonews.org <http://www.sonews.org/>`_ for recent binary and source tarballs. 
You may also checkout a recent version from `Github <https://github.com/chrlns/sonews.git>`_

Use the archive and extract it in a directory of your choice. Or use the 
checked-out source of course. Make sure your system provides the necessary 
prerequisites:

- Java 21 JDK (or higher)
- Apache Maven 3 (or higher)

Maven will download all necessary dependencies from a repository automatically. 
Use the following command to build and package sonews:

.. code-block:: bash

   $ mvn package

You'll find the resulting sonews-boot-2.1-SNAPSHOT-jar-with-dependencies.jar 
file in the sonews/target/ directory. The archive contains all necessary binaries.

Initial database setup
~~~~~~~~~~~~~~~~~~~~~~

Before you start sonews, you must prepare the database. Currently sonews is known to work with PostgreSQL and MySQL.

It is highly recommended to create an own database for every sonews instance, e.g. called 'sonews'. Additionally, it is recommended to create a unique database user for sonews, e.g. 'sonewsuser'. Please do not use the root user for sonews! The sonews user needs rights for SELECT, INSERT and UPDATE statements. Refer to the database manual for instructions.

You will find the SQL Schema definitions (database_*.sql) in the util/ subdirectory of the source and binary distributions.

Use these templates and a database tool (e.g. phpMyAdmin, pgAdmin, etc.) to create the necessary table structures.

Make sure you fill in the correct database settings in the sonews.conf file (see next chapter).

Running sonews
==============

Configuration values
--------------------

There is a bootstrap configuration in /etc/sonews/sonews.conf and a regular configuration in the database table config.

There are various configuration values that can be adapted:

'sonews.article.maxsize'
    Maximum allowed body size of a news message given in kilobytes. Please note that for MySQL the 'max_allowed_packet' configuration variable must be set to a value higher than 'sonews.article.maxsize' otherwise posting of large mails will fail.

'sonews.hostname'
    Canonical name of the server instance. This variable is part of the server's hello message to the client and used to generate Message-Ids. It is highly recommended to set sonews.hostname to the full qualified domain name (FQDN) of the host machine.

'sonews.loglevel'
    Specifies the minimum log level of messages sonews is logging to the logfile. Default: INFO. Can be one of the following values: ALL, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, OFF (disables logging)

'sonews.port'
    Listening port of sonews daemon. This value can be overridden with the -p command line argument.

'sonews.timeout'
    Socket timeout for client connections in seconds. Default as recommended in RFC3977 is 180 seconds.

'sonews.storage.database'
    Database connect string in the form: protocol:subprotocol:protocolspecific

    Example for PostgreSQL database sonews running on localhost: jdbc:postgresql:sonews

'sonews.storage.user'
    Database user name

'sonews.storage.password'
    Database user password

Command line arguments
----------------------

If you like to start sonews directly, you can use one of the following arguments:

.. code-block:: bash

   java -jar sonews.jar [arguments]
       where arguments:
   -c|-config         <path to config file> if custom config file preferred
   -dumpjdbcdriver    Prints out a list of available JDBC drivers
   -feed              Enables feed daemon for pulling news from peer servers
   -h|-help           This output
   -p portnumber      Port on which sonews is listening for incoming connections.
                      Overrides port settings in config file and database.

The source and binary distributions contain a ``util/sonews`` script that makes it more comfortable (well it's not comfortable at all) to start and stop sonews daemon.

You can start sonews directly from the source tree using Maven:

.. code-block:: bash

   $ mvn exec:java -Dexec.mainClass="org.sonews.Main" -Dexec.args="-p 9119"

Newsgroup configuration
-----------------------

The newsgroups are configured in the groups.conf configuration file. A sample file is provided as groups.conf.sample:

.. code-block:: text

   # Groupname   ID   Flags
   control       1    0
   local.test    2    0

The file contains one group per file. At first the name, then the internal ID which must be unique within the sonews instance and the flags number which is currently not used and should be always 0.

Peering
-------

sonews is able to synchronize selected groups with other newsservers using push or pull mechanisms. To enable the peering feature sonews must be started with the ``-feed`` command-line argument.

The peering mechanism is configured in the peers.conf file. A sample is provided in peers.conf.sample:

.. code-block:: text

   # Configuration file for Usenet peering
   # Format:
   # [PUSH|PULL] GROUPNAME HOST
   PULL news.software.servers.sonews news.sonews.org
   PUSH news.software.servers.sonews news.sonews.org

To peer with a remote server in both directions you need both a PUSH and a PULL entry. The push feeder is used every time a news message is posted to the local sonews instance. The news message is then immediately pushed to the remote host. The pull feeder checks the remote host in intervals for new messages and retrieves them.

Development
===========

You're welcome to create patches with bugfixes or additional features.

Some debugging hints: if the server blocks and does not longer respond you probably found a deadlock. Do not kill the process with "kill -9 <pid>" but send a SIGQUIT signal with "kill -3 <pid>" and the Java VM will output a stracktrace of all threads. This output is the most valuable information to fix the deadlock.

Writing extensions
------------------

With sonews/1.1 or higher it is possible to easily extend sonews with new functionality using the plugin API.

Command plugin
~~~~~~~~~~~~~~

To introduce additional NNTP commands, implement the `org.sonews.command.Command <apidoc/org/sonews/daemon/command/Command.html>`_ interface. Here is an example ``HelloCommand`` that simply returns "Hello" to the client:

.. code-block:: java

   public class HelloCommand implements Command
   {
     
     @Override
     public String[] getSupportedCommandStrings()
     {
       return "HELLO";
     }

     @Override
     public boolean hasFinished()
     {
       return true;
     }

     @Override
     public String impliedCapability()
     {
       return null;
     }

     @Override
     public boolean isStateful()
     {
       return false;
     }

     @Override
     public void processLine(NNTPConnection conn, final String line, byte[] raw)
       throws IOException
     {
       conn.println("100 Hello Client");
     }
     
   }

Compile this example against sonews.jar and tell sonews to load the plugin at startup:

.. code-block:: bash

   java -cp .:sonews.jar org.sonews.Main -p 9119 -plugin-command mypkg.HelloCommand

Then you can try the new command:

.. code-block:: text

   $ telnet localhost 9119
   200 sonews/1.1.0 localhost - posting ok
   hello
   100 Hello Client

The `API documentation <http://news.sonews.org/apidoc/>`_ contains more information about the sonews classes and their usage.

Backend storage plugin
~~~~~~~~~~~~~~~~~~~~~~

It is possible to use a completely different backend storage for sonews than a relational database. TODO: This feature is not completely available in sonews/2.0

Most important classes reside in package ``org.sonews.storage``. To use a custom storage backend in sonews you must implement a ``StorageProvider`` by implementing the `org.sonews.storage.StorageProvider <apidoc/org/sonews/storage/StorageProvider.html>`_ interface.

The StorageProvider must return an instance of the specific ``org.sonews.storage.Storage`` implementation.

Links and further information
=============================

Useful links regarding sonews and the sponsors:

- `Github Project Page <http://github.com/cli/sonews>`_, see here for issues
- `Sun Microsystems <http://www.sun.com/>`_, friendly sponsor.
- `University of Applied Sciences Osnabrueck <http://www.fh-osnabrueck>`_

Users
-----

As sonews is a relatively young project there are little users known, but there are some (if you know more let me know).

- `Sun Microsystems <http://www.sun.com/>`_ OpenOffice.org/StarOffice development located in Hamburg uses sonews to mirror the OpenOffice.org mailinglists.
- `news://news.sonews.org:119 <news://news.sonews.org:119>`_ uses sonews to provide a freely accessible demo Newsserver.

Contributors and sponsors
-------------------------

Maintainer and project lead: Christian Lins (contact christian at lins dot me)

See AUTHORS file for a complete list of contributors.

Thanks to Sun's Tooling/IT team in Hamburg for the support!

The author thanks `Sun Microsystems <http://www.sun.com/>`_ for fully financing the first version of sonews. A really free software supporting company!

If you like to support sonews with a donation of any kind (hardware, books, money, donuts,...), feel free to contact the project leader. A friendly email or a bug report is most welcome, too :-)

