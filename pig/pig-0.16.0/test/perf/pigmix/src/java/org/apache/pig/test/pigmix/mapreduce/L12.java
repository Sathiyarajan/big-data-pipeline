/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pig.test.pigmix.mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.KeyValueTextInputFormat;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.jobcontrol.Job;
import org.apache.hadoop.mapred.jobcontrol.JobControl;
import org.apache.hadoop.mapred.lib.IdentityMapper;

import org.apache.pig.test.pigmix.mapreduce.Library;

public class L12 {

    public static class HighestValuePagePerUser extends MapReduceBase
        implements Mapper<LongWritable, Text, Text, DoubleWritable>,
        Reducer<Text, DoubleWritable, Text, DoubleWritable> {

        public void map(
                LongWritable k,
                Text val,
                OutputCollector<Text, DoubleWritable> oc,
                Reporter reporter) throws IOException {

            List<Text> fields = Library.splitLine(val, '');

            // Filter out null users or query terms.
            if (fields.get(0).getLength() == 0 ||
                    fields.get(3).getLength() == 0) return;
            try {
                oc.collect(fields.get(0),
                    new DoubleWritable(Double.valueOf(fields.get(6).toString())));
            } catch (NumberFormatException nfe) {
                oc.collect(fields.get(0), new DoubleWritable(0));
            }
        }

        public void reduce(
                Text key,
                Iterator<DoubleWritable> iter, 
                OutputCollector<Text, DoubleWritable> oc,
                Reporter reporter) throws IOException {
            double max = Double.NEGATIVE_INFINITY;

            while (iter.hasNext()) {
                double d = iter.next().get();
            	if (max < d) max=d;
            }
            oc.collect(key, new DoubleWritable(max));
        }
    }

    public static class TotalTimespentPerTerm extends MapReduceBase
        implements Mapper<LongWritable, Text, Text, LongWritable>,
        Reducer<Text, LongWritable, Text, LongWritable> {

        public void map(
                LongWritable k,
                Text val,
                OutputCollector<Text, LongWritable> oc,
                Reporter reporter) throws IOException {
            List<Text> fields = Library.splitLine(val, '');

            // Filter out non-null users
            if (fields.get(0).getLength() != 0) return;
            try {
                oc.collect(fields.get(3),
                    new LongWritable(Long.valueOf(fields.get(2).toString())));
            } catch (NumberFormatException nfe) {
                oc.collect(fields.get(3), new LongWritable(0));
            }

        }

        public void reduce(
                Text key,
                Iterator<LongWritable> iter, 
                OutputCollector<Text, LongWritable> oc,
                Reporter reporter) throws IOException {
            long sum = 0;

            while (iter.hasNext()) sum += iter.next().get();
            oc.collect(key, new LongWritable(sum));
        }
    }

    public static class QueriesPerAction extends MapReduceBase
        implements Mapper<LongWritable, Text, Text, LongWritable>,
        Reducer<Text, LongWritable, Text, LongWritable> {

        public void map(
                LongWritable k,
                Text val,
                OutputCollector<Text, LongWritable> oc,
                Reporter reporter) throws IOException {
            List<Text> fields = Library.splitLine(val, '');
            
            // Filter out non-null users and non-null queries
            if (fields.get(0).getLength() == 0 || fields.get(3).getLength() != 0) return;
            oc.collect(fields.get(1), new LongWritable(1));
       }

        public void reduce(
                Text key,
                Iterator<LongWritable> iter, 
                OutputCollector<Text, LongWritable> oc,
                Reporter reporter) throws IOException {
  
        	long cnt = 0;
            while (iter.hasNext()) {
                LongWritable l = iter.next();
            	cnt += l.get();
            }
            oc.collect(key, new LongWritable(cnt));
        }
    }


