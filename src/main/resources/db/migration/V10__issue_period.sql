ALTER TABLE issues ADD COLUMN end_day DATE;
UPDATE issues SET end_day = day;
ALTER TABLE issues ALTER COLUMN end_day SET NOT NULL;
ALTER TABLE issues ADD CONSTRAINT issues_end_day_check CHECK (end_day >= day);
