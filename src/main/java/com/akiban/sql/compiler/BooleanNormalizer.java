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

   Derby - Class org.apache.derby.impl.sql.compile.AndNode

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

package com.akiban.sql.compiler;

import com.akiban.sql.parser.*;
import com.akiban.sql.unparser.NodeToString;

import com.akiban.sql.StandardException;
import com.akiban.sql.types.DataTypeDescriptor;
import com.akiban.sql.types.TypeId;

/** Perform normalization such as CNF on boolean expressions. */
public class BooleanNormalizer implements Visitor
{
  SQLParserContext parserContext;
  NodeFactory nodeFactory;
  public BooleanNormalizer(SQLParserContext parserContext) {
    this.parserContext = parserContext;
    this.nodeFactory = parserContext.getNodeFactory();
  }

  /* Normalize clauses in this SELECT node. */
  public void selectNode(SelectNode node) throws StandardException {
    node.setWhereClause(normalizeExpression(node.getWhereClause()));
    node.setHavingClause(normalizeExpression(node.getHavingClause()));
  }

  /* Normalize a top-level boolean expression. */
  public ValueNode normalizeExpression(ValueNode boolClause) throws StandardException {
    /* For each expression tree:
     *  o Eliminate NOTs (eliminateNots())
     *  o Ensure that there is an AndNode on top of every
     *    top level expression. (putAndsOnTop())
     *  o Finish the job (changeToCNF())
     */
    if (boolClause != null) {
      boolClause = eliminateNots(boolClause, false);
      assert verifyEliminateNots(boolClause);
      boolClause = putAndsOnTop(boolClause);
      assert verifyPutAndsOnTop(boolClause);
      boolClause = changeToCNF(boolClause, true);
      assert verifyChangeToCNF(boolClause, true);
    }
    return boolClause;
  }

