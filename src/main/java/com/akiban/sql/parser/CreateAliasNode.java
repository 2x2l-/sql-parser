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

   Derby - Class org.apache.derby.impl.sql.compile.CreateAliasNode

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
import com.akiban.sql.types.AliasInfo;
import com.akiban.sql.types.DataTypeDescriptor;
import com.akiban.sql.types.RoutineAliasInfo;
import com.akiban.sql.types.SynonymAliasInfo;
import com.akiban.sql.types.UDTAliasInfo;

import java.util.List;

/**
 * A CreateAliasNode represents a CREATE ALIAS statement.
 *
 */

public class CreateAliasNode extends DDLStatementNode
{
    // indexes into routineElements
    public static final int PARAMETER_ARRAY = 0;
    public static final int TABLE_NAME = PARAMETER_ARRAY + 1;
    public static final int DYNAMIC_RESULT_SET_COUNT = TABLE_NAME + 1;
    public static final int LANGUAGE = DYNAMIC_RESULT_SET_COUNT + 1;
    public static final int EXTERNAL_NAME = LANGUAGE + 1;
    public static final int PARAMETER_STYLE = EXTERNAL_NAME + 1;
    public static final int SQL_CONTROL = PARAMETER_STYLE + 1;
    public static final int DETERMINISTIC = SQL_CONTROL + 1;
    public static final int NULL_ON_NULL_INPUT = DETERMINISTIC + 1;
    public static final int RETURN_TYPE = NULL_ON_NULL_INPUT + 1;
    public static final int ROUTINE_SECURITY_DEFINER = RETURN_TYPE + 1;
    public static final int INLINE_DEFINITION = ROUTINE_SECURITY_DEFINER + 1;

    // Keep ROUTINE_ELEMENT_COUNT last (determines set cardinality).
    // Note: Remember to also update the map ROUTINE_CLAUSE_NAMES in
    // SQLGrammar.jj when elements are added.
    public static final int ROUTINE_ELEMENT_COUNT = INLINE_DEFINITION + 1;

    private String javaClassName;
    private String methodName;
    private AliasInfo.Type aliasType; 
    private AliasInfo aliasInfo;
    private String definition;

