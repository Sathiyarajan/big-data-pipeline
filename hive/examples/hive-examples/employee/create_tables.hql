create external table employee (
	    employee_id INT,
	    birthday DATE,
	    first_name STRING,
	    family_name STRING,
	    gender CHAR(1),
	    work_day DATE)
row format serde 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
with serdeproperties (
	    "separatorChar" = ",",
	    "quoteChar"     = "'"
)
stored as textfile
location '/employee/';

create external table salary (
	    employee_id INT,
	    salary INT,
	    start_date DATE,
	    end_date DATE)
row format serde 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
with serdeproperties (
	    "separatorChar" = ",",
	    "quoteChar"     = "'"
)
stored as textfile
location '/salary/';
