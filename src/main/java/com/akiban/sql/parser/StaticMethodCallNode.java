/* Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

/* The original from which this derives bore the following: */

/*

   Derby - Class org.apache.derby.impl.sql.compile.StaticMethodCallNode

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package com.akiban.sql.parser;

/**
 * A StaticMethodCallNode represents a static method call from a Class
 * (as opposed to from an Object).

   For a procedure the call requires that the arguments be ? parameters.
   The parameter is *logically* passed into the method call a number of different ways.

   <P>
   For a application call like CALL MYPROC(?) the logically Java method call is
   (in psuedo Java/SQL code) (examples with CHAR(10) parameter)
   <BR>
   Fixed length IN parameters - com.acme.MyProcedureMethod(?)
   <BR>
   Variable length IN parameters - com.acme.MyProcedureMethod(CAST (? AS CHAR(10))
   <BR>
   Fixed length INOUT parameter -
		String[] holder = new String[] {?}; com.acme.MyProcedureMethod(holder); ? = holder[0]
   <BR>
   Variable length INOUT parameter -
		String[] holder = new String[] {CAST (? AS CHAR(10)}; com.acme.MyProcedureMethod(holder); ? = CAST (holder[0] AS CHAR(10))

   <BR>
   Fixed length OUT parameter -
		String[] holder = new String[1]; com.acme.MyProcedureMethod(holder); ? = holder[0]

   <BR>
   Variable length INOUT parameter -
		String[] holder = new String[1]; com.acme.MyProcedureMethod(holder); ? = CAST (holder[0] AS CHAR(10))


    <P>
	For static method calls there is no pre-definition of an IN or INOUT parameter, so a call to CallableStatement.registerOutParameter()
	makes the parameter an INOUT parameter, provided:
		- the parameter is passed directly to the method call (no casts or expressions).
		- the method's parameter type is a Java array type.

    Since this is a dynmaic decision we compile in code to take both paths, based upon a boolean isINOUT which is dervied from the
	ParameterValueSet. Code is logically (only single parameter String[] shown here). Note, no casts can exist here.

	boolean isINOUT = getParameterValueSet().getParameterMode(0) == PARAMETER_IN_OUT;
	if (isINOUT) {
		String[] holder = new String[] {?}; com.acme.MyProcedureMethod(holder); ? = holder[0]
	   
	} else {
		com.acme.MyProcedureMethod(?)
	}

 *
 */
public class StaticMethodCallNode extends MethodCallNode
{
  private TableName procedureName;

  /**
   * Intializer for a NonStaticMethodCallNode
   *
   * @param methodName The name of the method to call
   * @param javaClassName The name of the java class that the static method belongs to.
   */
  public void init(Object methodName, Object javaClassName) {
    if (methodName instanceof String)
      init(methodName);
    else {
      procedureName = (TableName)methodName;
      init(procedureName.getTableName());
    }

    this.javaClassName = (String)javaClassName;
  }

  /**
   * Convert this object to a String.  See comments in QueryTreeNode.java
   * for how this should be done for tree printing.
   *
   * @return This object as a String
   */

  public String toString() {
    return "javaClassName: " +
      (javaClassName != null ? javaClassName : "null") + "\n" +
      super.toString();
  }

}
