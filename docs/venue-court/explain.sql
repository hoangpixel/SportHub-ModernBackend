-- Run against compose.verify.yml only. Queries contain demo IDs, no secrets.
SELECT VERSION() AS mysql_version;
SELECT 'venues' AS entity, COUNT(*) AS total FROM venues
UNION ALL SELECT 'courts', COUNT(*) FROM courts
UNION ALL SELECT 'court_schedules', COUNT(*) FROM court_schedules;
SHOW INDEX FROM venues;
SHOW INDEX FROM courts;
EXPLAIN SELECT c.id, c.name, c.price_per_hour
FROM courts c JOIN venues v ON v.id = c.venue_id JOIN sports s ON s.id = c.sport_id
WHERE c.venue_id = 1 AND c.sport_id = 1
ORDER BY c.price_per_hour, c.id LIMIT 1 OFFSET 1;
EXPLAIN SELECT v.id, v.name FROM venues v
WHERE v.owner_id = 2 AND LOWER(v.name) LIKE '%arena%'
ORDER BY v.id LIMIT 10 OFFSET 0;
