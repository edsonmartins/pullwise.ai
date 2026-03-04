-- Add acknowledged flag to issues table for interactive CLI flow
ALTER TABLE issues ADD COLUMN acknowledged BOOLEAN DEFAULT FALSE;
