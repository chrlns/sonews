/* 
  flags:
  If bit 0 is set, groups is a mirrorred mailing list. 
  If not set default newsgroup.

  Normalization: 1NF, 2NF, 3NF
*/
CREATE CACHED TABLE groups 
(
  group_id      INT,
  name          VARCHAR(80) NOT NULL,
  flags         TINYINT DEFAULT 0,

  PRIMARY KEY(group_id),
  UNIQUE(name)
);

CREATE CACHED TABLE articles 
(
  article_id    INT,
  body          VARBINARY,

  PRIMARY KEY(article_id)
);

CREATE CACHED TABLE article_ids
(
  article_id  INT,
  message_id  VARCHAR(255),

  PRIMARY KEY(article_id),
  UNIQUE(message_id),
  FOREIGN KEY(article_id) REFERENCES articles(article_id) ON DELETE CASCADE
);

CREATE CACHED TABLE headers
(
  article_id    INT,
  header_key    VARCHAR(255),
  header_value  LONGVARCHAR,
  header_index  INT,

  PRIMARY KEY(article_id, header_key, header_index),
  FOREIGN KEY(article_id) REFERENCES articles(article_id) ON DELETE CASCADE
);

/*
  Normalization: 1NF, 2NF
*/
CREATE CACHED TABLE postings 
(
  group_id      INTEGER,
  article_id    INTEGER,
  article_index INTEGER NOT NULL, 

  PRIMARY KEY(group_id, article_id),
  FOREIGN KEY(article_id) REFERENCES articles(article_id) ON DELETE CASCADE
);

/* 
  Table for association of newsgroups and mailing-lists 

  Normalization: 1NF, 2NF, 3NF
*/
CREATE CACHED TABLE groups2list
(
  group_id    INTEGER,
  listaddress VARCHAR(255),

  PRIMARY KEY(group_id, listaddress),
  UNIQUE(listaddress),
  FOREIGN KEY(group_id) REFERENCES groups(group_id) ON DELETE CASCADE 
);

/* 
  Configuration table, containing key/value pairs 

  Normalization: 1NF, 2NF, 3NF
*/
CREATE CACHED TABLE config
(
  config_key     VARCHAR(255),
  config_value   LONGVARCHAR,

  PRIMARY KEY(config_key)
);

/* 
  Newsserver peers 
  feedtype: 0: pullfeed 1: pushfeed
  Normalization: 1NF (atomic values), 2NF
*/
CREATE CACHED TABLE peers
(
  peer_id     INT,
  host        VARCHAR(255),
  port        INT,

  PRIMARY KEY(peer_id),
  UNIQUE(host, port)
);

/* 
  List of newsgroups to feed into sonews 

  Normalization: 1NF, 2NF, 3NF
*/
CREATE CACHED TABLE peer_subscriptions
(
  peer_id    INTEGER,
  group_id   INTEGER,
  feedtype   TINYINT DEFAULT 0,

  PRIMARY KEY(peer_id, group_id, feedtype),
  FOREIGN KEY(peer_id) REFERENCES peers(peer_id) ON DELETE CASCADE,
  FOREIGN KEY(group_id) REFERENCES groups(group_id) ON DELETE CASCADE
);

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
CREATE CACHED TABLE events
(
  event_time         BIGINT,   /* time of this snapshot */
  event_key          TINYINT,  /* which data */
  group_id           INT ,

  PRIMARY KEY(event_time, event_key),
  FOREIGN KEY(group_id) REFERENCES groups(group_id) ON DELETE CASCADE
);

COMMIT;
SHUTDOWN;
