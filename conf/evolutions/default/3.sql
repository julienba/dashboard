
# --- !Ups

ALTER TABLE module ADD position int DEFAULT 0


# --- !Downs

ALTER TABLE module DROP COLUMN position


