ALTER TABLE merchant ADD COLUMN merchant_status VARCHAR(255);
UPDATE merchant SET merchant_status = 'ACTIVE' WHERE merchant_status IS NULL;
ALTER TABLE merchant ALTER COLUMN merchant_status SET NOT NULL;