CREATE TABLE test2 (
  space text,
  key text,
  value text,
  PRIMARY KEY (space, key)
) with CLUSTERING ORDER BY (key ASC);

INSERT INTO test2 (space, key, value) VALUES ('foo', 'blah', 'meh');

UPDATE test2 SET value = 'profit!' WHERE space = 'foo' AND key = 'blah';