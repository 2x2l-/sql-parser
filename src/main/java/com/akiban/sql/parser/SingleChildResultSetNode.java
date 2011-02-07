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

   Derby - Class org.apache.derby.impl.sql.compile.SingleChildResultSetNode

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
 * A SingleChildResultSetNode represents a result set with a single child.
 *
 */

abstract class SingleChildResultSetNode extends FromTable
{
  /**
   * ResultSetNode under the SingleChildResultSetNode
   */
  ResultSetNode childResult;

  /**
   * Initialilzer for a SingleChildResultSetNode.
   *
   * @param childResult The child ResultSetNode
   * @param tableProperties Properties list associated with the table
   */

  public void init(Object childResult, Object tableProperties) {
    /* correlationName is always null */
    super.init(null, tableProperties);
    this.childResult = (ResultSetNode)childResult;
  }

  /**
   * Return the childResult from this node.
   *
   * @return ResultSetNode The childResult from this node.
   */
  public ResultSetNode getChildResult() {
    return childResult;
  }

  /**
   * Set the childResult for this node.
   *
   * @param childResult The new childResult for this node.
   */
  void setChildResult(ResultSetNode childResult) {
    this.childResult = childResult;
  }

  /**
   * Prints the sub-nodes of this object.  See QueryTreeNode.java for
   * how tree printing is supposed to work.
   *
   * @param depth The depth of this node in the tree
   */

  public void printSubNodes(int depth) {
    super.printSubNodes(depth);

    if (childResult != null) {
      printLabel(depth, "childResult: ");
      childResult.treePrint(depth + 1);
    }
  }

  /**
   * Set the (query block) level (0-based) for this FromTable.
   *
   * @param level The query block level for this FromTable.
   */
  public void setLevel(int level) {
    super.setLevel(level);
    if (childResult instanceof FromTable) {
        ((FromTable)childResult).setLevel(level);
      }
  }

  /** 
   * Determine whether or not the specified name is an exposed name in
   * the current query block.
   *
   * @param name The specified name to search for as an exposed name.
   * @param schemaName Schema name, if non-null.
   * @param exactMatch Whether or not we need an exact match on specified schema and table
   *                   names or match on table id.
   *
   * @return The FromTable, if any, with the exposed name.
   *
   * @exception StandardException Thrown on error
   */
  protected FromTable getFromTableByName(String name, String schemaName, 
                                         boolean exactMatch)
      throws StandardException {
    return childResult.getFromTableByName(name, schemaName, exactMatch);
  }

  /**
   * Accept the visitor for all visitable children of this node.
   * 
   * @param v the visitor
   *
   * @exception StandardException on error
   */
  void acceptChildren(Visitor v) throws StandardException {
    super.acceptChildren(v);

    if (childResult != null) {
      childResult = (ResultSetNode)childResult.accept(v);
    }
  }

}
