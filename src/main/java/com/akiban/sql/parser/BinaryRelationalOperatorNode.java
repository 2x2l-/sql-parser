/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

/* The original from which this derives bore the following: */

/*

   Derby - Class org.apache.derby.impl.sql.compile.BinaryRelationalOperatorNode

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
 * This class represents the 6 binary operators: LessThan, LessThanEquals,
 * Equals, NotEquals, GreaterThan and GreaterThanEquals.
 *
 */

public class BinaryRelationalOperatorNode extends BinaryComparisonOperatorNode 
{
    // TODO: Is there any point to this?

    public final int EQUALS_RELOP = 1;
    public final int NOT_EQUALS_RELOP = 2;
    public final int GREATER_THAN_RELOP = 3;
    public final int GREATER_EQUALS_RELOP = 4;
    public final int LESS_THAN_RELOP = 5;
    public final int LESS_EQUALS_RELOP = 6;
    public final int IS_NULL_RELOP = 7;
    public final int IS_NOT_NULL_RELOP = 8;

    private int operatorType;

    public void init(Object leftOperand, Object rightOperand) {
        String methodName = "";
        String operatorName = "";

        switch (getNodeType()) {
        case NodeTypes.BINARY_EQUALS_OPERATOR_NODE:
            methodName = "equals";
            operatorName = "=";
            operatorType = EQUALS_RELOP;
            break;

        case NodeTypes.BINARY_GREATER_EQUALS_OPERATOR_NODE:
            methodName = "greaterOrEquals";
            operatorName = ">=";
            operatorType = GREATER_EQUALS_RELOP;
            break;

        case NodeTypes.BINARY_GREATER_THAN_OPERATOR_NODE:
            methodName = "greaterThan";
            operatorName = ">";
            operatorType = GREATER_THAN_RELOP;
            break;

        case NodeTypes.BINARY_LESS_EQUALS_OPERATOR_NODE:
            methodName = "lessOrEquals";
            operatorName = "<=";
            operatorType =  LESS_EQUALS_RELOP;
            break;

        case NodeTypes.BINARY_LESS_THAN_OPERATOR_NODE:
            methodName = "lessThan";
            operatorName = "<";
            operatorType = LESS_THAN_RELOP;
            break;
        case NodeTypes.BINARY_NOT_EQUALS_OPERATOR_NODE:
            methodName = "notEquals";
            operatorName = "<>";
            operatorType = NOT_EQUALS_RELOP;
            break;

        default:
            assert false : "init for BinaryRelationalOperator called with wrong nodeType = " + getNodeType();
            break;
        }
        super.init(leftOperand, rightOperand, operatorName, methodName);
    }

    /**
     * Fill this node with a deep copy of the given node.
     */
    public void copyFrom(QueryTreeNode node) throws StandardException {
        super.copyFrom(node);

        BinaryRelationalOperatorNode other = (BinaryRelationalOperatorNode)node;
        this.operatorType = other.operatorType;
    }

    public int getOperatorType() {
        return operatorType;
    }

}
