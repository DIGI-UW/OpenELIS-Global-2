ALTER SYSTEM SET wal_level = 'replica';
ALTER SYSTEM SET wal_log_hints = 'on';
ALTER SYSTEM SET max_wal_senders = '3';
ALTER SYSTEM SET wal_keep_size = '16000';
ALTER SYSTEM SET hot_standby = 'on';
