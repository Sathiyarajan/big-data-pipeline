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
package org.apache.pig.piggybank.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceSchema.ResourceFieldSchema;
import org.apache.pig.StoreFunc;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.util.UDFContext;
import org.apache.pig.impl.util.Utils;
import org.joda.time.DateTime;

public class DBStorage extends StoreFunc {
  private final Log log = LogFactory.getLog(getClass());

  private PreparedStatement ps;
  private Connection con;
  private String jdbcURL;
  private String user;
  private String pass;
  private int batchSize;
  private int count = 0;
  private String insertQuery;

  //We want to store the schema if possible so that we can try to deal with nulls.
  protected ResourceSchema schema = null;
  private String udfcSignature = null;
  private static final String SCHEMA_SIGNATURE = "pig.dbstorage.schema";

  public DBStorage(String driver, String jdbcURL, String insertQuery) {
    this(driver, jdbcURL, null, null, insertQuery, "100");
  }

  public DBStorage(String driver, String jdbcURL, String user, String pass,
      String insertQuery) throws SQLException {
    this(driver, jdbcURL, user, pass, insertQuery, "100");
  }

  public DBStorage(String driver, String jdbcURL, String user, String pass,
      String insertQuery, String batchSize) throws RuntimeException {
    log.debug("DBStorage(" + driver + "," + jdbcURL + "," + user + ",XXXX,"
        + insertQuery + ")");
    try {
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      log.error("can't load DB driver:" + driver, e);
      throw new RuntimeException("Can't load DB Driver", e);
    }
    this.jdbcURL = jdbcURL;
    this.user = user;
    this.pass = pass;
    this.insertQuery = insertQuery;
    this.batchSize = Integer.parseInt(batchSize);
  }

  /**
   * Write the tuple to Database directly here.
   */
  @Override
  public void putNext(Tuple tuple) throws IOException {
    int sqlPos = 1;
    try {
      int size = tuple.size();
      for (int i = 0; i < size; i++) {
        try {
          Object field = tuple.get(i);
          switch (DataType.findType(field)) {
          case DataType.NULL:
            //Default to varchar
            int nullSqlType = java.sql.Types.VARCHAR;
            if (schema != null) {
                ResourceFieldSchema fs = schema.getFields()[i];
                nullSqlType = sqlDataTypeFromPigDataType(fs.getType());
            }
            ps.setNull(sqlPos, nullSqlType);
            sqlPos++;
            break;

          case DataType.BOOLEAN:
            ps.setBoolean(sqlPos, (Boolean) field);
            sqlPos++;
            break;

          case DataType.INTEGER:
            ps.setInt(sqlPos, (Integer) field);
            sqlPos++;
            break;

          case DataType.LONG:
            ps.setLong(sqlPos, (Long) field);
            sqlPos++;
            break;

          case DataType.FLOAT:
            ps.setFloat(sqlPos, (Float) field);
            sqlPos++;
            break;

          case DataType.DOUBLE:
            ps.setDouble(sqlPos, (Double) field);
            sqlPos++;
            break;

          case DataType.DATETIME:
            ps.setTimestamp(sqlPos, new Timestamp(((DateTime) field).getMillis()));
            sqlPos++;
            break;

          case DataType.BYTEARRAY:
            byte[] b = ((DataByteArray) field).get();
            ps.setBytes(sqlPos, b);

            sqlPos++;
            break;
          case DataType.CHARARRAY:
            ps.setString(sqlPos, (String) field);
            sqlPos++;
            break;
          case DataType.BYTE:
            ps.setByte(sqlPos, (Byte) field);
            sqlPos++;
            break;

          case DataType.MAP:
          case DataType.TUPLE:
          case DataType.BAG:
            throw new RuntimeException("Cannot store a non-flat tuple "
                + "using DbStorage");

          default:
            throw new RuntimeException("Unknown datatype "
                + DataType.findType(field));

          }

        } catch (ExecException ee) {
          throw new RuntimeException(ee);
        }

      }
      ps.addBatch();
      count++;
      if (count > batchSize) {
        count = 0;
        ps.executeBatch();
        ps.clearBatch();
        ps.clearParameters();
      }
    } catch (SQLException e) {
      try {
        log.error("Unable to insert record:" + tuple.toDelimitedString("\t"), e);
      } catch (ExecException ee) {
        // do nothing
      }
      if (e.getErrorCode() == 1366) {
        // errors that come due to utf-8 character encoding
        // ignore these kind of errors TODO: Temporary fix - need to find a
        // better way of handling them in the argument statement itself
      } else {
        throw new RuntimeException("JDBC error", e);
      }
    }
  }

