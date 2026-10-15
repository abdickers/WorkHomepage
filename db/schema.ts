import {
  sqliteTable,
  text,
  integer,
  uniqueIndex,
} from "drizzle-orm/sqlite-core";
export const invoices = sqliteTable(
  "invoices",
  {
    id: text("id").primaryKey(),
    ownerId: text("owner_id").notNull(),
    fingerprint: text("fingerprint").notNull(),
    vendor: text("vendor").notNull(),
    invoiceNumber: text("invoice_number"),
    amountCents: integer("amount_cents"),
    currency: text("currency").notNull().default(""),
    dueDate: text("due_date"),
    messageId: text("message_id").notNull(),
    sourceUrl: text("source_url").notNull(),
    recipient: text("recipient").notNull(),
    subject: text("subject").notNull(),
    receivedAt: text("received_at").notNull(),
    status: text("status").notNull().default("review"),
    note: text("note").notNull(),
  },
  (t) => [
    uniqueIndex("invoice_owner_fingerprint").on(t.ownerId, t.fingerprint),
  ],
);
