ALTER TABLE settlement ADD COLUMN retry_count SMALLINT;
UPDATE settlement SET retry_count = 0 WHERE retry_count IS NULL;
ALTER TABLE settlement ALTER COLUMN retry_count SET NOT NULL;