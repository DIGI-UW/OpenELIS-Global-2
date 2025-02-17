# Password Migration

# Notes:

This procedure is to be done for old instances of OpenELIS that are using an
outdated method of storing passwords. To check if the old method is used, follow
these steps:

1. Connect to running database
1. Query login_user table
   1. `SELECT * FROM clinlims.login_user;`
1. Check the password column values

If all passwords start with something similar to $2a$12$ then the passwords have
been migrated, if not, then this procedure must be completed for OE2 to
function.

It is recommended for all users to **change their password **after this tool is
run. This is because the old method of storing passwords was insecure so it is
possible that an attacker compromised their old password.

# Migrating Passwords

## Install Python tools on computer with connection to DB

1. Run the following commands
   1. `sudo apt update`
   1. `wget https://bootstrap.pypa.io/pip/2.7/get-pip.py`
   1. `python2 get-pip.py`
   1. `sudo apt install libpq-dev python-dev`
   1. `python2 -m pip install pycrypto`
   1. `python2 -m pip install psycopg2`
   1. `python2 -m pip install bcrypt`

## Run the Password Migration tool

1. Download the
   [Password Migration](https://github.com/I-TECH-UW/Password-Migrator) tool and
   unpack it
   1. `wget https://github.com/I-TECH-UW/Password-Migrator/archive/master.tar.gz`
   1. `tar -xvzf master.tar.gz`
1. Run the tool and follow instructions
   1. `python2 Password-Migrator-master/migrator/migrate.py`
   1. Provide DB connection info
1. Confirm that no errors occurred
