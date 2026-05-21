ALTER TABLE advertiser DROP COLUMN budget;
ALTER TABLE advertiser DROP COLUMN currency;
ALTER TABLE advertiser DROP COLUMN status;

ALTER TABLE ad_campaign ADD COLUMN budget numeric(38, 2) NULL;