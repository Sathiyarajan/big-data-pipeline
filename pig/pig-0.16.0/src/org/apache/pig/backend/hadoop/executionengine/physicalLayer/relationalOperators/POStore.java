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
package org.apache.pig.backend.hadoop.executionengine.physicalLayer.relationalOperators;

import java.io.IOException;
import java.util.List;

import org.apache.pig.PigException;
import org.apache.pig.SortInfo;
import org.apache.pig.StoreFuncInterface;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.MapReducePOStoreImpl;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.POStatus;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.Result;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhyPlanVisitor;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.PigContext;
import org.apache.pig.impl.io.FileSpec;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.VisitorException;
import org.apache.pig.pen.util.ExampleTuple;
import org.apache.pig.pen.util.LineageTracer;
import org.apache.pig.tools.pigstats.PigStatsUtil;

/**
 * The store operator which is used in two ways:
 * 1) As a local operator it can be used to store files
 * 2) In the Map Reduce setting, it is used to create jobs
 *    from MapReduce operators which keep the loads and
 *    stores in the Map and Reduce Plans till the job is created
 *
 */
public class POStore extends PhysicalOperator {

    private static final long serialVersionUID = 1L;
    transient private StoreFuncInterface storer;
    transient private StoreFuncDecorator sDecorator;
    transient private POStoreImpl impl;
    transient private String counterName = null;
    private FileSpec sFile;
    private Schema schema;

    // flag to distinguish user stores from MRCompiler stores.
    private boolean isTmpStore;

    // flag to distinguish single store from multiquery store.
    private boolean isMultiStore;

    // flag to indicate if the custom counter should be disabled.
    private boolean disableCounter = false;

    // the index of multiquery store to track counters
    private int index;

    // If we know how to reload the store, here's how. The lFile
    // FileSpec is set in PigServer.postProcess. It can be used to
    // reload this store, if the optimizer has the need.
    private FileSpec lFile;

    // if the predecessor of store is Sort (order by)
    // then sortInfo will have information of the sort
    // column names and the asc/dsc info
    private SortInfo sortInfo;

    private String signature;

    private transient List<String> cacheFiles = null;
    private transient List<String> shipFiles = null;

    public POStore(OperatorKey k) {
        this(k, -1, null);
    }

    public POStore(OperatorKey k, int rp) {
        this(k, rp, null);
    }

    public POStore(OperatorKey k, int rp, List<PhysicalOperator> inp) {
        super(k, rp, inp);
    }

    public POStore(POStore copy) {
        super(copy);
        this.sFile = copy.sFile;
        this.schema = copy.schema;
        this.isTmpStore = copy.isTmpStore;
        this.isMultiStore = copy.isMultiStore;
        this.disableCounter = copy.disableCounter;
        this.index = copy.index;
        this.lFile = copy.lFile;
        this.sortInfo = copy.sortInfo;
        this.signature = copy.signature;
    }

    /**
     * Set up the storer
     * @throws IOException
     */
    public void setUp() throws IOException{
        if (impl != null) {
            try{
                storer = impl.createStoreFunc(this);
                if (!isTmpStore && !disableCounter && impl instanceof MapReducePOStoreImpl) {
                    counterName = PigStatsUtil.getMultiStoreCounterName(this);
                    if (counterName != null) {
                        // Create the counter. This is needed because
                        // incrCounter() may never be called in case of empty
                        // file.
                        ((MapReducePOStoreImpl) impl).incrRecordCounter(counterName, 0);
                    }
                }
            }catch (IOException ioe) {
                int errCode = 2081;
                String msg = "Unable to setup the store function.";
                throw new ExecException(msg, errCode, PigException.BUG, ioe);
            }
        }
    }

    /**
     * Called at the end of processing for clean up.
     * @throws IOException
     */
    public void tearDown() throws IOException{
        if (impl != null) {
            impl.tearDown();
        }
   }

    /**
     * To perform cleanup when there is an error.
     * @throws IOException
     */
    public void cleanUp() throws IOException{
        if (impl != null) {
            impl.cleanUp();
        }
    }

