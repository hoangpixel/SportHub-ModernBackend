-- V2: Prevent duplicate venue names for the same owner.

ALTER TABLE venues
ADD CONSTRAINT uk_venues_owner_name
UNIQUE (owner_id, name);