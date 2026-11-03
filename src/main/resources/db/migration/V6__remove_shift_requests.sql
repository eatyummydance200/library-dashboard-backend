UPDATE issues SET category = '공지' WHERE category = '근무 변경';
ALTER TABLE issues DROP CONSTRAINT fk_issues_shift_id;
ALTER TABLE issues DROP CONSTRAINT fk_issues_shift_staff_id;
DROP INDEX issues_shift;
ALTER TABLE issues DROP COLUMN shift_id;
ALTER TABLE issues DROP COLUMN shift_staff_id;
ALTER TABLE issues DROP COLUMN shift_start;
ALTER TABLE issues DROP COLUMN shift_end;
ALTER TABLE issues DROP COLUMN shift_role;
