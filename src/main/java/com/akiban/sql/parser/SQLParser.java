/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

/**
 * SQL Parser.
 *
 */

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

// TODO: The Derby exception handling coordinated localized messages
// and SQLSTATE values, which will be needed, but in the context of
// the new engine.

package com.akiban.sql.parser;

import com.akiban.sql.StandardException;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLParser implements SQLParserContext {
    private String sqlText;
    private List<ValueNode> parameterList;
    private boolean returnParameterFlag;
    private Map printedObjectsMap;
    private int generatedColumnNameIndex;
    
    static final int LARGE_TOKEN_SIZE = 128;

    private CharStream charStream = null;
    private SQLGrammarTokenManager tokenManager = null;
    private SQLGrammar parser = null;

    private int maxStringLiteralLength = 65535;
    /* Identifiers (Constraint, Cursor, Function/Procedure, Index,
     * Trigger, Column, Schema, Savepoint, Table and View names) are
     * limited to 128.
     */ 
    private int maxIdentifierLength = 128;

    // TODO: Needs much more thought.
    private String messageLocale = null;

    NodeFactory nodeFactory;

    /** Make a new parser.
     * Parser can be reused.
     */
    public SQLParser() {
        nodeFactory = new NodeFactoryImpl();
    }

    /** Return the SQL string this parser just parsed. */
    public String getSQLText() {
        return sqlText;
    }

    /** Return the parameters to the parsed statement. */
    public List<ValueNode> getParameterList() {
        return parameterList;
    }

    /**
     * Looks up an unnamed parameter given its parameter number.
     *
     * @param paramNumber Number of parameter in unnamedparameter list.
     *
     * @return corresponding unnamed parameter.
     *
     */
    public ParameterNode lookupUnnamedParameter(int paramNumber) {
        ParameterNode unnamedParameter;
        unnamedParameter = (ParameterNode)parameterList.get(paramNumber);
        return unnamedParameter;
    }

    /** Normal external parser entry. */
    public StatementNode parseStatement(String sqlText) 
            throws StandardException {
        this.sqlText = sqlText;
        Reader reader = new StringReader(sqlText);
        if (charStream == null) {
            charStream = new UCode_CharStream(reader, 1, 1, LARGE_TOKEN_SIZE);
        }
        else {
            charStream.ReInit(reader, 1, 1, LARGE_TOKEN_SIZE);
        }
        if (tokenManager == null) {
            tokenManager = new SQLGrammarTokenManager(charStream);
        } 
        else {
            tokenManager.ReInit(charStream);
        }
        if (parser == null) {
            parser = new SQLGrammar(tokenManager);
            parser.setParserContext(this);
        }
        else {
            parser.ReInit(tokenManager);
        }
        parameterList = new ArrayList<ValueNode>();
        returnParameterFlag = false;
        printedObjectsMap = null;
        generatedColumnNameIndex = 1;
        try {
            return parser.parseStatement(sqlText, parameterList);
        }
        catch (ParseException ex) {
            throw new StandardException(ex);
        }
        catch (TokenMgrError ex) {
            // Throw away the cached parser.
            parser = null;
            throw new StandardException(ex);
        }
    }

    /** Get maximum length of a string literal. */
    public int getMaxStringLiteralLength() {
        return maxStringLiteralLength;
    }
    /** Set maximum length of a string literal. */
    public void setMaxStringLiteralLength(int maxLength) {
        maxStringLiteralLength = maxLength;
    }

    /** Check that string literal is not too long. */
    public void checkStringLiteralLengthLimit(String image) throws StandardException {
        if (image.length() > maxStringLiteralLength) {
            throw new StandardException("String literal too long");
        }
    }

    /** Get maximum length of an identifier. */
    public int getMaxIdentifierLength() {
        return maxIdentifierLength;
    }
    /** Set maximum length of an identifier. */
    public void setMaxIdentifierLength(int maxLength) {
        maxIdentifierLength = maxLength;
    }

    /**
     * Check that identifier is not too long.
     */
    public void checkIdentifierLengthLimit(String identifier)
            throws StandardException {
        if (identifier.length() > maxIdentifierLength)
            throw new StandardException("Identifier too long: '" + identifier + "'");
    }

    public void setReturnParameterFlag() {
        returnParameterFlag = true;
    }

    public String getMessageLocale() {
        return messageLocale;
    }
    public void setMessageLocale(String locale) {
        messageLocale = locale;
    }

    /** Get a node factory. */
    public NodeFactory getNodeFactory() {
        return nodeFactory;
    }

    /** Set the node factory. */
    public void setNodeFactory(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    /**
     * Return a map of AST nodes that have already been printed during a
     * compiler phase, so as to be able to avoid printing a node more than once.
     * @see org.apache.derby.impl.sql.compile.QueryTreeNode#treePrint(int)
     * @return the map
     */
    public Map getPrintedObjectsMap() {
        if (printedObjectsMap == null)
            printedObjectsMap = new HashMap();
        return printedObjectsMap;
    }

    public String generateColumnName() {
        return "_SQL_COL_" + generatedColumnNameIndex++;
    }

}
