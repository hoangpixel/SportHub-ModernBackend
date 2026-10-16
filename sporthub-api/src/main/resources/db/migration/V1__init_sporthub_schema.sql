-- SportHub: baseline schema migration (MySQL 8.x)
-- Derived from schema DDL of the uploaded MySQL dump (database: sporthub).
-- IMPORTANT: Intended for a NEW, EMPTY database only. Do not run on an existing database.
-- Excludes all INSERTs, DROP TABLEs, MySQL dump session commands, and sample user data.
-- Foreign-key dependencies are ordered before their referencing tables.
-- Version 1 is immutable after successful deployment. New changes belong in V2, V3, ...

-- matches
CREATE TABLE `matches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `court_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `round_number` int DEFAULT NULL,
  `scheduled_date` date DEFAULT NULL,
  `start_time` time DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','IN_PROGRESS','SCHEDULED') DEFAULT NULL,
  `team1_id` bigint DEFAULT NULL,
  `team1_score` int DEFAULT NULL,
  `team2_id` bigint DEFAULT NULL,
  `team2_score` int DEFAULT NULL,
  `tournament_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `winner_team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- permissions
CREATE TABLE `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_code` (`code`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- roles
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_name` (`name`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- sports
CREATE TABLE `sports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `icon_url` varchar(500) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtj61or3k005spbrx0lgjpwtde` (`name`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- team_members
CREATE TABLE `team_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `joined_at` datetime(6) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `team_role` enum('CAPTAIN','MEMBER') DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- teams
CREATE TABLE `teams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `logo_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `sport_id` bigint DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- tournament_teams
CREATE TABLE `tournament_teams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `registered_at` datetime(6) DEFAULT NULL,
  `registration_status` enum('APPROVED','PENDING','REJECTED') DEFAULT NULL,
  `seed_number` int DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `tournament_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- tournaments
CREATE TABLE `tournaments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `max_teams` int DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `registration_end` date DEFAULT NULL,
  `registration_start` date DEFAULT NULL,
  `sport_id` bigint DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','ONGOING','UPCOMING') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `venue_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- users
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `username` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- bookings
CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_code` varchar(50) NOT NULL,
  `booking_date` date NOT NULL,
  `court_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `end_time` time NOT NULL,
  `note` varchar(500) DEFAULT NULL,
  `start_time` time NOT NULL,
  `status` enum('CANCELLED','COMPLETED','CONFIRMED','PENDING') NOT NULL,
  `total_amount` decimal(12,2) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKq97166k18hklq6ls46osbrftx` (`booking_code`),
  KEY `idx_booking_overlap` (`court_id`,`booking_date`,`start_time`,`end_time`),
  KEY `FKeyog2oic85xg7hsu2je2lx3s6` (`user_id`),
  CONSTRAINT `FKeyog2oic85xg7hsu2je2lx3s6` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- role_permissions
CREATE TABLE `role_permissions` (
  `permission_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`permission_id`,`role_id`),
  KEY `FKn5fotdgk8d1xvo8nav9uv3muc` (`role_id`),
  CONSTRAINT `FKegdk29eiy7mdtefy5c7eirr6e` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `FKn5fotdgk8d1xvo8nav9uv3muc` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- user_profiles
CREATE TABLE `user_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `gender` varchar(20) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_profiles_user` (`user_id`),
  CONSTRAINT `FKjcad5nfve11khsnpwj1mv8frj` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- user_roles
CREATE TABLE `user_roles` (
  `role_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`user_id`),
  KEY `FKhfh9dx7w3ubf1co1vdev94g3f` (`user_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- venues
CREATE TABLE `venues` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(500) NOT NULL,
  `city` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_venue_name` (`name`),
  KEY `idx_venue_city` (`city`),
  KEY `idx_venue_district` (`district`),
  KEY `idx_venue_owner_id` (`owner_id`),
  CONSTRAINT `FKq22to1hxop8unjpmeawieega6` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- courts
CREATE TABLE `courts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `close_time` time DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `open_time` time DEFAULT NULL,
  `price_per_hour` decimal(12,2) NOT NULL,
  `status` enum('ACTIVE','INACTIVE','MAINTENANCE') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `sport_id` bigint NOT NULL,
  `venue_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_court_name` (`name`),
  KEY `idx_court_venue_id` (`venue_id`),
  KEY `idx_court_sport_id` (`sport_id`),
  KEY `idx_court_price` (`price_per_hour`),
  CONSTRAINT `FKc588eogjvkwv2gfs9ulycwbk1` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`),
  CONSTRAINT `FKl47nyo4h27u5jruxs7utlo0va` FOREIGN KEY (`sport_id`) REFERENCES `sports` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- payments
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(12,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `payment_method` enum('BANK_TRANSFER','CASH','MOMO','VNPAY') NOT NULL,
  `status` enum('COMPLETED','FAILED','PENDING','REFUNDED') NOT NULL,
  `transaction_code` varchar(100) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `booking_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnuscjm6x127hkb15kcb8n56wo` (`booking_id`),
  UNIQUE KEY `UK8inpv30544qjykcwa6ck7pusy` (`transaction_code`),
  CONSTRAINT `FKc52o2b1jkxttngufqp3t7jr3h` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- reviews
CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` text,
  `court_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `rating` int NOT NULL,
  `status` enum('HIDDEN','VISIBLE') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `booking_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3p9j9vyr1qofbcxju65es206r` (`booking_id`),
  KEY `FKcgy7qjc1r99dp117y9en6lxye` (`user_id`),
  CONSTRAINT `FK28an517hrxtt2bsg93uefugrm` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `FKcgy7qjc1r99dp117y9en6lxye` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- court_schedules
CREATE TABLE `court_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `available` bit(1) NOT NULL,
  `close_time` time NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') NOT NULL,
  `open_time` time NOT NULL,
  `court_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_court_schedule_day` (`court_id`,`day_of_week`),
  CONSTRAINT `FKng5egae5us8jm646ld017fxku` FOREIGN KEY (`court_id`) REFERENCES `courts` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
