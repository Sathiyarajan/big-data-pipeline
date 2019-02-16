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
package org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans;

import java.util.HashSet;
import java.util.Set;

import org.apache.pig.LoadFunc;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.POCast;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.POUserFunc;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.relationalOperators.POLoad;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.relationalOperators.POStore;
import org.apache.pig.impl.PigContext;
import org.apache.pig.impl.plan.DepthFirstWalker;
import org.apache.pig.impl.plan.VisitorException;

public class UdfCacheShipFilesVisitor extends PhyPlanVisitor {
    private Set<String> cacheFiles = new HashSet<String>();
    private Set<String> shipFiles = new HashSet<String>();

    public UdfCacheShipFilesVisitor(PhysicalPlan plan) {
        super(plan, new DepthFirstWalker<PhysicalOperator, PhysicalPlan>(plan));
    }

    @Override
    public void visitLoad(POLoad ld) throws VisitorException {
        if (ld.getCacheFiles() != null) {
            cacheFiles.addAll(ld.getCacheFiles());
        }
        if (ld.getShipFiles() != null) {
            shipFiles.addAll(ld.getShipFiles());
        }
    }

    @Override
    public void visitStore(POStore st) throws VisitorException {
        if (st.getCacheFiles() != null) {
            cacheFiles.addAll(st.getCacheFiles());
        }
        if (st.getShipFiles() != null) {
            shipFiles.addAll(st.getShipFiles());
        }
    }

    public void visitUserFunc(POUserFunc udf) throws VisitorException {
        if (udf.getCacheFiles() != null) {
            cacheFiles.addAll(udf.getCacheFiles());
        }
        if (udf.getShipFiles() != null) {
            shipFiles.addAll(udf.getShipFiles());
        }
    }

    @Override
    public void visitCast(POCast cast) {
        if (cast.getFuncSpec()!=null) {
            Object obj = PigContext.instantiateFuncFromSpec(cast.getFuncSpec());
            if (obj instanceof LoadFunc) {
                LoadFunc loadFunc = (LoadFunc)obj;
                if (loadFunc.getCacheFiles()!=null) {
                    cacheFiles.addAll(loadFunc.getCacheFiles());
                }
                if (loadFunc.getShipFiles()!=null) {
                    shipFiles.addAll(loadFunc.getShipFiles());
                }
            }
        }
    }

    public Set<String> getCacheFiles() {
        return cacheFiles;
    }

    public Set<String> getShipFiles() {
        return shipFiles;
    }
}