  /**
   * Eliminate NotNodes in the current query block.  We traverse the tree, 
   * inverting ANDs and ORs and eliminating NOTs as we go.  We stop at 
   * ComparisonOperators and boolean expressions.  We invert 
   * ComparisonOperators and replace boolean expressions with 
   * boolean expression = false.
   * NOTE: Since we do not recurse under ComparisonOperators, there
   * still could be NotNodes left in the tree.
   *
   * @param node An expression node.
   * @param underNotNode Whether or not we are under a NotNode.
   *                                                    
   * @return The modified expression
   *
   * @exception StandardException               Thrown on error
   */
  protected ValueNode eliminateNots(ValueNode node, boolean underNotNode)
      throws StandardException {
    switch (node.getNodeType()) {
    case NodeTypes.NOT_NODE:
      {
        NotNode notNode = (NotNode)node;
        return eliminateNots(notNode.getOperand(), !underNotNode);
      }
    case NodeTypes.AND_NODE:
    case NodeTypes.OR_NODE:
      {
        BinaryLogicalOperatorNode bnode = (BinaryLogicalOperatorNode)node;
        ValueNode leftOperand = bnode.getLeftOperand();
        ValueNode rightOperand = bnode.getRightOperand();
        leftOperand = eliminateNots(leftOperand, underNotNode);
        rightOperand = eliminateNots(rightOperand, underNotNode);
        if (underNotNode) {
          /* Convert AND to OR and vice versa. */
          BinaryLogicalOperatorNode cnode = (BinaryLogicalOperatorNode)
            nodeFactory.getNode(NodeTypes.OR_NODE,
                                leftOperand, rightOperand,
                                parserContext);
          cnode.setType(bnode.getType());
          return cnode;
        }
        else {
          bnode.setLeftOperand(leftOperand);
          bnode.setRightOperand(rightOperand);
        }
      }
      break;
    case NodeTypes.CONDITIONAL_NODE:
      {
        ConditionalNode conditionalNode = (ConditionalNode)node;
        ValueNode thenNode = conditionalNode.getThenNode();
        ValueNode elseNode = conditionalNode.getElseNode();
        // TODO: Derby does not do this; is there any benefit?
        thenNode = eliminateNots(thenNode, false);
        elseNode = eliminateNots(elseNode, false);
        if (underNotNode) {
          ValueNode swap = thenNode;
          thenNode = elseNode;
          elseNode = swap;
        }
        conditionalNode.setThenNode(thenNode);
        conditionalNode.setElseNode(elseNode);
      }
      break;
    /* // Not sure about this
    case NodeTypes.IS_NODE:
      {
        IsNode isNode = (IsNode)node;
        ValueNode leftOperand = isNode.getLeftOperand();
        ValueNode rightOperand = isNode.getRightOperand();
        leftOperand = eliminateNots(leftOperand, underNotNode);
        rightOperand = eliminateNots(rightOperand, underNotNode);
        isNode.setLeftOperand(leftOperand);
        isNode.setRightOperand(rightOperand);
        if (underNotNode)
          isNode.toggleNegated();
      }
      break;
    */
    case NodeTypes.IS_NULL_NODE:
    case NodeTypes.IS_NOT_NULL_NODE:
      if (underNotNode) {
        UnaryOperatorNode unode = (UnaryOperatorNode)node;
        ValueNode operand = unode.getOperand();
        int newNodeType;
        switch (node.getNodeType()) {
        case NodeTypes.IS_NULL_NODE:
          newNodeType = NodeTypes.IS_NOT_NULL_NODE;
          break;
        case NodeTypes.IS_NOT_NULL_NODE:
          newNodeType = NodeTypes.IS_NULL_NODE;
          break;
        default:
          assert false;
          newNodeType = -1;
        }
        ValueNode newNode = (ValueNode)nodeFactory.getNode(newNodeType,
                                                           operand,
                                                           parserContext);
        newNode.setType(unode.getType());
        return newNode;
      }
      break;
    case NodeTypes.BINARY_EQUALS_OPERATOR_NODE:
    case NodeTypes.BINARY_GREATER_EQUALS_OPERATOR_NODE:
    case NodeTypes.BINARY_GREATER_THAN_OPERATOR_NODE:
    case NodeTypes.BINARY_LESS_THAN_OPERATOR_NODE:
    case NodeTypes.BINARY_LESS_EQUALS_OPERATOR_NODE:
    case NodeTypes.BINARY_NOT_EQUALS_OPERATOR_NODE:
      if (underNotNode) {
        BinaryOperatorNode onode = (BinaryOperatorNode)node;
        ValueNode leftOperand = onode.getLeftOperand();
        ValueNode rightOperand = onode.getRightOperand();
        int newNodeType;
        switch (node.getNodeType()) {
        case NodeTypes.BINARY_EQUALS_OPERATOR_NODE:
          newNodeType = NodeTypes.BINARY_NOT_EQUALS_OPERATOR_NODE;
          break;
        case NodeTypes.BINARY_GREATER_EQUALS_OPERATOR_NODE:
          newNodeType = NodeTypes.BINARY_LESS_THAN_OPERATOR_NODE;
          break;
        case NodeTypes.BINARY_GREATER_THAN_OPERATOR_NODE:
          newNodeType = NodeTypes.BINARY_LESS_EQUALS_OPERATOR_NODE;
          break;
        case NodeTypes.BINARY_LESS_THAN_OPERATOR_NODE:
          newNodeType = NodeTypes.BINARY_GREATER_EQUALS_OPERATOR_NODE;
          break;
        case NodeTypes.BINARY_LESS_EQUALS_OPERATOR_NODE:
          newNodeType = NodeTypes.BINARY_GREATER_THAN_OPERATOR_NODE;
          break;
        case NodeTypes.BINARY_NOT_EQUALS_OPERATOR_NODE:
          newNodeType = NodeTypes.BINARY_EQUALS_OPERATOR_NODE;
          break;
        default:
          assert false;
          newNodeType = -1;
        }
        ValueNode newNode = (ValueNode)nodeFactory.getNode(newNodeType, 
                                                           leftOperand, rightOperand,
                                                           parserContext);
        newNode.setType(onode.getType());
        return newNode;
      }
      break;
    case NodeTypes.BETWEEN_OPERATOR_NODE:
      if (underNotNode) {
        BetweenOperatorNode betweenOperatorNode = (BetweenOperatorNode)node;
        ValueNode leftOperand = betweenOperatorNode.getLeftOperand();
        ValueNodeList rightOperandList = betweenOperatorNode.getRightOperandList();
        /* We want to convert the BETWEEN  * into < OR > as described below. */ 
        /* Convert:
         *    leftO between rightOList.elementAt(0) and rightOList.elementAt(1)
         * to:
         *    leftO < rightOList.elementAt(0) or leftO > rightOList.elementAt(1)
         * NOTE - We do the conversion here since ORs will eventually be
         * optimizable and there's no benefit for the optimizer to see NOT BETWEEN
         */

        /* leftO < rightOList.elementAt(0) */
        BinaryComparisonOperatorNode leftBCO = (BinaryComparisonOperatorNode)
          nodeFactory.getNode(NodeTypes.BINARY_LESS_THAN_OPERATOR_NODE,
                              leftOperand, 
                              rightOperandList.get(0),
                              parserContext);

        /* leftO > rightOList.elementAt(1) */
        BinaryComparisonOperatorNode rightBCO = (BinaryComparisonOperatorNode) 
          nodeFactory.getNode(NodeTypes.BINARY_GREATER_THAN_OPERATOR_NODE,
                              leftOperand,
                              rightOperandList.get(1),
                              parserContext);

        /* Create and return the OR */
        OrNode newOr = (OrNode)nodeFactory.getNode(NodeTypes.OR_NODE,
                                                   leftBCO,
                                                   rightBCO,
                                                   parserContext);
        // TODO: Maybe something about types.
        return newOr;
      }
    case NodeTypes.IN_LIST_OPERATOR_NODE:
      if (underNotNode) {
        InListOperatorNode inListOperatorNode = (InListOperatorNode)node;
        ValueNode leftOperand = inListOperatorNode.getLeftOperand();
        ValueNodeList rightOperandList = inListOperatorNode.getRightOperandList();
        /* We want to convert the IN List into = OR = ... as * described below. */
        /* Convert:
         *    leftO IN rightOList.elementAt(0) , rightOList.elementAt(1) ...
         * to:
         *    leftO <> rightOList.elementAt(0) AND leftO <> rightOList.elementAt(1) ...
         * NOTE - We do the conversion here since the single table clauses
         * can be pushed down and the optimizer may eventually have a filter factor
         * for <>.
         */
        ValueNode result = null;
        for (ValueNode rightOperand : rightOperandList) {
          BinaryComparisonOperatorNode rightBCO = (BinaryComparisonOperatorNode)
            nodeFactory.getNode(NodeTypes.BINARY_NOT_EQUALS_OPERATOR_NODE,
                                leftOperand, rightOperand,
                                parserContext);
          if (result == null)
            result = rightBCO;
          else {
            AndNode andNode = (AndNode)nodeFactory.getNode(NodeTypes.AND_NODE,
                                                           result, rightBCO,
                                                           parserContext);
            result = andNode;
          }
        }
        return result;
      }
      break;
    case NodeTypes.BOOLEAN_CONSTANT_NODE:
      if (underNotNode) {
        BooleanConstantNode bnode = (BooleanConstantNode)node;
        bnode.setBooleanValue(!bnode.getBooleanValue());
      }
      break;
    case NodeTypes.SQL_BOOLEAN_CONSTANT_NODE:
      if (underNotNode) {
        ConstantNode cnode = (ConstantNode)node;
        cnode.setValue(cnode.getValue() == Boolean.TRUE ? Boolean.FALSE : Boolean.TRUE);
      }
      break;
    default:
      if (underNotNode) {
        BooleanConstantNode falseNode = (BooleanConstantNode) 
          nodeFactory.getNode(NodeTypes.BOOLEAN_CONSTANT_NODE,
                              Boolean.FALSE,
                              parserContext);
        BinaryRelationalOperatorNode equalsNode = (BinaryRelationalOperatorNode)
          nodeFactory.getNode(NodeTypes.BINARY_EQUALS_OPERATOR_NODE,
                              node, falseNode,
                              parserContext);
        if (node.getType() != null) {
          boolean nullableResult = node.getType().isNullable();
          equalsNode.setType(new DataTypeDescriptor(TypeId.BOOLEAN_ID,
                                                    nullableResult));
        }
        return equalsNode;
      }
      break;
    }
    return node;
  }