  protected int sqlDataTypeFromPigDataType(byte pigDataType) {
      switch(pigDataType) {
      case DataType.INTEGER:
          return java.sql.Types.INTEGER;
      case DataType.LONG:
          return java.sql.Types.BIGINT;
      case DataType.FLOAT:
          return java.sql.Types.FLOAT;
      case DataType.DOUBLE:
          return java.sql.Types.DOUBLE;
      case DataType.BOOLEAN:
          return java.sql.Types.BOOLEAN;
      case DataType.DATETIME:
          return java.sql.Types.DATE;
      case DataType.BYTEARRAY:
      case DataType.CHARARRAY:
      case DataType.BYTE:
          return java.sql.Types.VARCHAR;
      default:
          log.warn("Can not find SQL data type for " + pigDataType + " returning VARCHAR");
          return java.sql.Types.VARCHAR;
      }
  }

  class MyDBOutputFormat extends OutputFormat<NullWritable, NullWritable> {

    @Override
    public void checkOutputSpecs(JobContext context) throws IOException,
        InterruptedException {
      // IGNORE
    }

    @Override
    public OutputCommitter getOutputCommitter(TaskAttemptContext context)
        throws IOException, InterruptedException {
      return new OutputCommitter() {

        @Override
        public void abortTask(TaskAttemptContext context) throws IOException {
          try {
            if (ps != null) {
              ps.close();
            }
            if (con != null) {
              con.rollback();
              con.close();
            }
          } catch (SQLException sqe) {
            throw new IOException(sqe);
          }
        }

        @Override
        public void commitTask(TaskAttemptContext context) throws IOException {
          if (ps != null) {
            try {
              ps.executeBatch();
              con.commit();
              ps.close();
              con.close();
              ps = null;
              con = null;
            } catch (SQLException e) {
              log.error("ps.close", e);
              throw new IOException("JDBC Error", e);
            }
          }
        }

        @Override
        public boolean needsTaskCommit(TaskAttemptContext context)
            throws IOException {
          return true;
        }

        @Override
        public void cleanupJob(JobContext context) throws IOException {
          // IGNORE
        }

        @Override
        public void setupJob(JobContext context) throws IOException {
          // IGNORE
        }

        @Override
        public void setupTask(TaskAttemptContext context) throws IOException {
          // IGNORE
        }
      };
    }

    @Override
    public RecordWriter<NullWritable, NullWritable> getRecordWriter(
      TaskAttemptContext context) throws IOException, InterruptedException {
      // We don't use a record writer to write to database
      return new RecordWriter<NullWritable, NullWritable>() {
          @Override
          public void close(TaskAttemptContext context) {
              // Noop
          }
          @Override
          public void write(NullWritable k, NullWritable v) {
              // Noop
          }
      };
    }

  }

  @SuppressWarnings("unchecked")
  @Override
  public OutputFormat getOutputFormat()
      throws IOException {
    return new MyDBOutputFormat();
  }

  /**
   * Initialise the database connection and prepared statement here.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void prepareToWrite(RecordWriter writer)
      throws IOException {
    ps = null;
    con = null;
    if (insertQuery == null) {
      throw new IOException("SQL Insert command not specified");
    }
    try {
      if (user == null || pass == null) {
        con = DriverManager.getConnection(jdbcURL);
      } else {
        con = DriverManager.getConnection(jdbcURL, user, pass);
      }
      con.setAutoCommit(false);
      ps = con.prepareStatement(insertQuery);
    } catch (SQLException e) {
      log.error("Unable to connect to JDBC @" + jdbcURL);
      throw new IOException("JDBC Error", e);
    }
    count = 0;

    // Try to get the schema from the UDFContext object.
    UDFContext udfc = UDFContext.getUDFContext();
    Properties p =
        udfc.getUDFProperties(this.getClass(), new String[]{udfcSignature});
    String strSchema = p.getProperty(SCHEMA_SIGNATURE);
    if (strSchema != null) {
        // Parse the schema from the string stored in the properties object.
        schema = new ResourceSchema(Utils.getSchemaFromString(strSchema));
    }
  }

  @Override
  public void setStoreLocation(String location, Job job) throws IOException {
    // IGNORE since we are writing records to DB.
  }

  @Override
  public void setStoreFuncUDFContextSignature(String signature) {
      // store the signature so we can use it later
      udfcSignature = signature;
  }

  @Override
  public void checkSchema(ResourceSchema s) throws IOException {
      // We won't really check the schema here, we'll store it in our
      // UDFContext properties object so we have it when we need it on the
      // backend

      UDFContext udfc = UDFContext.getUDFContext();
      Properties p =
          udfc.getUDFProperties(this.getClass(), new String[]{udfcSignature});
      p.setProperty(SCHEMA_SIGNATURE, s.toString());
  }

  @Override
  public Boolean supportsParallelWriteToStoreLocation() {
    return false;
  }

}
