ALTER TABLE ad_campaign ALTER COLUMN start_date TYPE date USING start_date::date;
ALTER TABLE ad_campaign ALTER COLUMN end_date TYPE date USING end_date::date;