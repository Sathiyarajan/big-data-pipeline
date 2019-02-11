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

public class L17 {

    public static class ReadPageViews extends MapReduceBase
        implements Mapper<LongWritable, Text, Text, Text> {

        public void map(
                LongWritable k,
                Text val,
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {
            List<Text> vals = Library.splitLine(val, '');
            if (vals.size() != 27) return;
            StringBuffer key = new StringBuffer();
            key.append(vals.get(0).toString());
            key.append("");
            key.append(vals.get(1).toString());
            key.append("");
            key.append(vals.get(2).toString());
            key.append("");
            key.append(vals.get(3).toString());
            key.append("");
            key.append(vals.get(4).toString());
            key.append("");
            key.append(vals.get(5).toString());
            key.append("");
            key.append(vals.get(6).toString());
            key.append("");
            key.append(vals.get(9).toString());
            key.append("");
            key.append(vals.get(10).toString());
            key.append("");
            key.append(vals.get(11).toString());
            key.append("");
            key.append(vals.get(12).toString());
            key.append("");
            key.append(vals.get(13).toString());
            key.append("");
            key.append(vals.get(14).toString());
            key.append("");
            key.append(vals.get(15).toString());
            key.append("");
            key.append(vals.get(18).toString());
            key.append("");
            key.append(vals.get(19).toString());
            key.append("");
            key.append(vals.get(20).toString());
            key.append("");
            key.append(vals.get(21).toString());
            key.append("");
            key.append(vals.get(22).toString());
            key.append("");
            key.append(vals.get(23).toString());
            key.append("");
            key.append(vals.get(24).toString());
            
            StringBuffer sb = new StringBuffer();
            sb.append(vals.get(2).toString());
            sb.append("");
            sb.append(vals.get(11).toString());
            sb.append("");
            sb.append(vals.get(20).toString());
            sb.append("");
            sb.append(vals.get(6).toString());
            sb.append("");
            sb.append(vals.get(15).toString());
            sb.append("");
            sb.append(vals.get(24).toString());
            oc.collect(new Text(key.toString()), new Text(sb.toString()));
        }
    }

    public static class Combiner extends MapReduceBase
        implements Reducer<Text, Text, Text, Text> {

        public void reduce(
                Text key,
                Iterator<Text> iter, 
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {
            int tsSum = 0, tsSum1 = 0, tsSum2 = 0, erCnt = 0;
            double erSum = 0.0, erSum1 = 0.0, erSum2=0.0;
            while (iter.hasNext()) {
                List<Text> vals = Library.splitLine(iter.next(), '');
                try {
                    tsSum += Integer.valueOf(vals.get(0).toString());
                } catch (NumberFormatException nfe) {
                }
                try {
                    tsSum1 += Integer.valueOf(vals.get(1).toString());
                } catch (NumberFormatException nfe) {
                }
                try {
                    tsSum2 += Integer.valueOf(vals.get(2).toString());
                } catch (NumberFormatException nfe) {
                }
                try {
                    erSum += Double.valueOf(vals.get(3).toString());
                } catch (NumberFormatException nfe) {
                }
                try {
                    erSum1 += Double.valueOf(vals.get(4).toString());
                } catch (NumberFormatException nfe) {
                }
                try {
                    erSum2 += Double.valueOf(vals.get(5).toString());
                } catch (NumberFormatException nfe) {
                }
                erCnt++;
            }
            StringBuffer sb = new StringBuffer();
            sb.append((new Integer(tsSum)).toString());
            sb.append("");
            sb.append((new Integer(tsSum1)).toString());
            sb.append("");
            sb.append((new Integer(tsSum2)).toString());
            sb.append("");
            sb.append((new Double(erSum)).toString());
            sb.append("");
            sb.append((new Double(erSum1)).toString());
            sb.append("");
            sb.append((new Double(erSum2)).toString());
            sb.append("");
            sb.append((new Integer(erCnt)).toString());
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
            int tsSum = 0, tsSum1 = 0, tsSum2 = 0, erCnt = 0;
            double erSum = 0.0, erSum1 = 0.0, erSum2 = 0.0;
            while (iter.hasNext()) {
                List<Text> vals = Library.splitLine(iter.next(), '');
                try {
                    tsSum += Integer.valueOf(vals.get(0).toString());
                    tsSum1 += Integer.valueOf(vals.get(1).toString());
                    tsSum2 += Integer.valueOf(vals.get(2).toString());
                    erSum += Double.valueOf(vals.get(3).toString());
                    erSum1 += Double.valueOf(vals.get(4).toString());
                    erSum2 += Double.valueOf(vals.get(5).toString());
                    erCnt++;
                } catch (NumberFormatException nfe) {
                }
            }
            double erAvg = erSum / erCnt, erAvg1 = erSum1 / erCnt, erAvg2 = erSum2 / erCnt;
            StringBuffer sb = new StringBuffer();
            sb.append((new Double(tsSum)).toString());
            sb.append("\t");
            sb.append((new Double(tsSum1)).toString());
            sb.append("\t");
            sb.append((new Double(tsSum2)).toString());
            sb.append("\t");
            sb.append((new Double(erAvg)).toString());
            sb.append("\t");
            sb.append((new Double(erAvg1)).toString());
            sb.append("\t");
            sb.append((new Double(erAvg2)).toString());
            oc.collect(null, new Text(sb.toString()));
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
        JobConf lp = new JobConf(L17.class);
        lp.setJobName("L17 Wide group by");
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
        FileInputFormat.addInputPath(lp, new Path(inputDir + "/widegroupbydata"));
        FileOutputFormat.setOutputPath(lp, new Path(outputDir + "/L17out"));
        lp.setNumReduceTasks(Integer.parseInt(parallel));
        Job group = new Job(lp);

        JobControl jc = new JobControl("L17 group by");
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