  /**
   * Verify that eliminateNots() did its job correctly.  Verify that
   * there are no NotNodes above the top level comparison operators
   * and boolean expressions.
   *
   * @return Boolean which reflects validity of the tree.
   */
  protected boolean verifyEliminateNots(ValueNode node) {
    switch (node.getNodeType()) {
    case NodeTypes.NOT_NODE:
      return false;
    case NodeTypes.AND_NODE:
    case NodeTypes.OR_NODE:
      {
        BinaryLogicalOperatorNode bnode = (BinaryLogicalOperatorNode)node;
        return verifyEliminateNots(bnode.getLeftOperand()) &&
               verifyEliminateNots(bnode.getRightOperand());
      }
    }
    return true;
  }

  /**
   * Do the 1st step in putting an expression into conjunctive normal
   * form.  This step ensures that the top level of the expression is
   * a chain of AndNodes terminated by a true BooleanConstantNode.
   *
   * @param node An expression node.
   *
   * @return The modified expression
   *
   * @exception StandardException Thrown on error
   */
  protected AndNode putAndsOnTop(ValueNode node) throws StandardException {
    switch (node.getNodeType()) {
    case NodeTypes.AND_NODE:
      {
        AndNode andNode = (AndNode)node;
        andNode.setRightOperand(putAndsOnTop(andNode.getRightOperand()));
        return andNode;
      }
    case NodeTypes.COLUMN_REFERENCE:
      {
        /* X -> (X = TRUE) AND TRUE */
        // TODO: This is suspicious: it only happens on the right-hand AND branch,
        // and so outputs WHERE f AND ((g = true) AND true).
        // Perhaps a separate pass that does more (down both sides of
        // AND and OR), or an earlier conversion that makes sure
        // things are in some standard boolean form.
        BooleanConstantNode trueNode = (BooleanConstantNode)
          nodeFactory.getNode(NodeTypes.BOOLEAN_CONSTANT_NODE,
                              Boolean.TRUE,
                              parserContext);
        BinaryComparisonOperatorNode equalsNode = (BinaryComparisonOperatorNode)
          nodeFactory.getNode(NodeTypes.BINARY_EQUALS_OPERATOR_NODE,
                              node, trueNode,
                              parserContext);
        AndNode andNode = (AndNode)nodeFactory.getNode(NodeTypes.AND_NODE,
                                                       equalsNode, trueNode, 
                                                       parserContext);
        return andNode;
      }
      /* // Not sure at all about this
         case NodeTypes.IS_NODE:
         {
         IsNode isNode = (IsNode)node;
         ValueNode leftOperand = isNode.getLeftOperand();
         ValueNode rightOperand = isNode.getRightOperand();
         leftOperand = putAndsOnTop(leftOperand);
         rightOperand = putAndsOnTop(rightOperand);
         isNode.setLeftOperand(leftOperand);
         isNode.setRightOperand(rightOperand);
         }
         break;
      */
    default:
      {
        /* expr -> expr AND TRUE */
        BooleanConstantNode trueNode = (BooleanConstantNode)
          nodeFactory.getNode(NodeTypes.BOOLEAN_CONSTANT_NODE,
                              Boolean.TRUE,
                              parserContext);
        AndNode andNode = (AndNode)nodeFactory.getNode(NodeTypes.AND_NODE,
                                                       node, trueNode,
                                                       parserContext);
        return andNode;
      }
    }
  }

