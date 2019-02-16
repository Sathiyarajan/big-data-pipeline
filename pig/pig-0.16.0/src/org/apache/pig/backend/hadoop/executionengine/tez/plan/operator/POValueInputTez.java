/**
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

package org.apache.pig.backend.hadoop.executionengine.tez.plan.operator;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.JobControlCompiler;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.POStatus;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.Result;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhyPlanVisitor;
import org.apache.pig.backend.hadoop.executionengine.tez.runtime.TezInput;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.VisitorException;
import org.apache.tez.runtime.api.LogicalInput;
import org.apache.tez.runtime.api.Reader;
import org.apache.tez.runtime.library.api.KeyValueReader;
import org.apache.tez.runtime.library.api.KeyValuesReader;

/**
 * POValueInputTez is used read tuples from a Tez Intermediate output from a 1-1
 * edge
 */
public class POValueInputTez extends PhysicalOperator implements TezInput {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(POValueInputTez.class);
    private String inputKey;
    private transient boolean finished = false;
    private transient Configuration conf;
    // TODO Change this to value only reader after implementing
    // value only input output
    private transient KeyValueReader reader;
    private transient KeyValuesReader shuffleReader;
    private transient boolean shuffleInput;
    private transient boolean hasNext;
    private transient Boolean hasFirstRecord;

    public POValueInputTez(OperatorKey k) {
        super(k);
    }

    @Override
    public String[] getTezInputs() {
        return new String[] { inputKey };
    }

    @Override
    public void replaceInput(String oldInputKey, String newInputKey) {
        if (oldInputKey.equals(inputKey)) {
            inputKey = newInputKey;
        }
    }

    @Override
    public void addInputsToSkip(Set<String> inputsToSkip) {
    }

    @Override
    public void attachInputs(Map<String, LogicalInput> inputs,
            Configuration conf)
            throws ExecException {
        this.conf = conf;
        LogicalInput input = inputs.get(inputKey);
        if (input == null) {
            throw new ExecException("Input from vertex " + inputKey + " is missing");
        }

        try {
            Reader r = input.getReader();
            if (r instanceof KeyValueReader) {
                reader = (KeyValueReader) r;
                // Force input fetch
                hasFirstRecord = reader.next();
            } else {
                shuffleInput = true;
                shuffleReader = (KeyValuesReader) r;
                hasNext = shuffleReader.next();
            }
            LOG.info("Attached input from vertex " + inputKey + " : input=" + input + ", reader=" + r);
        } catch (Exception e) {
            throw new ExecException(e);
        }
    }

    @Override
    public Result getNextTuple() throws ExecException {
        try {
            if (finished) {
                return RESULT_EOP;
            }
            if (shuffleInput) {
                while (hasNext) {
                    if (shuffleReader.getCurrentValues().iterator().hasNext()) {
                        Tuple origTuple = (Tuple)shuffleReader.getCurrentValues().iterator().next();
                        Tuple copy = mTupleFactory.newTuple(origTuple.getAll());
                        return new Result(POStatus.STATUS_OK, copy);
                    }
                    hasNext = shuffleReader.next();
                }
            } else {
                if (hasFirstRecord != null) {
                    if (hasFirstRecord) {
                        hasFirstRecord = null;
                        Tuple origTuple = (Tuple) reader.getCurrentValue();
                        Tuple copy = mTupleFactory.newTuple(origTuple.getAll());
                        return new Result(POStatus.STATUS_OK, copy);
                    }
                    hasFirstRecord = null;
                } else {
                    while (reader.next()) {
                        Tuple origTuple = (Tuple) reader.getCurrentValue();
                        Tuple copy = mTupleFactory.newTuple(origTuple.getAll());
                        return new Result(POStatus.STATUS_OK, copy);
                    }
                }
            }
            finished = true;
            // For certain operators (such as STREAM), we could still have some work
            // to do even after seeing the last input. These operators set a flag that
            // says all input has been sent and to run the pipeline one more time.
            if (Boolean.valueOf(conf.get(JobControlCompiler.END_OF_INP_IN_MAP, "false"))) {
                this.parentPlan.endOfAllInput = true;
            }
            return RESULT_EOP;
        } catch (IOException e) {
            throw new ExecException(e);
        }
    }

    public void setInputKey(String inputKey) {
        this.inputKey = inputKey;
    }

    @Override
    public Tuple illustratorMarkup(Object in, Object out, int eqClassIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void visit(PhyPlanVisitor v) throws VisitorException {
        v.visit(this);
    }

    @Override
    public boolean supportsMultipleInputs() {
        return false;
    }

    @Override
    public boolean supportsMultipleOutputs() {
        return false;
    }

    @Override
    public String name() {
        return "POValueInputTez - " + mKey.toString() + "\t<-\t " + inputKey;
    }
}
