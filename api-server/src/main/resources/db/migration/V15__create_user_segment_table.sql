CREATE TABLE IF NOT EXISTS public.user_segment (
    user_id VARCHAR(255) NOT NULL,
    segment_id VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_segment_pkey PRIMARY KEY (user_id, segment_id)
);

CREATE INDEX IF NOT EXISTS idx_user_segment_user_id
    ON public.user_segment (user_id);
