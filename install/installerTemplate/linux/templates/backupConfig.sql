alter system set archive_mode=on;
alter system set wal_level=replica;
alter system set archive_command='test ! -f [% backups_dir %]archive/%f && cp %p [% backups_dir %]archive/%f';
alter system set max_replication_slots = 10;
alter system set max_wal_senders = 10;
ALTER SYSTEM SET wal_keep_size = '16000';