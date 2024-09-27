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

CREATE TABLE groups
(
  group_id  INTEGER,
  name      TEXT,
  flags     TEXT,
  watermark BIGINT,
  
  PRIMARY KEY(group_id)
);
ENGINE = INNODB
CHARACTER SET utf8;

/*
  Normalization: 1NF, 2NF
*/
CREATE TABLE postings 
(
  group_id      INTEGER REFERENCES groups.group_id,
  article_id    INTEGER REFERENCES articles.article_id ON DELETE CASCADE,
  article_index BIGINT NOT NULL, 

  PRIMARY KEY(group_id, article_id)
)
ENGINE = INNODB
CHARACTER SET utf8;