    public static void main(String[] args) throws IOException {

        if (args.length!=3) {
            System.out.println("Parameters: inputDir outputDir parallel");
            System.exit(1);
        }
        String inputDir = args[0];
        String outputDir = args[1];
        String parallel = args[2];
        String user = System.getProperty("user.name");
        JobConf lp = new JobConf(L12.class);
        lp.setJobName("L12 Find Highest Value Page Per User");
        lp.setInputFormat(TextInputFormat.class);
        lp.setOutputKeyClass(Text.class);
        lp.setOutputValueClass(DoubleWritable.class);
        lp.setMapperClass(HighestValuePagePerUser.class);
        lp.setCombinerClass(HighestValuePagePerUser.class);
        lp.setReducerClass(HighestValuePagePerUser.class);
        Properties props = System.getProperties();
        for (Map.Entry<Object,Object> entry : props.entrySet()) {
            lp.set((String)entry.getKey(), (String)entry.getValue());
        }
        FileInputFormat.addInputPath(lp, new Path(inputDir + "/page_views"));
        FileOutputFormat.setOutputPath(lp, new Path(outputDir + "/highest_value_page_per_user"));
        lp.setNumReduceTasks(Integer.parseInt(parallel));
        Job loadPages = new Job(lp);

        JobConf lu = new JobConf(L12.class);
        lu.setJobName("L12 Find Total Timespent per Term");
        lu.setInputFormat(TextInputFormat.class);
        lu.setOutputKeyClass(Text.class);
        lu.setOutputValueClass(LongWritable.class);
        lu.setMapperClass(TotalTimespentPerTerm.class);
        lu.setCombinerClass(TotalTimespentPerTerm.class);
        lu.setReducerClass(TotalTimespentPerTerm.class);
        props = System.getProperties();
        for (Map.Entry<Object,Object> entry : props.entrySet()) {
            lu.set((String)entry.getKey(), (String)entry.getValue());
        }
        FileInputFormat.addInputPath(lu, new Path(inputDir + "/page_views"));
        FileOutputFormat.setOutputPath(lu, new Path(outputDir + "/total_timespent_per_term"));
        lu.setNumReduceTasks(Integer.parseInt(parallel));
        Job loadUsers = new Job(lu);

        JobConf join = new JobConf(L12.class);
        join.setJobName("L12 Find Queries Per Action");
        join.setInputFormat(TextInputFormat.class);
        join.setOutputKeyClass(Text.class);
        join.setOutputValueClass(LongWritable.class);
        join.setMapperClass(QueriesPerAction.class);
        join.setCombinerClass(QueriesPerAction.class);
        join.setReducerClass(QueriesPerAction.class);
        props = System.getProperties();
        for (Map.Entry<Object,Object> entry : props.entrySet()) {
            join.set((String)entry.getKey(), (String)entry.getValue());
        }
        FileInputFormat.addInputPath(join, new Path(inputDir + "/page_views"));
        FileOutputFormat.setOutputPath(join, new Path(outputDir + "/queries_per_action"));
        join.setNumReduceTasks(Integer.parseInt(parallel));
        Job joinJob = new Job(join);

        JobControl jc = new JobControl("L12 join");
        jc.addJob(loadPages);
        jc.addJob(loadUsers);
        jc.addJob(joinJob);

        new Thread(jc).start();
   
        int i = 0;
        while(!jc.allFinished()){
            ArrayList<Job> failures = jc.getFailedJobs();
            if (failures != null && failures.size() > 0) {
                for (Job failure : failures) {
                    System.err.println(failure.getMessage());
                }
                break;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {}

            if (i % 10000 == 0) {
                System.out.println("Running jobs");
                ArrayList<Job> running = jc.getRunningJobs();
                if (running != null && running.size() > 0) {
                    for (Job r : running) {
                        System.out.println(r.getJobName());
                    }
                }
                System.out.println("Ready jobs");
                ArrayList<Job> ready = jc.getReadyJobs();
                if (ready != null && ready.size() > 0) {
                    for (Job r : ready) {
                        System.out.println(r.getJobName());
                    }
                }
                System.out.println("Waiting jobs");
                ArrayList<Job> waiting = jc.getWaitingJobs();
                if (waiting != null && waiting.size() > 0) {
                    for (Job r : ready) {
                        System.out.println(r.getJobName());
                    }
                }
                System.out.println("Successful jobs");
                ArrayList<Job> success = jc.getSuccessfulJobs();
                if (success != null && success.size() > 0) {
                    for (Job r : ready) {
                        System.out.println(r.getJobName());
                    }
                }
            }
            i++;
        }
        ArrayList<Job> failures = jc.getFailedJobs();
        if (failures != null && failures.size() > 0) {
            for (Job failure : failures) {
                System.err.println(failure.getMessage());
            }
        }
        jc.stop();
    }

}
