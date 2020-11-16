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

COMMIT;
SHUTDOWN;
