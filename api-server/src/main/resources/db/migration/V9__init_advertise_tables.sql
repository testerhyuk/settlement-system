CREATE TABLE public.advertiser (
    advertiser_id VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NULL,
    budget numeric(38, 2) NULL,
    currency VARCHAR(255) NULL,
    status VARCHAR(255) NULL,
    CONSTRAINT advertiser_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying])::text[]))),
    CONSTRAINT advertiser_pkey PRIMARY KEY (advertiser_id)
);

CREATE TABLE public.ad_campaign (
    campaign_id VARCHAR(255) NOT NULL,
    advertiser_id VARCHAR(255) NOT NULL,
    cpc_amount numeric(38, 2) NULL,
    currency VARCHAR(255) NULL,
    start_date timestamp(6) NULL,
    end_date timestamp(6) NULL,
    status VARCHAR(255) NULL,
    CONSTRAINT adcampaign_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'PAUSED'::character varying, 'ENDED'::character varying, 'BUDGET_EXHAUSTED'::character varying])::text[]))),
    CONSTRAINT ad_campaign_pkey PRIMARY KEY (campaign_id)
);