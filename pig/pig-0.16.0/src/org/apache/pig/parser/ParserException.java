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

import org.antlr.runtime.RecognitionException;
import org.apache.pig.impl.logicalLayer.FrontendException;


public class ParserException extends FrontendException {
	private static final long serialVersionUID = 1L;
	private static final int errorCode = 1200;
	
	public ParserException(RecognitionException recoException) {
		super( "Pig script failed to parse: " + recoException, errorCode, recoException );
	}
	
	public ParserException(String msg) {
		super( msg, errorCode );
	}
	
	public ParserException(String msg, SourceLocation location) {
	    super(msg, errorCode, location);
	}

	public ParserException(String msg, Throwable cause) {
	    super( msg, errorCode, cause );
	}
	
	@Override
	public String toString() {
		return "Failed to parse: " +  getMessage();
	}
}
