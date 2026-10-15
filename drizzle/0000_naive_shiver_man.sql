CREATE TABLE `invoices` (
	`id` text PRIMARY KEY NOT NULL,
	`owner_id` text NOT NULL,
	`fingerprint` text NOT NULL,
	`vendor` text NOT NULL,
	`invoice_number` text,
	`amount_cents` integer,
	`currency` text DEFAULT '' NOT NULL,
	`due_date` text,
	`message_id` text NOT NULL,
	`source_url` text NOT NULL,
	`recipient` text NOT NULL,
	`subject` text NOT NULL,
	`received_at` text NOT NULL,
	`status` text DEFAULT 'review' NOT NULL,
	`note` text NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `invoice_owner_fingerprint` ON `invoices` (`owner_id`,`fingerprint`);