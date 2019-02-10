Data = LOAD '/pig-input/eticaretlog.csv' USING PigStorage(',') AS
(
userId : int,
country : chararray,
duration : int,
search : chararray
);
Gr_Data = GROUP Data BY country;

New_Data = FOREACH Gr_Data{
	Generate
	group,
	MAX(Data.duration) as maxDurTime;
}
DUMP New_Data;

STORE New_Data INTO ' hdfs://localhost:9000/pig-output-feb/ ' USING PigStorage (',');
