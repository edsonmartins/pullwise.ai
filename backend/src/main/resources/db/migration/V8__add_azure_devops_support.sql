-- Azure DevOps platform support
-- Platform enum stored as VARCHAR, no DDL changes needed for the enum itself.
-- Add Azure DevOps specific fields to projects table.

ALTER TABLE projects ADD COLUMN IF NOT EXISTS azure_devops_org VARCHAR(255);
ALTER TABLE projects ADD COLUMN IF NOT EXISTS azure_devops_project VARCHAR(255);
