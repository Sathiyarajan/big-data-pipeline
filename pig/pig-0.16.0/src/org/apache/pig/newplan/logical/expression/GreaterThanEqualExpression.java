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
package org.apache.pig.newplan.logical.expression;

import org.apache.pig.data.DataType;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.newplan.Operator;
import org.apache.pig.newplan.OperatorPlan;
import org.apache.pig.newplan.PlanVisitor;
import org.apache.pig.newplan.logical.relational.LogicalSchema;
import org.apache.pig.parser.SourceLocation;

public class GreaterThanEqualExpression extends BinaryExpression {

    /**
     * Will add this operator to the plan and connect it to the 
     * left and right hand side operators.
     * @param plan plan this operator is part of
     * @param lhs expression on its left hand side
     * @param rhs expression on its right hand side
     */
    public GreaterThanEqualExpression(OperatorPlan plan,
                           LogicalExpression lhs,
                           LogicalExpression rhs) {
        super("GreaterThanEqual", plan, lhs, rhs);
    }

    /**
     * @link org.apache.pig.newplan.Operator#accept(org.apache.pig.newplan.PlanVisitor)
     */
    @Override
    public void accept(PlanVisitor v) throws FrontendException {
        if (!(v instanceof LogicalExpressionVisitor)) {
            throw new FrontendException("Expected LogicalExpressionVisitor", 2222);
        }
        ((LogicalExpressionVisitor)v).visit(this);
    }
    
    @Override
    public boolean isEqual(Operator other) throws FrontendException {
        if (other != null && other instanceof GreaterThanEqualExpression) {
            GreaterThanEqualExpression eo = (GreaterThanEqualExpression)other;
            return eo.getLhs().isEqual(getLhs()) && eo.getRhs().isEqual(getRhs());
        } else {
            return false;
        }
    }
    
    @Override
    public LogicalSchema.LogicalFieldSchema getFieldSchema() throws FrontendException {
        if (fieldSchema!=null)
            return fieldSchema;
        fieldSchema = new LogicalSchema.LogicalFieldSchema(null, null, DataType.BOOLEAN);
        uidOnlyFieldSchema = fieldSchema.mergeUid(uidOnlyFieldSchema);
        return fieldSchema;
    }

    @Override
    public LogicalExpression deepCopy(LogicalExpressionPlan lgExpPlan) throws FrontendException {
        LogicalExpression copy = new GreaterThanEqualExpression( 
                lgExpPlan,
                this.getLhs().deepCopy(lgExpPlan),
                this.getRhs().deepCopy(lgExpPlan));
        copy.setLocation( new SourceLocation( location ) );
        return copy;
    }
 
}
