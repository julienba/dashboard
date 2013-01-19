# --- !Ups

INSERT INTO users(mail, name, password) VALUES ('julien.bille@nospam.com', 'jb', 'password1234');
INSERT INTO tab(id, title, position, users) VALUES (1, 'tab1', 0, 'julien.bille@nospam.com');
INSERT INTO tab(id, title, position, users) VALUES (2, 'tab2', 0, 'julien.bille@nospam.com');

INSERT INTO module(id, tabId, title, type, website_url, url, status, lastUpdate) VALUES (1, 1, 'Flux RSS de Blog Xebia France', 'application/rss+xml', 'http://blog.xebia.fr/', 'http://blog.xebia.fr/feed/', 'OK', NOW());
INSERT INTO module(id, tabId, title, type, website_url, url, status, lastUpdate) VALUES (2, 1, 'Atom 0.3', 'application/atom+xml', 'http://www.touilleur-express.fr/', 'http://www.touilleur-express.fr/feed/atom/', 'OK', NOW());

# --- !Downs

DELETE FROM users;
DELETE FROM tab;
DELETE FROM module;


