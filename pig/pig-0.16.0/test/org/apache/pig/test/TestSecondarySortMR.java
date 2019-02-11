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
package org.apache.pig.test;

import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.SecondaryKeyOptimizerMR;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.plans.MROperPlan;
import org.apache.pig.backend.hadoop.executionengine.optimizer.SecondaryKeyOptimizer;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhysicalPlan;
import org.apache.pig.impl.plan.VisitorException;

public class TestSecondarySortMR extends TestSecondarySort {

    public TestSecondarySortMR() {
        super();
    }

    @Override
    public MiniGenericCluster getCluster() {
        return MiniGenericCluster.buildCluster(MiniGenericCluster.EXECTYPE_MR);
    }

    @Override
    public SecondaryKeyOptimizer visitSecondaryKeyOptimizer(String query)
            throws Exception, VisitorException {
        PhysicalPlan pp = Util.buildPp(pigServer, query);
        MROperPlan mrPlan = Util.buildMRPlan(pp, pc);

        SecondaryKeyOptimizerMR so = new SecondaryKeyOptimizerMR(mrPlan);
        so.visit();
        return so;
    }

}

