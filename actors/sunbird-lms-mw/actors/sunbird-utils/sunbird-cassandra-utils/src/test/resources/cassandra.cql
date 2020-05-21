CREATE KEYSPACE IF NOT EXISTS sunbird1 WITH replication = {'class':'SimpleStrategy','replication_factor':1};

//to change cluster name
//UPDATE system.local SET cluster_name = 'sunbird' where key='local';
//ALTER USER cassandra WITH PASSWORD 'password';
USE sunbird1;

//Address Type values(permanent, current, office, home)
CREATE TABLE IF NOT EXISTS sunbird1.address1(id text, userId text, country text,state text,city text,zipCode text,addType text,createdDate text,createdBy text,updatedDate text,updatedBy text, PRIMARY KEY (id));
//CREATE INDEX inx_add_userid ON sunbird1.address1 (userId);
//CREATE INDEX inx_add_addType ON sunbird1.address1 (addType);

//ALTER TABLE sunbird1.address1 ADD addressLine1 text;
//ALTER TABLE sunbird1.address1 ADD addressLine2 text;

//ALTER TABLE sunbird1.address1 ADD isDeleted boolean;