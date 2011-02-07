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

   Derby - Class org.apache.derby.impl.sql.compile.DDLStatementNode

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

import com.akiban.sql.StandardException;

/**
 * A DDLStatementNode represents any type of DDL statement: CREATE TABLE,
 * CREATE INDEX, ALTER TABLE, etc.
 *
 */

abstract class DDLStatementNode extends StatementNode
{
  /////////////////////////////////////////////////////////////////////////
  //
  // CONSTANTS
  //
  /////////////////////////////////////////////////////////////////////////

  public static final int UNKNOWN_TYPE = 0;
  public static final int ADD_TYPE = 1;
  public static final int DROP_TYPE = 2;
  public static final int MODIFY_TYPE = 3;
  public static final int LOCKING_TYPE = 4;

  /////////////////////////////////////////////////////////////////////////
  //
  // STATE
  //
  /////////////////////////////////////////////////////////////////////////

  private TableName objectName;
  private boolean initOk;

  /**
     sub-classes can set this to be true to allow implicit
     creation of the main object's schema at execution time.
  */
  boolean implicitCreateSchema;

  /////////////////////////////////////////////////////////////////////////
  //
  // BEHAVIOR
  //
  /////////////////////////////////////////////////////////////////////////

    public void init(Object objectName) throws StandardException {
      initAndCheck(objectName);
    }

  /**
     Initialize the object name we will be performing the DDL
     on and check that we are not in the system schema
     and that DDL is allowed.
  */
  protected void initAndCheck(Object objectName) throws StandardException {
    this.objectName = (TableName)objectName;
    initOk = true;
  }

  /**
   * A DDL statement is always atomic
   *
   * @return true 
   */
  public boolean isAtomic() {
    return true;
  }

  /**
   * Return the name of the table being dropped.
   * This is the unqualified table name.
   *
   * @return the relative name
   */
  public String getRelativeName() {
    return objectName.getTableName() ;
  }

  /**
   * Return the full dot expression name of the 
   * object being dropped.
   * 
   * @return the full name
   */
  public String getFullName() {
    return objectName.getFullTableName() ;
  }

  public final TableName getObjectName() { 
    return objectName; 
  }

  /**
   * Convert this object to a String.  See comments in QueryTreeNode.java
   * for how this should be done for tree printing.
   *
   * @return This object as a String
   */

  public String toString() {
    return ((objectName==null)?"":
            "name: " + objectName.toString() +"\n") + super.toString();
  }
    
}
