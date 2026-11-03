ALTER TABLE issues ADD COLUMN shift_id BIGINT;
ALTER TABLE issues ADD COLUMN shift_staff_id BIGINT;
ALTER TABLE issues ADD COLUMN shift_start INTEGER;
ALTER TABLE issues ADD COLUMN shift_end INTEGER;
ALTER TABLE issues ADD COLUMN shift_role VARCHAR(16);
ALTER TABLE issues ADD CONSTRAINT fk_issues_shift_id FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE SET NULL;
ALTER TABLE issues ADD CONSTRAINT fk_issues_shift_staff_id FOREIGN KEY (shift_staff_id) REFERENCES staff(id);
CREATE INDEX issues_shift ON issues(shift_id);
