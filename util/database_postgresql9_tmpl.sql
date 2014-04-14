--- Create role sonews with password "sonews"
CREATE ROLE sonews LOGIN
  ENCRYPTED PASSWORD 'md5390af2bc9a786a23acc08ebbbc80fa5d'
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;

CREATE DATABASE sonews WITH ROLE sonews ENCODING 'UTF8';

CREATE TABLE articles 
(
  article_id    INT,
  body          BYTEA,

  PRIMARY KEY(article_id)
);
ALTER TABLE articles
  OWNER TO sonews;

CREATE TABLE article_ids
(
  article_id  INT REFERENCES articles(article_id) ON DELETE CASCADE,
  message_id  VARCHAR(255),

  PRIMARY KEY(article_id),
  UNIQUE(message_id)
);
ALTER TABLE article_ids
  OWNER TO sonews;

CREATE TABLE headers
(
  article_id    INT REFERENCES articles(article_id) ON DELETE CASCADE,
  header_key    VARCHAR(255),
  header_value  TEXT,
  header_index  INT,

  PRIMARY KEY(article_id, header_key, header_index)
);
ALTER TABLE headers
  OWNER TO sonews;

/*
  Normalization: 1NF, 2NF
*/
CREATE TABLE postings 
(
  group_id      INTEGER,
  article_id    INTEGER REFERENCES articles (article_id) ON DELETE CASCADE,
  article_index INTEGER NOT NULL, 

  PRIMARY KEY(group_id, article_id)
);
ALTER TABLE postings
  OWNER TO sonews;
