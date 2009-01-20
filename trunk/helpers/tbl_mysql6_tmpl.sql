CREATE DATABASE staroffice_news;

CREATE TABLE groups 
(
  group_id      SERIAL,
  name          VARCHAR(80) NOT NULL,
  flags         INTEGER DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX name_id_index ON groups (name);

CREATE TABLE articles 
(
  article_id    SERIAL,
  message_id    TEXT,
  header        TEXT,
  body          TEXT
);

CREATE UNIQUE INDEX article_message_index ON articles (message_id(255));

CREATE TABLE postings 
(
  group_id      INTEGER,
  article_id    INTEGER,
  article_index INTEGER NOT NULL
);

CREATE UNIQUE INDEX posting_article_index ON postings (article_id);

CREATE TABLE subscriptions 
(
  group_id    INTEGER
);
    
CREATE TABLE overview 
(
  header      TEXT
);