  /**
   * Verify that putAndsOnTop() did its job correctly.  Verify that the top level 
   * of the expression is a chain of AndNodes terminated by a true BooleanConstantNode.
   *
   * @param node An expression node.
   *
   * @return Boolean which reflects validity of the tree.
   */
  protected boolean verifyPutAndsOnTop(ValueNode node) {
    while (true) {
      if (!(node instanceof AndNode)) 
        return false;
      node = ((AndNode)node).getRightOperand();
      if (node.isBooleanTrue())
        return true;
      // else another AND.
    }
  }

  /**
   * Finish putting an expression into conjunctive normal
   * form.  An expression tree in conjunctive normal form meets
   * the following criteria:
   *    o  If the expression tree is not null,
   *       the top level will be a chain of AndNodes terminating
   *       in a true BooleanConstantNode.
   *    o  The left child of an AndNode will never be an AndNode.
   *    o  Any right-linked chain that includes an AndNode will
   *       be entirely composed of AndNodes terminated by a true BooleanConstantNode.
   *    o  The left child of an OrNode will never be an OrNode.
   *    o  Any right-linked chain that includes an OrNode will
   *       be entirely composed of OrNodes terminated by a false BooleanConstantNode.
   *    o  ValueNodes other than AndNodes and OrNodes are considered
   *       leaf nodes for purposes of expression normalization.
   *       In other words, we won't do any normalization under
   *       those nodes.
   *
   * In addition, we track whether or not we are under a top level AndNode.  
   * SubqueryNodes need to know this for subquery flattening.
   *
   * @param node An expression node.
   * @param underTopAndNode Whether or not we are under a top level AndNode.
   *
   * @return The modified expression
   *
   * @exception StandardException Thrown on error
   */
  protected ValueNode changeToCNF(ValueNode node, boolean underTopAndNode)
      throws StandardException {
    switch (node.getNodeType()) {
    case NodeTypes.AND_NODE:
      {
        AndNode andNode = (AndNode)node;
        ValueNode leftOperand = andNode.getLeftOperand();
        ValueNode rightOperand = andNode.getRightOperand();

        /* Top chain will be a chain of Ands terminated by a non-AndNode.
         * (putAndsOnTop() has taken care of this. If the last node in
         * the chain is not a true BooleanConstantNode then we need to do the
         * transformation to make it so.
         */

        /* Add the true BooleanConstantNode if not there yet */
        if (!(rightOperand instanceof AndNode) &&
            !(rightOperand.isBooleanTrue())) {
          BooleanConstantNode trueNode = (BooleanConstantNode) 
            nodeFactory.getNode(NodeTypes.BOOLEAN_CONSTANT_NODE,
                                Boolean.TRUE,
                                parserContext);
          rightOperand = (AndNode)nodeFactory.getNode(NodeTypes.AND_NODE,
                                                      rightOperand, trueNode,
                                                      parserContext);
        }

        /* If leftOperand is an AndNode, then we modify the tree from:
         *
         *                And1
         *               /    \
         *            And2    Nodex
         *           /    \        ...
         *        left2    right2
         *
         *    to:
         *
         *                        And1
         *                       /    \
         *     changeToCNF(left2)      And2
         *                            /    \
         *         changeToCNF(right2)      changeToCNF(Nodex)
         *
         *  NOTE: We could easily switch places between changeToCNF(left2) and 
         *  changeToCNF(right2).
         */

        /* Pull up the AndNode chain to our left */
        while (leftOperand instanceof AndNode) {
          AndNode oldLeft = (AndNode)leftOperand;
          ValueNode oldRight = rightOperand;
          ValueNode newLeft = oldLeft.getLeftOperand();
          AndNode newRight = oldLeft;
          
          /* We then twiddle the tree to match the above diagram */
          leftOperand = newLeft;
          rightOperand = newRight;
          newRight.setLeftOperand(oldLeft.getRightOperand());
          newRight.setRightOperand(oldRight);
        }
        
        /* We then twiddle the tree to match the above diagram */
        leftOperand = changeToCNF(leftOperand, underTopAndNode);
        rightOperand = changeToCNF(rightOperand, underTopAndNode);
        
        andNode.setLeftOperand(leftOperand);
        andNode.setRightOperand(rightOperand);
      }
      break;
    case NodeTypes.OR_NODE:
      {
        OrNode orNode = (OrNode)node;
        ValueNode leftOperand = orNode.getLeftOperand();
        ValueNode rightOperand = orNode.getRightOperand();

        /* If rightOperand is an AndNode, then we must generate an 
         * OrNode above it.
         */
        if (rightOperand instanceof AndNode) {
          BooleanConstantNode falseNode = (BooleanConstantNode) 
            nodeFactory.getNode(NodeTypes.BOOLEAN_CONSTANT_NODE,
                                Boolean.FALSE,
                                parserContext);
          rightOperand = (ValueNode)nodeFactory.getNode(NodeTypes.OR_NODE,
                                                        rightOperand, falseNode,
                                                        parserContext);
          orNode.setRightOperand(rightOperand);
        }
        
        /* We need to ensure that the right chain is terminated by
         * a false BooleanConstantNode.
         */
        while (rightOperand instanceof OrNode) {
          orNode = (OrNode)orNode.getRightOperand();
          rightOperand = orNode.getRightOperand();
        }

        /* Add the false BooleanConstantNode if not there yet */
        if (!rightOperand.isBooleanFalse()) {
          BooleanConstantNode	falseNode = (BooleanConstantNode) 
            nodeFactory.getNode(NodeTypes.BOOLEAN_CONSTANT_NODE,
                                Boolean.FALSE,
                                parserContext);
          orNode.setRightOperand((ValueNode)nodeFactory.getNode(NodeTypes.OR_NODE,
                                                                rightOperand, 
                                                                falseNode,
                                                                parserContext));
        }

        orNode = (OrNode)node;
        rightOperand = orNode.getRightOperand();

        /* If leftOperand is an OrNode, then we modify the tree from:
         *
         *                Or1 
         *               /    \
         *            Or2        Nodex
         *           /    \        ...
         *        left2    right2
         *
         *    to:
         *
         *                        Or1 
         *                       /    \
         *     changeToCNF(left2)      Or2
         *                            /    \
         *         changeToCNF(right2)      changeToCNF(Nodex)
         *
         *  NOTE: We could easily switch places between changeToCNF(left2) and 
         *  changeToCNF(right2).
         */

        while (leftOperand instanceof OrNode) {
          OrNode oldLeft = (OrNode)leftOperand;
          ValueNode oldRight = rightOperand;
          ValueNode newLeft = oldLeft.getLeftOperand();
          OrNode newRight = oldLeft;

          /* We then twiddle the tree to match the above diagram */
          leftOperand = newLeft;
          rightOperand = newRight;
          newRight.setLeftOperand(oldLeft.getRightOperand());
          newRight.setRightOperand(oldRight);
        }

        /* Finally, we continue to normalize the left and right subtrees. */
        leftOperand = changeToCNF(leftOperand, false);
        rightOperand = changeToCNF(rightOperand, false);
        
        orNode.setLeftOperand(leftOperand);
        orNode.setRightOperand(rightOperand);
      }
      break;

      // TODO: subquery node to pick up underTopAndNode for flattening.
      // BinaryComparisonOperatorNode for that case.

    }
    return node;
  }

