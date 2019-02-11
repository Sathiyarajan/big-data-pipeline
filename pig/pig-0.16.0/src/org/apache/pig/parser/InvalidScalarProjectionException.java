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

package org.apache.pig.parser;

import org.antlr.runtime.IntStream;
import org.apache.pig.newplan.logical.expression.ScalarExpression;
import org.apache.pig.newplan.logical.relational.LogicalRelationalOperator;

public class InvalidScalarProjectionException extends PigRecognitionException {
    private static final long serialVersionUID = 1L;
    
    private ScalarExpression scalarExpr;
    private String msg;
    
    public InvalidScalarProjectionException(IntStream input, SourceLocation loc,
            ScalarExpression expr) {
        super( input, loc );
        this.scalarExpr = expr;
        this.msg = null;
    }

    /**
     * Constructor that takes an additional string to be concatenated 
     *  with the exception message 
     */
    public InvalidScalarProjectionException(IntStream input, SourceLocation loc,
            ScalarExpression expr, String msg) {
        super( input, loc );
        this.scalarExpr = expr;
        this.msg = new String(msg);
    }
    
    public String toString() {
        return msgHeader() + "Invalid scalar projection: " + 
            ((LogicalRelationalOperator)scalarExpr.getImplicitReferencedOperator()).getAlias() + 
            ((msg!=null) ? msg : ""); 
    }

}
