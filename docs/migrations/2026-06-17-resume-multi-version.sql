USE ai_recruit;

ALTER TABLE `resume`
  DROP INDEX `uk_seeker_id`;

ALTER TABLE `resume`
  ADD INDEX `idx_resume_seeker_id` (`seeker_id`);
