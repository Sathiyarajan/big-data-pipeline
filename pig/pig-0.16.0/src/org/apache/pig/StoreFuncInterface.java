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
package org.apache.pig;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;

import org.apache.pig.classification.InterfaceAudience;
import org.apache.pig.classification.InterfaceStability;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.util.UDFContext;


/**
* StoreFuncs take records from Pig's processing and store them into a data store.  Most frequently
* this is an HDFS file, but it could also be an HBase instance, RDBMS, etc.
*/
@InterfaceAudience.Public
@InterfaceStability.Stable
public interface StoreFuncInterface {

    /**
     * This method is called by the Pig runtime in the front end to convert the
     * output location to an absolute path if the location is relative. The
     * StoreFuncInterface implementation is free to choose how it converts a relative 
     * location to an absolute location since this may depend on what the location
     * string represent (hdfs path or some other data source). 
     * The static method {@link LoadFunc#getAbsolutePath} provides a default 
     * implementation for hdfs and hadoop local file system and it can be used
     * to implement this method.  
     * 
     * @param location location as provided in the "store" statement of the script
     * @param curDir the current working direction based on any "cd" statements
     * in the script before the "store" statement. If there are no "cd" statements
     * in the script, this would be the home directory - 
     * <pre>/user/<username> </pre>
     * @return the absolute location based on the arguments passed
     * @throws IOException if the conversion is not possible
     */
    String relToAbsPathForStoreLocation(String location, Path curDir) throws IOException;

    /**
     * Return the OutputFormat associated with StoreFuncInterface.  This will be called
     * on the front end during planning and on the backend during
     * execution. 
     * @return the {@link OutputFormat} associated with StoreFuncInterface
     * @throws IOException if an exception occurs while constructing the 
     * OutputFormat
     *
     */
    OutputFormat getOutputFormat() throws IOException;

    /**
     * Communicate to the storer the location where the data needs to be stored.  
     * The location string passed to the {@link StoreFuncInterface} here is the 
     * return value of {@link StoreFuncInterface#relToAbsPathForStoreLocation(String, Path)}
     * This method will be called in the frontend and backend multiple times. Implementations
     * should bear in mind that this method is called multiple times and should
     * ensure there are no inconsistent side effects due to the multiple calls.
     * {@link #checkSchema(ResourceSchema)} will be called before any call to
     * {@link #setStoreLocation(String, Job)}.
     * 

     * @param location Location returned by 
     * {@link StoreFuncInterface#relToAbsPathForStoreLocation(String, Path)}
     * @param job The {@link Job} object
     * @throws IOException if the location is not valid.
     */
    void setStoreLocation(String location, Job job) throws IOException;
 
    /**
     * Set the schema for data to be stored.  This will be called on the
     * front end during planning if the store is associated with a schema.
     * A Store function should implement this function to
     * check that a given schema is acceptable to it.  For example, it
     * can check that the correct partition keys are included;
     * a storage function to be written directly to an OutputFormat can
     * make sure the schema will translate in a well defined way.  
     * @param s to be checked
     * @throws IOException if this schema is not acceptable.  It should include
     * a detailed error message indicating what is wrong with the schema.
     */
    void checkSchema(ResourceSchema s) throws IOException;

    /**
     * Initialize StoreFuncInterface to write data.  This will be called during
     * execution before the call to putNext.
     * @param writer RecordWriter to use.
     * @throws IOException if an exception occurs during initialization
     */
    void prepareToWrite(RecordWriter writer) throws IOException;

    /**
     * Write a tuple to the data store.
     * @param t the tuple to store.
     * @throws IOException if an exception occurs during the write
     */
    void putNext(Tuple t) throws IOException;
    
    /**
     * This method will be called by Pig both in the front end and back end to
     * pass a unique signature to the {@link StoreFuncInterface} which it can use to store
     * information in the {@link UDFContext} which it needs to store between
     * various method invocations in the front end and back end.  This is necessary
     * because in a Pig Latin script with multiple stores, the different
     * instances of store functions need to be able to find their (and only their)
     * data in the UDFContext object.
     * @param signature a unique signature to identify this StoreFuncInterface
     */
    public void setStoreFuncUDFContextSignature(String signature);

    /**
     * This method will be called by Pig if the job which contains this store
     * fails. Implementations can clean up output locations in this method to
     * ensure that no incorrect/incomplete results are left in the output location
     * @param location Location returned by 
     * {@link StoreFuncInterface#relToAbsPathForStoreLocation(String, Path)}
     * @param job The {@link Job} object - this should be used only to obtain 
     * cluster properties through {@link Job#getConfiguration()} and not to set/query
     * any runtime job information. 
     */
    void cleanupOnFailure(String location, Job job) throws IOException;

    /**
     * This method will be called by Pig if the job which contains this store
     * is successful, and some cleanup of intermediate resources is required.
     * Implementations can clean up output locations in this method to
     * ensure that no incorrect/incomplete results are left in the output location
     * @param location Location returned by 
     * {@link StoreFuncInterface#relToAbsPathForStoreLocation(String, Path)}
     * @param job The {@link Job} object - this should be used only to obtain 
     * cluster properties through {@link Job#getConfiguration()} and not to set/query
     * any runtime job information. 
     */
    void cleanupOnSuccess(String location, Job job) throws IOException;
}
