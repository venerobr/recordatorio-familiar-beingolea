CREATE TABLE IF NOT EXISTS members (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS birthdays (
  id TEXT PRIMARY KEY,
  person_id TEXT NOT NULL REFERENCES members(id),
  date_mmdd TEXT NOT NULL,
  message TEXT NOT NULL,
  recipient_ids TEXT NOT NULL,
  last_sent_year TEXT
);

CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
