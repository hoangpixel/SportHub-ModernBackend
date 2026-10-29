ALTER TABLE bookings
    MODIFY COLUMN status ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED','EXPIRED','NO_SHOW') NOT NULL,
    ADD COLUMN expires_at DATETIME(6) NULL,
    ADD COLUMN cancellation_reason VARCHAR(500) NULL,
    ADD COLUMN cancelled_at DATETIME(6) NULL,
    ADD COLUMN cancelled_by VARCHAR(255) NULL,
    ADD COLUMN outcome_reason VARCHAR(500) NULL,
    ADD COLUMN resolved_by VARCHAR(255) NULL,
    ADD COLUMN resolved_at DATETIME(6) NULL;

ALTER TABLE payments
    MODIFY COLUMN status ENUM('PENDING','COMPLETED','FAILED','REFUND_PENDING','REFUNDED') NOT NULL,
    ADD COLUMN refunded_at DATETIME(6) NULL,
    ADD COLUMN refunded_by VARCHAR(255) NULL,
    ADD COLUMN refund_reference VARCHAR(200) NULL;

UPDATE bookings b
SET b.status = 'PENDING'
WHERE b.status = 'CONFIRMED'
  AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.id);

UPDATE bookings
SET expires_at = LEAST(DATE_ADD(created_at, INTERVAL 10 MINUTE), TIMESTAMP(booking_date, start_time))
WHERE status = 'PENDING';

CREATE INDEX idx_booking_expiry ON bookings(status, expires_at);
