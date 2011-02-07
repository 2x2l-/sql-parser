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

   Derby - Class org.apache.derby.impl.sql.compile.InsertNode

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

import java.util.Properties;

/**
 * An InsertNode is the top node in a query tree for an
 * insert statement.
 * <p>
 * After parsing, the node contains
 *   targetTableName: the target table for the insert
 *   collist: a list of column names, if specified
 *   queryexpr: the expression being inserted, either
 *				a values clause or a select form; both
 *			    of these are represented via the SelectNode,
 *				potentially with a TableOperatorNode such as
 *				UnionNode above it.
 * <p>
 * After binding, the node has had the target table's
 * descriptor located and inserted, and the queryexpr
 * and collist have been massaged so that they are identical
 * to the table layout.  This involves adding any default
 * values for missing columns, and reordering the columns
 * to match the table's ordering of them.
 * <p>
 * After optimizing, ...
 */
public final class InsertNode extends DMLModStatementNode
{
  public ResultColumnList targetColumnList;
  public Properties targetProperties;
  private OrderByList orderByList;
  private ValueNode offset;
  private ValueNode fetchFirst;

  /**
   * Initializer for an InsertNode.
   *
   * @param targetName The name of the table/VTI to insert into
   * @param insertColumns A ResultColumnList with the names of the
   *                      columns to insert into.  May be null if the
   *                      user did not specify the columns - in this
   *                      case, the binding phase will have to figure
   *                      it out.
   * @param queryExpression The query expression that will generate
   *                        the rows to insert into the given table
   * @param targetProperties The properties specified on the target table
   * @param orderByList The order by list for the source result set, null if
   *                    no order by list
   */

  public void init(Object targetName,
                   Object insertColumns,
                   Object queryExpression,
                   Object targetProperties,
                   Object orderByList,
                   Object offset,
                   Object fetchFirst) {
    /* statementType gets set in super() before we've validated
     * any properties, so we've kludged the code to get the
     * right statementType for a bulk insert replace.
     */
    super.init(queryExpression,
               getStatementType((Properties)targetProperties));
    setTarget((QueryTreeNode)targetName);
    targetColumnList = (ResultColumnList)insertColumns;
    this.targetProperties = (Properties)targetProperties;
    this.orderByList = (OrderByList)orderByList;
    this.offset = (ValueNode)offset;
    this.fetchFirst = (ValueNode)fetchFirst;

    /* Remember that the query expression is the source to an INSERT */
    getResultSetNode().setInsertSource();
  }

  /**
   * Convert this object to a String.  See comments in QueryTreeNode.java
   * for how this should be done for tree printing.
   *
   * @return This object as a String
   */

  public String toString() {
    return targetProperties + "\n"
      + super.toString();
  }

  public String statementToString() {
    return "INSERT";
  }

  /**
   * Prints the sub-nodes of this object.  See QueryTreeNode.java for
   * how tree printing is supposed to work.
   *
   * @param depth The depth of this node in the tree
   */

  public void printSubNodes(int depth) {
    super.printSubNodes(depth);

    if (targetTableName != null) {
      printLabel(depth, "targetTableName: ");
      targetTableName.treePrint(depth + 1);
    }

    if (targetColumnList != null) {
      printLabel(depth, "targetColumnList: ");
      targetColumnList.treePrint(depth + 1);
    }

    if (orderByList != null) {
      printLabel(depth, "orderByList: ");
      orderByList.treePrint(depth + 1);
    }

    /* RESOLVE - need to print out targetTableDescriptor */
  }

  /**
   * Return the type of statement, something from
   * StatementType.
   *
   * @return the type of statement
   */
  protected int getStatementType() {
    return StatementType.INSERT;
  }

  /**
   * Return the statement type, where it is dependent on
   * the targetProperties.  (insertMode = replace causes
   * statement type to be BULK_INSERT_REPLACE.
   *
   * @return the type of statement
   */
  static int getStatementType(Properties targetProperties) {
    int retval = StatementType.INSERT;

    // The only property that we're currently interested in is insertMode
    String insertMode = (targetProperties == null) ? null : targetProperties.getProperty("insertMode");
    if (insertMode != null) {
      if ("REPLACE".equalsIgnoreCase(insertMode)) {
        retval = StatementType.BULK_INSERT_REPLACE;
      }
    }
    return retval;
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

    if (targetColumnList != null) {
      targetColumnList.accept(v);
    }
  }

}
