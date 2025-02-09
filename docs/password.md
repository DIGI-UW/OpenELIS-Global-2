# Password Migration

## Overview

This procedure is required for legacy OpenELIS instances using outdated password storage methods. Before proceeding, verify if migration is necessary:

1. Connect to your running database
2. Query the login_user table:
```sql
SELECT * FROM clinlims.login_user;
```
3. Check the password column values:
   - If passwords start with `$2a$12$`: Migration already completed
   - If not: Migration required for OE2 functionality

**Security Notice:** Due to the previous insecure password storage method, all users should change their passwords after running this migration tool.

## Migration Steps

### 1. Install Required Python Tools

Run these commands on a computer with database access:
```bash
sudo apt update
wget https://bootstrap.pypa.io/pip/2.7/get-pip.py
python2 get-pip.py
sudo apt install libpq-dev python-dev
python2 -m pip install pycrypto
python2 -m pip install psycopg2
python2 -m pip install bcrypt
```

### 2. Download and Run Migration Tool

1. Download and extract the Password Migration tool:
```bash
wget https://github.com/I-TECH-UW/Password-Migrator/archive/master.tar.gz
tar -xvzf master.tar.gz
```

2. Run the migration script:
```bash
python2 Password-Migrator-master/migrator/migrate.py
```

3. Follow the prompts to provide database connection information

4. Verify that no errors occurred during migration

### Post-Migration Steps

1. Verify all passwords in the database now start with `$2a$12$`
2. Notify all users to change their passwords at next login
3. Document the migration completion in your system records
