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
package org.apache.pig.builtin;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Iterator;

import org.apache.pig.Accumulator;
import org.apache.pig.Algebraic;
import org.apache.pig.EvalFunc;
import org.apache.pig.PigException;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.schema.Schema;


/**
 * This method should never be used directly, use {@link AVG}.
 */
public class BigIntegerAvg extends EvalFunc<BigDecimal> implements Algebraic, Accumulator<BigDecimal> {

    private static TupleFactory mTupleFactory = TupleFactory.getInstance();

    @Override
    public BigDecimal exec(Tuple input) throws IOException {
        try {
            BigInteger sum = sum(input);
            if (sum == null) {
                // either we were handed an empty bag or a bag
                // filled with nulls - return null in this case
                return null;
            }
            BigInteger count = count(input);

            BigDecimal avg = null;
            if (count.compareTo(BigInteger.ZERO) > 0)
                avg = div(sum, count);
            return avg;
        } catch (ExecException ee) {
            throw ee;
        }
    }

    @Override
    public String getInitial() {
        return Initial.class.getName();
    }

    @Override
    public String getIntermed() {
        return Intermediate.class.getName();
    }

    @Override
    public String getFinal() {
        return Final.class.getName();
    }

    static public class Initial extends EvalFunc<Tuple> {
        @Override
        public Tuple exec(Tuple input) throws IOException {
            try {
                Tuple t = mTupleFactory.newTuple(2);
                // input is a bag with one tuple containing
                // the column we are trying to avg on
                DataBag bg = (DataBag) input.get(0);
                BigInteger d = null;
                if (bg.iterator().hasNext()) {
                    Tuple tp = bg.iterator().next();
                    d = (BigInteger)(tp.get(0));
                }
                t.set(0, d);
                if (d != null) {
                    t.set(1, BigInteger.ONE);
                } else {
                    t.set(1, BigInteger.ZERO);
                }
                return t;
            } catch (ExecException ee) {
                throw ee;
            } catch (Exception e) {
                int errCode = 2106;
                String msg = "Error while computing average in " + this.getClass().getSimpleName();
                throw new ExecException(msg, errCode, PigException.BUG, e);
            }

        }
    }

    static public class Intermediate extends EvalFunc<Tuple> {
        @Override
        public Tuple exec(Tuple input) throws IOException {
            try {
                DataBag b = (DataBag)input.get(0);
                return combine(b);
            } catch (ExecException ee) {
                throw ee;
            } catch (Exception e) {
                int errCode = 2106;
                String msg = "Error while computing average in " + this.getClass().getSimpleName();
                throw new ExecException(msg, errCode, PigException.BUG, e);
            }
        }
    }

    static public class Final extends EvalFunc<BigDecimal> {
        @Override
        public BigDecimal exec(Tuple input) throws IOException {
            try {
                DataBag b = (DataBag)input.get(0);
                Tuple combined = combine(b);

                BigInteger sum = (BigInteger)combined.get(0);
                if (sum == null) {
                    return null;
                }
                BigInteger count = (BigInteger)combined.get(1);

                BigDecimal avg = null;
                if (count.compareTo(BigInteger.ZERO) > 0) {
                    avg = div(sum,count);
                }
                return avg;
            } catch (ExecException ee) {
                throw ee;
            } catch (Exception e) {
                int errCode = 2106;
                String msg = "Error while computing average in " + this.getClass().getSimpleName();
                throw new ExecException(msg, errCode, PigException.BUG, e);
            }
        }
    }