    @Override
    public Result getNextTuple() throws ExecException {
        Result res = processInput();
        try {
            switch (res.returnStatus) {
            case POStatus.STATUS_OK:
                if (illustrator == null) {
                    sDecorator.putNext((Tuple) res.result);
                } else
                    illustratorMarkup(res.result, res.result, 0);
                res = RESULT_EMPTY;

                if (counterName != null) {
                    ((MapReducePOStoreImpl) impl).incrRecordCounter(counterName, 1);
                }
                break;
            case POStatus.STATUS_EOP:
                break;
            case POStatus.STATUS_ERR:
            case POStatus.STATUS_NULL:
            default:
                break;
            }
        } catch (IOException ioe) {
            int errCode = 2135;
            String msg = "Received error from store function." + ioe.getMessage();
            throw new ExecException(msg, errCode, ioe);
        }
        return res;
    }

    @Override
    public String name() {
        return (sFile != null) ? getAliasString() + "Store" + "("
                + sFile.toString() + ")" + " - " + mKey.toString()
                : getAliasString() + "Store" + "(" + "DummyFil:DummyLdr" + ")"
                        + " - " + mKey.toString();
    }

    @Override
    public boolean supportsMultipleInputs() {
        return false;
    }

    @Override
    public boolean supportsMultipleOutputs() {
        return true;
    }

    @Override
    public void visit(PhyPlanVisitor v) throws VisitorException {
        v.visitStore(this);
    }

    public FileSpec getSFile() {
        return sFile;
    }

    public void setSFile(FileSpec sFile) {
        this.sFile = sFile;
        storer = null;
    }

    public void setInputSpec(FileSpec lFile) {
        this.lFile = lFile;
    }

    public FileSpec getInputSpec() {
        return lFile;
    }

    public void setIsTmpStore(boolean tmp) {
        isTmpStore = tmp;
    }

    public boolean isTmpStore() {
        return isTmpStore;
    }

    public void setStoreImpl(POStoreImpl impl) {
        this.impl = impl;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        return schema;
    }


    public StoreFuncInterface getStoreFunc() {
        if (storer == null) {
            storer = (StoreFuncInterface) PigContext.instantiateFuncFromSpec(sFile.getFuncSpec());
            storer.setStoreFuncUDFContextSignature(signature);
        }
        if (sDecorator == null) {
            // Init the Decorator we use for writing Tuples
            setStoreFuncDecorator(new StoreFuncDecorator(storer, signature));
        }
        return storer;
    }

    void setStoreFuncDecorator(StoreFuncDecorator sDecorator) {
        this.sDecorator = sDecorator;
    }

    /**
     *
     * @return The {@link StoreFuncDecorator} used to write Tuples
     */
    public StoreFuncDecorator getStoreFuncDecorator() {
        return sDecorator;
    }

    /**
     * @param sortInfo the sortInfo to set
     */
    public void setSortInfo(SortInfo sortInfo) {
        this.sortInfo = sortInfo;
    }

    /**
     * @return the sortInfo
     */
    public SortInfo getSortInfo() {
        return sortInfo;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setMultiStore(boolean isMultiStore) {
        this.isMultiStore = isMultiStore;
    }

    public boolean isMultiStore() {
        return isMultiStore;
    }

    @Override
    public Tuple illustratorMarkup(Object in, Object out, int eqClassIndex) {
        if(illustrator != null) {
            ExampleTuple tIn = (ExampleTuple) in;
            LineageTracer lineage = illustrator.getLineage();
            lineage.insert(tIn);
            if (!isTmpStore)
                illustrator.getEquivalenceClasses().get(eqClassIndex).add(tIn);
            illustrator.addData((Tuple) out);
        }
        return (Tuple) out;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setDisableCounter(boolean disableCounter) {
        this.disableCounter = disableCounter;
    }

    public boolean disableCounter() {
        return disableCounter;
    }

    public void setStoreFunc(StoreFuncInterface storeFunc) {
        this.storer = storeFunc;
    }

    public List<String> getCacheFiles() {
        return cacheFiles;
    }

    public void setCacheFiles(List<String> cf) {
        cacheFiles = cf;
    }

    public List<String> getShipFiles() {
        return shipFiles;
    }

    public void setShipFiles(List<String> sf) {
        shipFiles = sf;
    }
}
