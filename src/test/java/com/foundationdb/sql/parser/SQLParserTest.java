/**
 * Copyright 2011-2013 FoundationDB, LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foundationdb.sql.parser;

import com.foundationdb.sql.TestBase;

import com.foundationdb.sql.parser.SQLParser;
import com.foundationdb.sql.parser.StatementNode;

import org.junit.Before;
import org.junit.Test;
import static junit.framework.Assert.*;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runner.RunWith;

import java.io.*;
import java.util.*;

@RunWith(Parameterized.class)
public class SQLParserTest extends TestBase implements TestBase.GenerateAndCheckResult
{
    public static final File RESOURCE_DIR = 
        new File("src/test/resources/"
                 + SQLParserTest.class.getPackage().getName().replace('.', '/'));

    protected SQLParser parser;
    protected File featuresFile;

    @Before
    public void before() throws Exception {
        parser = new SQLParser();
        if (featuresFile != null)
            parseFeatures(featuresFile, parser.getFeatures());
    }

    protected void parseFeatures(File file, Set<SQLParserFeature> features) 
            throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                boolean add;
                switch (line.charAt(0)) {
                case '+':
                    add = true;
                    break;
                case '-':
                    add = false;
                    break;
                default:
                    throw new IOException("Malformed features line: should start with + or - " + line);
                }
                SQLParserFeature feature = SQLParserFeature.valueOf(line.substring(1));
                if (add)
                    features.add(feature);
                else
                    features.remove(feature);
            }
        }
        finally {
            reader.close();
        }
    }

    protected String getTree(StatementNode stmt) throws IOException {
        StringWriter str = new StringWriter();
        stmt.treePrint(str);
        return str.toString().trim();
    }

    @Parameters(name="{0}")
    public static Collection<Object[]> queries() throws Exception {
        Collection<Object[]> result = new ArrayList<Object[]>();
        for (Object[] args : sqlAndExpected(RESOURCE_DIR)) {
            File featuresFile = new File(RESOURCE_DIR, args[0] + ".features");
            if (!featuresFile.exists())
                featuresFile = null;
            Object[] nargs = new Object[args.length+1];
            nargs[0] = args[0];
            nargs[1] = featuresFile;
            System.arraycopy(args, 1, nargs, 2, args.length-1);
            result.add(nargs);
        }
        return result;
    }

    public SQLParserTest(String caseName, File featuresFile,
                         String sql, String expected, String error) {
        super(caseName, sql, expected, error);
        this.featuresFile = featuresFile;
    }

    @Test
    public void testParser() throws Exception {
        generateAndCheckResult();
    }

    @Override
    public String generateResult() throws Exception {
        StatementNode stmt = parser.parseStatement(sql);
        return getTree(stmt);
    }

    @Override
    public void checkResult(String result) throws IOException {
        assertEqualsWithoutHashes(caseName, expected, result);
    }

}
