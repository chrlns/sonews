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
  group_id      INTEGER,
  name          VARCHAR(80) NOT NULL,
  flags         TINYINT UNSIGNED DEFAULT 0,
  watermark	BIGINT,

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
  article_id  INT,
  message_id  VARCHAR(255),

  PRIMARY KEY(article_id),
  UNIQUE(message_id),
  FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE
)
ENGINE = INNODB
CHARACTER SET utf8;

CREATE TABLE headers
(
  article_id    INT,
  header_key    VARCHAR(255),
  header_value  TEXT,
  header_index  INT,

  PRIMARY KEY(article_id, header_key, header_index),
  FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE
)
ENGINE = INNODB
CHARACTER SET utf8;

/*
  Normalization: 1NF, 2NF
*/
CREATE TABLE postings 
(
  group_id      INTEGER,
  article_id    INTEGER,
  article_index BIGINT NOT NULL, 

  PRIMARY KEY(group_id, article_id),
  FOREIGN KEY (group_id) REFERENCES `groups`(group_id),
  FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE
)
ENGINE = INNODB
CHARACTER SET utf8;