    static protected Tuple combine(DataBag values) throws ExecException {
        BigInteger sum = BigInteger.ZERO;
        BigInteger count = BigInteger.ZERO;

        // combine is called from Intermediate and Final
        // In either case, Initial would have been called
        // before and would have sent in valid tuples
        // Hence we don't need to check if incoming bag
        // is empty

        Tuple output = mTupleFactory.newTuple(2);
        boolean sawNonNull = false;
        for (Iterator<Tuple> it = values.iterator(); it.hasNext();) {
            Tuple t = it.next();
            BigInteger d = (BigInteger)t.get(0);
            // we count nulls in avg as contributing 0
            // a departure from SQL for performance of
            // COUNT() which implemented by just inspecting
            // size of the bag
            if (d == null) {
                d = BigInteger.ZERO;
            } else {
                sawNonNull = true;
            }
            sum = sum.add(d);
            count = count.add((BigInteger)t.get(1));
        }
        if (sawNonNull) {
            output.set(0, sum);
        } else {
            output.set(0, null);
        }
        output.set(1, count);
        return output;
    }

    static protected BigInteger count(Tuple input) throws ExecException {
        DataBag values = (DataBag)input.get(0);
        Iterator<Tuple> it = values.iterator();
        BigInteger cnt = BigInteger.ZERO;
        while (it.hasNext()) {
            Tuple t = (Tuple)it.next();
            if (t != null && t.size() > 0 && t.get(0) != null)
                cnt = cnt.add(BigInteger.ONE);
        }
        return cnt;
    }

    static protected BigInteger sum(Tuple input) throws ExecException, IOException {
        DataBag values = (DataBag)input.get(0);

        // if we were handed an empty bag, return NULL
        if (values == null || values.size() == 0) {
            return null;
        }

        BigInteger sum = BigInteger.ZERO;
        boolean sawNonNull = false;
        for (Iterator<Tuple> it = values.iterator(); it.hasNext();) {
            Tuple t = it.next();
            try {
                BigInteger d = (BigInteger)(t.get(0));
                if (d == null) continue;
                sawNonNull = true;
                sum = sum.add(d);
            } catch (RuntimeException exp) {
                int errCode = 2103;
                String msg = "Problem while computing sum of doubles.";
                throw new ExecException(msg, errCode, PigException.BUG, exp);
            }
        }

        if (sawNonNull) {
            return sum;
        } else {
            return null;
        }
    }

    static protected BigDecimal div(BigInteger dividend, BigInteger divisor) {
        // Averages will have IEEE 754R Decimal64 format, 16 digits, and a
        // rounding mode of HALF_EVEN, the IEEE 754R default
        return (new BigDecimal(dividend)).divide((new BigDecimal(divisor)), MathContext.DECIMAL128);
    }

    @Override
    public Schema outputSchema(Schema input) {
        return new Schema(new Schema.FieldSchema(null, DataType.BIGDECIMAL));
    }

    /* Accumulator interface */

    private BigInteger intermediateSum = null;
    private BigInteger intermediateCount = null;

    @Override
    public void accumulate(Tuple b) throws IOException {
        try {
            BigInteger sum = sum(b);
            if (sum == null) {
                return;
            }
            // set default values
            if (intermediateSum == null || intermediateCount == null) {
                intermediateSum = BigInteger.ZERO;
                intermediateCount = BigInteger.ZERO;
            }

            BigInteger count = (BigInteger)count(b);

            if (count.compareTo(BigInteger.ZERO) > 0) {
                intermediateCount = intermediateCount.add(count);
                intermediateSum = intermediateSum.add(sum);
            }
        } catch (ExecException ee) {
            throw ee;
        } catch (Exception e) {
            int errCode = 2106;
            String msg = "Error while computing average in " + this.getClass().getSimpleName();
            throw new ExecException(msg, errCode, PigException.BUG, e);
        }
    }

    @Override
    public void cleanup() {
        intermediateSum = null;
        intermediateCount = null;
    }

    @Override
    public BigDecimal getValue() {
        BigDecimal avg = null;
        if (intermediateCount != null && (intermediateCount.compareTo(BigInteger.ZERO) > 0)) {
            avg = div(intermediateSum,intermediateCount);
        }
        return avg;
    }
}
