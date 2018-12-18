# Cassandra Java and Scala Examples

This repo shows examples using Apache Cassandra and Datastax Java Driver for Apache Cassandra                          
>Connecting to Cassandra Using Java and Scala programming languages.              
>Basic example to test the connection with Cassandra.                                                  
>Connecting to cassandra in a secured way .                                       
>Performing CRUD oprations in Cassandra.                                         

Cassandra Installation:                                       
-----------------------                                       
wget http://downloads.datastax.com/community/dsc-cassandra-3.0.9-bin.tar.gz                                       
tar -zxvf dsc-cassandra-3.0.9-bin.tar.gz                              

vi ~/.bash_profile                                       
export CASSANDRA_HOME=/users/apps/cassandra/dsc-cassandra-3.0.9/                                       
export PATH=$CASSANDRA_HOME/bin:$PATH                                       
source ~/.bash_profile                                       

Start Cassandra as a background process:                                       
>sudo cassandra&                                       
>cqlsh                                       

Creating Cassandra Keyspace and Table :                                                                    
---------------------------------------                             
create keyspace dev  with replication = {'class':'SimpleStrategy','replication_factor':1};                
 use dev;                           
create table emp (empid int primary key,emp_first varchar, emp_last varchar, emp_dept varchar);                    

insert into emp (empid, emp_first, emp_last, emp_dept) values (1,'rev','kumar','eng');                            
insert into emp (empid, emp_first, emp_last, emp_dept) values (2,'smith','reddy','eng');                                 
insert into emp (empid, emp_first, emp_last, emp_dept) values (3,'shyam','sunder','CA');                                    


See also for Reference :      
------------------------
Cassandra Installation on Windows:                                         
http://cassandra.apache.org/                                                
http://www.luketillman.com/developing-with-cassandra-on-windows/        

Configuring data consistency :                    
http://docs.datastax.com/en/archived/cassandra/2.0/cassandra/dml/dml_config_consistency_c.html                                          

Using Cassandra CQL :                                                 
https://docs.datastax.com/en/cql/3.1/cql/cql_using/about_cql_c.html                                                

Node calculator :                                                
https://www.ecyrd.com/cassandracalculator/                                                

Cassandra Partitioning & Clustering Keys Explained :                                                
http://datascale.io/cassandra-partitioning-and-clustering-keys-explained/                                                
https://www.datastax.com/dev/blog/the-most-important-thing-to-know-in-cassandra-data-modeling-the-primary-key            

Understanding How Cassandra Stores Data :                                            
https://www.hakkalabs.co/articles/how-cassandra-stores-data                                           

Significance of Vnodes in Cassandra:                                           
http://stackoverflow.com/questions/25379457/what-is-virtual-nodes-and-how-it-is-helping-during-partitioning-in-casssandra           
http://stackoverflow.com/questions/38423888/significance-of-vnodes-in-cassandra                                                      
http://www.datastax.com/dev/blog/virtual-nodes-in-cassandra-1-2                                            

Accessing Cassandra from Spark in Java:                                                 
http://www.datastax.com/dev/blog/accessing-cassandra-from-spark-in-java                                                

Cassandra-Spark-Demo Project :
https://github.com/doanduyhai/Cassandra-Spark-Demo                                                 

DataStax course  on  Apache Cassandra /spark :                                                
https://academy.datastax.com/resources/ds201-foundations-apache-cassandra                                                 
https://academy.datastax.com/resources/getting-started-apache-spark                                                 
                                       
Install and setup Apache Cassandra Single Node cluster   :                                                                                http://www.techburps.com/cassandra/cassandra-single-node-cluster/59/         

Install and setup Apache Cassandra Multiple Node cluster :                                                               http://www.techburps.com/cassandra/cassandra-multiple-node-cluster/60/


------------------------------------------------------------------------------------------------------------------------------------     

You can reach me for any suggestions/clarifications on  : revanthkumar95@gmail.com                                              
Feel free to share any insights or constructive criticism. Cheers!!                                                           
#Happy Learning!!!.. 
