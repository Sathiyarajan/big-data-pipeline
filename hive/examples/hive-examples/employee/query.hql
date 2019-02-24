select e.first_name, e.family_name, avg(s.salary) as avg_salary from
    employee as e join salary as s on (e.employee_id == s.employee_id)
        group by e.first_name, e.family_name limit 5;

select e.gender, avg(s.salary) as avg_salary from
    employee as e join salary as s on (e.employee_id == s.employee_id)
        group by e.gender;

select e.first_name, e.family_name, avg(s.salary) as avg_salary from
    employee as e join salary as s on (e.employee_id == s.employee_id)
        group by e.first_name, e.family_name order by avg_salary limit 10;

select * from employee order by work_day asc limit 10;

select * from employee order by birthday asc limit 5;

select first_name, family_name, work_day from employee where work_day >= '1990-01-01' and work_day <= '1990-01-31'