  /**
   * Verify that changeToCNF() did its job correctly.  Verify that:
   *    o  AndNode  - rightOperand is not instanceof OrNode
   *                  leftOperand is not instanceof AndNode
   *    o  OrNode    - rightOperand is not instanceof AndNode
   *                  leftOperand is not instanceof OrNode
   *
   * @param node An expression node.
   *
   * @return Boolean which reflects validity of the tree.
   */
  protected boolean verifyChangeToCNF(ValueNode node, boolean top) {
    if (node instanceof AndNode) {
      AndNode andNode = (AndNode)node;
      ValueNode leftOperand = andNode.getLeftOperand();
      ValueNode rightOperand = andNode.getRightOperand();
      boolean isValid = ((rightOperand instanceof AndNode) ||
                         rightOperand.isBooleanTrue());
      if (rightOperand instanceof AndNode) {
        isValid = verifyChangeToCNF(rightOperand, false);
      }
      if (leftOperand instanceof AndNode) {
        isValid = false;
      }
      else {
        isValid = isValid && verifyChangeToCNF(leftOperand, false);
      }
      return isValid;
    }
    if (top) 
      return false;
    if (node instanceof OrNode) {
      OrNode orNode = (OrNode)node;
      ValueNode leftOperand = orNode.getLeftOperand();
      ValueNode rightOperand = orNode.getRightOperand();
      boolean isValid = ((rightOperand instanceof OrNode) ||
                         rightOperand.isBooleanFalse());
      if (rightOperand instanceof OrNode) {
        isValid = verifyChangeToCNF(rightOperand, false);
      }
      if (leftOperand instanceof OrNode) {
        isValid = false;
      }
      else {
        isValid = verifyChangeToCNF(leftOperand, false);
      }
      return isValid;
    }
    return true;
  }

  /* Visitor interface */

  public Visitable visit(Visitable node) throws StandardException {
    switch (((QueryTreeNode)node).getNodeType()) {
    case NodeTypes.SELECT_NODE:
      selectNode((SelectNode)node);
    }
    return node;
  }

  public boolean visitChildrenFirst(Visitable node) {
    return true;
  }
  public boolean stopTraversal() {
    return false;
  }
  public boolean skipChildren(Visitable node) throws StandardException {
    return false;
  }

  // TODO: Temporary low-budget testing.
  public static void main(String[] args) throws Exception {
    SQLParser p = new SQLParser();
    BooleanNormalizer bn = new BooleanNormalizer(p);
    NodeToString ts = new NodeToString();
    for (String arg : args) {
      System.out.println("=====");
      System.out.println(arg);
      try {
        StatementNode stmt = p.parseStatement(arg);
        String sql = ts.toString(stmt);
        System.out.println(sql);
        stmt = (StatementNode)stmt.accept(bn);
        sql = ts.toString(stmt);
        System.out.println(sql);
      }
      catch (StandardException ex) {
        ex.printStackTrace();
      }
    }
  }
}
