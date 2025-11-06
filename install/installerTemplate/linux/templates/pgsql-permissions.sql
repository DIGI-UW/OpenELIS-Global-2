
CREATE USER clinlims PASSWORD '[% itechappPassword %]';
CREATE USER admin SUPERUSER PASSWORD '[% adminPassword %]';
ALTER DATABASE clinlims OWNER TO clinlims;

