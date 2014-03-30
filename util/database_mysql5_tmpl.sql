/* 
  Create a database at first:
    CREATE DATABASE sonews CHARACTER SET utf8
*/

/* 
  flags:
  If bit 0 is set, groups is a mirrorred mailing list. 
  If not set default newsgroup.

  Normalization: 1NF, 2NF, 3NF
*/
CREATE TABLE groups 
(
  group_id      SERIAL,
  name          VARCHAR(80) NOT NULL,
  flags         TINYINT UNSIGNED DEFAULT 0,

  PRIMARY KEY(group_id),
  UNIQUE(name)
)
ENGINE = INNODB
CHARACTER SET utf8;

CREATE TABLE articles 
(
  article_id    INT,
  body          LONGBLOB,

  PRIMARY KEY(article_id)
)
ENGINE = INNODB
CHARACTER SET utf8;

CREATE TABLE article_ids
(
  article_id  INT REFERENCES articles.article_id ON DELETE CASCADE,
  message_id  VARCHAR(255),

  PRIMARY KEY(article_id),
  UNIQUE(message_id)
)
ENGINE = INNODB
CHARACTER SET utf8;

CREATE TABLE headers
(
  article_id    INT REFERENCES articles.article_id ON DELETE CASCADE,
  header_key    VARCHAR(255),
  header_value  TEXT, /* Max. 64k */
  header_index  INT,

  PRIMARY KEY(article_id, header_key, header_index)
)
ENGINE = INNODB
CHARACTER SET utf8;

/*
  Normalization: 1NF, 2NF
*/
CREATE TABLE postings 
(
  group_id      INTEGER,
  article_id    INTEGER REFERENCES articles.article_id ON DELETE CASCADE,
  article_index INTEGER NOT NULL, 

  PRIMARY KEY(group_id, article_id)
)
ENGINE = INNODB
CHARACTER SET utf8;

/* 
  Table for association of newsgroups and mailing-lists 

  Normalization: 1NF, 2NF, 3NF
*/
CREATE TABLE groups2list
(
  group_id    INTEGER REFERENCES groups.group_id ON DELETE CASCADE,
  listaddress VARCHAR(255),

  PRIMARY KEY(group_id, listaddress),
  UNIQUE(listaddress)
)
ENGINE = INNODB
CHARACTER SET utf8;

/* 
  Configuration table, containing key/value pairs 

  Normalization: 1NF, 2NF, 3NF
*/
CREATE TABLE config
(
  config_key     VARCHAR(255),
  config_value   TEXT,

  PRIMARY KEY(config_key)
)
ENGINE = INNODB
CHARACTER SET utf8;

/* 
  Newsserver peers 
  feedtype: 0: pullfeed 1: pushfeed
  Normalization: 1NF (atomic values), 2NF
*/
CREATE TABLE peers
(
  peer_id     SERIAL,
  host        VARCHAR(255),
  port        SMALLINT UNSIGNED,

  PRIMARY KEY(peer_id),
  UNIQUE(host, port)
)
ENGINE = INNODB
CHARACTER SET utf8;

/* 
  List of newsgroups to feed into sonews 

  Normalization: 1NF, 2NF, 3NF
*/
CREATE TABLE peer_subscriptions
(
  peer_id    INTEGER REFERENCES peers.peer_id ON DELETE CASCADE,
  group_id   INTEGER REFERENCES groups.group_id ON DELETE CASCADE,
  feedtype   TINYINT UNSIGNED DEFAULT 0,

  PRIMARY KEY(peer_id, group_id, feedtype)
)
ENGINE = INNODB
CHARACTER SET utf8;

/* 
   Tables for server event statistics

   Possible statistic keys:
   1=CONNECTIONS     (active connections)
   2=POSTED_NEWS     (directly to the server posted unique messages)
   3=GATEWAYED_NEWS  (posted unique message gateways through the ML-gateway)
   4=FEEDED_NEWS     (unique messages feed via NNTP)

   The server will create snapshots of the above data.

   Normalization: 1NF, 2NF
*/
CREATE TABLE events
(
  event_time         BIGINT UNSIGNED,   /* time of this snapshot */
  event_key          TINYINT UNSIGNED,  /* which data */
  group_id           INT REFERENCES groups.group_id ON DELETE CASCADE,

  PRIMARY KEY(event_time, event_key)
)
ENGINE = INNODB
CHARACTER SET utf8;
