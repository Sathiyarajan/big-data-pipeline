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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
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

public class L15 {

    public static class ReadPageViews extends MapReduceBase
        implements Mapper<LongWritable, Text, Text, Text> {

        public void map(
                LongWritable k,
                Text val,
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {

            // Split the line
            List<Text> fields = Library.splitLine(val, '');
            if (fields.size() != 9) return;

            StringBuffer sb = new StringBuffer();
			sb.append(fields.get(1).toString()); // action
            sb.append('');
			sb.append(fields.get(6).toString()); // estimated_revenue
            sb.append('');
			sb.append(fields.get(2).toString()); // timespent
            sb.append('');
            oc.collect(fields.get(0), new Text(sb.toString()));
        }
    }

    public static class Combiner extends MapReduceBase
        implements Reducer<Text, Text, Text, Text> {

        public void reduce(
                Text key,
                Iterator<Text> iter, 
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {
            HashSet<Text> hash1 = new HashSet<Text>();
            HashSet<Text> hash2 = new HashSet<Text>();
            HashSet<Text> hash3 = new HashSet<Text>();
            int cnt_per_combiner = 0;
            while (iter.hasNext()) {
                List<Text> vals = Library.splitLine(iter.next(), '');
                cnt_per_combiner++;
                try {
					hash1.add(vals.get(0));
					hash2.add(vals.get(1));
					hash3.add(vals.get(2));
				} catch(NumberFormatException nfe) {
				}
			}
			Double rev= new Double(0.0);
			Integer ts=0;
			try {
            for (Text t : hash2) rev += Double.valueOf(t.toString());
            for (Text t : hash3) ts += Integer.valueOf(t.toString());
			} catch (NumberFormatException e) {
			}
			StringBuffer sb = new StringBuffer();
			sb.append((new Integer(hash1.size())).toString());
            sb.append("");
            sb.append(rev.toString());
            sb.append("");
            sb.append(ts.toString());
            sb.append("");
            sb.append(cnt_per_combiner);
            sb.append("");
            oc.collect(key, new Text(sb.toString()));
            reporter.setStatus("OK");
        }
    }
    public static class Group extends MapReduceBase
        implements Reducer<Text, Text, Text, Text> {

        public void reduce(
                Text key,
                Iterator<Text> iter, 
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {
            HashSet<Text> hash1 = new HashSet<Text>();
            HashSet<Text> hash2 = new HashSet<Text>();
            HashSet<Text> hash3 = new HashSet<Text>();
            while (iter.hasNext()) {
				Text line = iter.next();
				List<Text> vals = Library.splitLine(line, '');
				try {
					hash1.add(vals.get(0));
					hash2.add(vals.get(1));
					hash3.add(vals.get(2));
				}catch(NumberFormatException nfe) {
				}
			}

			Integer ts = 0;
			Double rev = new Double(0.0);
			Integer overall_cnt_per_group = new Integer(0);
			for (Text t : hash2)
				rev += Double.valueOf(t.toString());
			for (Text t : hash3)
				ts += Integer.valueOf(t.toString());
			StringBuffer sb = new StringBuffer();
			sb.append((new Integer(hash1.size())).toString());
			sb.append("");
			sb.append(rev.toString());
			sb.append("");
			Double avg = (double) ((Integer.valueOf(ts.toString())) / hash3.size());
			sb.append(avg);
			oc.collect(key, new Text(sb.toString()));
			reporter.setStatus("OK");
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
        JobConf lp = new JobConf(L15.class);
        lp.setJobName("L15 Load Page Views");
        lp.setInputFormat(TextInputFormat.class);
        lp.setOutputKeyClass(Text.class);
        lp.setOutputValueClass(Text.class);
        lp.setMapperClass(ReadPageViews.class);
        lp.setCombinerClass(Combiner.class);
        lp.setReducerClass(Group.class);
        Properties props = System.getProperties();
        for (Map.Entry<Object,Object> entry : props.entrySet()) {
            lp.set((String)entry.getKey(), (String)entry.getValue());
        }
        FileInputFormat.addInputPath(lp, new Path(inputDir + "/page_views"));
        FileOutputFormat.setOutputPath(lp, new Path(outputDir + "/L15out"));
        lp.setNumReduceTasks(Integer.parseInt(parallel));
        Job group = new Job(lp);

        JobControl jc = new JobControl("L15 join");
        jc.addJob(group);

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
