# tradr-logger

The logger subscribes to a kafka queue, fetches all the available data and logs them to differnent sinks. 
Right now, this is a Cassandra Database but usually this would also include HDFS or something similar.

Because of problems with Datastax Cassandra Connector this is currently a simple job: Start it and it automatically sits on  the queue and listens for new data. With the upcoming v4.0.0 of the cassandra connector this could be changed to allow more control from outside.
