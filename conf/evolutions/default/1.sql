# Feed schema
 
# --- !Ups

CREATE SEQUENCE module_id_seq start with 1000;
CREATE SEQUENCE feed_id_seq start with 1000;
CREATE SEQUENCE tab_id_seq start with 1000;

create table users (
  mail                      varchar(255) not null primary key,
  name                      varchar(255) not null,
  password                  varchar(255) not null
);

create table tab (
  id                        bigint not null primary key,
  title                     varchar(31) not null,
  position                  int not null,
  users                     varchar(255) not null,
  foreign key(users)         references users(mail) on delete set null
);

create table module (
  id                        bigint not null primary key,
  tabId                     bigint not null,
  title                     varchar(255) not null,
  website_url               varchar(511) not null,
  url                       varchar(511) not null,
  status                    varchar(255),
  lastUpdate                timestamp,
  type                      varchar(127),
  foreign key(tabId)        references tab(id) on delete set null
);

create table feed (
  id                        bigint not null primary key,
  identifier        	    varchar(255) not null,
  title                     varchar(255) not null,
  url                       varchar(255) not null,
  description               varchar(2047),
  pubDate                   timestamp not null,
  moduleId                  bigint not null,
  read            boolean,
  foreign key(moduleId)     references module(id) on delete set null
);

 
# --- !Downs
DROP TABLE if exists module;
DROP TABLE if exists feed;
DROP TABLE if exists users;
DROP TABLE if exists tab;

DROP SEQUENCE module_id_seq;
DROP SEQUENCE feed_id_seq;
DROP SEQUENCE tab_id_seq;



