DELETE from clinlims.site_information where name = 'siteNumber';
INSERT INTO clinlims.site_information( id, lastupdated, "name", description, "value") VALUES ( nextval('clinlims.site_information_seq'), now(), 'siteNumber', 'The site number of the this lab', 'DEV01' );

DELETE from clinlims.site_information where name = 'Accession number prefix';
INSERT INTO clinlims.site_information( id, lastupdated, "name", description, "value") VALUES ( nextval('clinlims.site_information_seq'), now(), 'Accession number prefix', 'Prefix for SITEYEARNUM format.  Can not be changed if there are samples', 'DEV01' );

