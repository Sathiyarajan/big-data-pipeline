Data = LOAD '/operatorexam/eticaretlog' USING PigStorage(',') AS
(
userId : int,
country : chararray,
duration : int,
search : chararray
);
New_Data = GROUP Data BY country;
DUMP New_Data;