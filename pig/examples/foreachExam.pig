Data = LOAD '/operatorexam/eticaretlog' USING PigStorage(',') AS
(
userId : int,
country : chararray,
duration : int,
search : chararray
);
Gr_Data = GROUP Data BY country;

Result_Data = FOREACH Gr_Data{
	Generate
	group,
	AVG(Data.duration);
}
DUMP Result_Data;