    /**
     * Initializer for a CreateAliasNode
     *
     * @param aliasName The name of the alias
     * @param targetObject Target name
     * @param methodName The method name
     * @param aliasType The alias type
     *
     * @exception StandardException Thrown on error
     */
    public void init(Object aliasName,
                     Object targetObject,
                     Object methodName,
                     Object aliasSpecificInfo,
                     Object aliasType) 
            throws StandardException {
        TableName qn = (TableName)aliasName;
        this.aliasType = (AliasInfo.Type)aliasType;

        initAndCheck(qn);

        switch (this.aliasType) {
        case UDT:
            this.javaClassName = (String)targetObject;
            aliasInfo = new UDTAliasInfo();

            implicitCreateSchema = true;
            break;
                                
        case PROCEDURE:
        case FUNCTION:
            {
                //routineElements contains the description of the procedure.
                // 
                // 0 - Object[] 3 element array for parameters
                // 1 - TableName - specific name
                // 2 - Integer - dynamic result set count
                // 3 - String language
                // 4 - String external name (also passed directly to create alias node - ignore
                // 5 - ParameterStyle parameter style 
                // 6 - SQLAllowed - SQL control
                // 7 - Boolean - whether the routine is DETERMINISTIC
                // 8 - Boolean - CALLED ON NULL INPUT (always TRUE for procedures)
                // 9 - DataTypeDescriptor - return type (always NULL for procedures)

                Object[] routineElements = (Object[])aliasSpecificInfo;
                Object[] parameters = (Object[])routineElements[PARAMETER_ARRAY];
                int paramCount = ((List)parameters[0]).size();

                String[] names = null;
                DataTypeDescriptor[] types = null;
                int[] modes = null;

                if (paramCount != 0) {

                    names = new String[paramCount];
                    ((List<String>)parameters[0]).toArray(names);

                    types = new DataTypeDescriptor[paramCount];
                    ((List<DataTypeDescriptor>)parameters[1]).toArray(types);

                    modes = new int[paramCount];
                    for (int i = 0; i < paramCount; i++) {
                        int currentMode = ((List<Integer>)parameters[2]).get(i).intValue();
                        modes[i] = currentMode;
                    }

                    if (paramCount > 1) {
                        String[] dupNameCheck = new String[paramCount];
                        System.arraycopy(names, 0, dupNameCheck, 0, paramCount);
                        java.util.Arrays.sort(dupNameCheck);
                        for (int dnc = 1; dnc < dupNameCheck.length; dnc++) {
                            if (! dupNameCheck[dnc].equals("") && dupNameCheck[dnc].equals(dupNameCheck[dnc - 1]))
                                throw new StandardException("Duplicate parameter name");
                        }
                    }
                }

                Integer drso = (Integer)routineElements[DYNAMIC_RESULT_SET_COUNT];
                int drs = drso == null ? 0 : drso.intValue();

                RoutineAliasInfo.SQLAllowed sqlAllowed = (RoutineAliasInfo.SQLAllowed)routineElements[SQL_CONTROL];
                if (sqlAllowed == null)
                    sqlAllowed = (this.aliasType == AliasInfo.Type.PROCEDURE ?
                                  RoutineAliasInfo.SQLAllowed.MODIFIES_SQL_DATA : RoutineAliasInfo.SQLAllowed.READS_SQL_DATA);

                Boolean isDeterministicO = (Boolean)routineElements[DETERMINISTIC];
                boolean isDeterministic = (isDeterministicO == null) ? false : isDeterministicO.booleanValue();

                Boolean definersRightsO = (Boolean)routineElements[ROUTINE_SECURITY_DEFINER];
                boolean definersRights  = (definersRightsO == null) ? false : definersRightsO.booleanValue();

                Boolean calledOnNullInputO = (Boolean)routineElements[NULL_ON_NULL_INPUT];
                boolean calledOnNullInput = (calledOnNullInputO == null) ? false : calledOnNullInputO.booleanValue();

                DataTypeDescriptor returnType = (DataTypeDescriptor)routineElements[RETURN_TYPE];

                String language = (String)routineElements[LANGUAGE];
                RoutineAliasInfo.ParameterStyle pstyle = (RoutineAliasInfo.ParameterStyle)routineElements[PARAMETER_STYLE];
                if (pstyle == null) {
                    pstyle = language.equalsIgnoreCase("JAVA") ? RoutineAliasInfo.ParameterStyle.JAVA : RoutineAliasInfo.ParameterStyle.ENVIRONMENT;
                }
                
                this.definition = (String)routineElements[INLINE_DEFINITION];
                if (this.definition == null) {
                    switch (pstyle) {
                    case JAVA:
                    case DERBY_JDBC_RESULT_SET:
                        this.javaClassName = (String)targetObject;
                        this.methodName = (String)methodName;
                        break;
                    case AKIBAN_LOADABLE_PLAN:
                        this.javaClassName = (String)targetObject + "." + (String)methodName;
                        break;
                    }
                }
                aliasInfo = new RoutineAliasInfo(this.methodName,
                                                 paramCount,
                                                 names,
                                                 types,
                                                 modes,
                                                 drs,
                                                 language,
                                                 pstyle,
                                                 sqlAllowed,
                                                 isDeterministic,
                                                 definersRights,
                                                 calledOnNullInput,
                                                 returnType);

                implicitCreateSchema = true;
            }
            break;

        case SYNONYM:
            String targetSchema = null;
            implicitCreateSchema = true;
            TableName t = (TableName)targetObject;
            if (t.getSchemaName() != null)
                targetSchema = t.getSchemaName();
            aliasInfo = new SynonymAliasInfo(targetSchema, t.getTableName());
            break;

        default:
            assert false : "Unexpected value for aliasType " + aliasType;
        }
    }

    public String getJavaClassName() {
        return javaClassName;
    }

    public String getMethodName() {
        return methodName;
    }

    public AliasInfo.Type getAliasType() {
        return aliasType;
    }

    public AliasInfo getAliasInfo() {
        return aliasInfo;
    }

    public String getDefinition() {
        return definition;
    }

    /**
     * Convert this object to a String.  See comments in QueryTreeNode.java
     * for how this should be done for tree printing.
     *
     * @return This object as a String
     */

    public String toString() {
        return "aliasType: " + aliasType + "\n" +
            "aliasInfo: " + aliasInfo + "\n" +
            ((definition != null) ? 
             ("definition: " + definition + "\n") :
             ("javaClassName: " + javaClassName + "\n" +
             "methodName: " + methodName + "\n")) +
            super.toString();
    }

    /**
     * Fill this node with a deep copy of the given node.
     */
    public void copyFrom(QueryTreeNode node) throws StandardException {
        super.copyFrom(node);

        CreateAliasNode other = (CreateAliasNode)node;
        this.javaClassName = other.javaClassName;
        this.methodName = other.methodName;
        this.definition = other.definition;
        this.aliasType = other.aliasType; 
        this.aliasInfo = other.aliasInfo; // TODO: Clone?
    }

    public String statementToString() {
        switch (this.aliasType) {
        case UDT:
            return "CREATE TYPE";
        case PROCEDURE:
            return "CREATE PROCEDURE";
        case SYNONYM:
            return "CREATE SYNONYM";
        default:
            return "CREATE FUNCTION";
        }
    }

}
