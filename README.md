# FoundationDB SQL Parser

## Overview

The FoundationDB SQL Parser is a complete, production-quality Java parser for
the SQL language. It defines the SQL grammar as implemented by the FoundationDB
SQL Layer but can be used independently. It is derived from the Apache Derby
parser.


## Building From Source

[Maven](http://maven.apache.org) is used to build, test, and deploy.

Run tests and create jars

    mvn package

The resulting jar files are in the `target` directory.

Generate the documentation

    mvn javadoc:javadoc

The resulting Javadoc HTML files are in `target/site/apidocs`.


## Using From Maven

The FoundationDB SQL Parser is in the standard Maven Central repository. Any
Maven project can use it directly by adding the appropriate entries to the
`dependencies` section of its `pom.xml` file:

Note:

> This project was recently renamed from `akiban-sql-parser` and can be
> found as such in Maven Central until the next release.

```xml
<dependencies>
  <dependency>
    <groupId>com.akiban</groupId>
    <artifactId>akiban-sql-parser</artifactId>
    <version>1.0.16</version>
  </dependency>
</dependencies>
```


## Installing From Binaries

Pre-built jars can be downloaded directly from the
[release page](https://github.com/foundationdb/sql-parser/releases).
Expand the package into a into a convenient directory using the appropriate
utility (e.g. `unzip` or `tar`).

The SQL Parser is licensed under the Apache License, Version 2.0. The full text
can be found in the `LICENSE.txt` file.


## Working with the SQL Parser

The following example demonstrates a simple usage:

```java
import com.foundationdb.sql.parser.SQLParser;
import com.foundationdb.sql.parser.StatementNode;

public class ParserHello {
    public static void main(String[] args) throws Exception {
        SQLParser parser = new SQLParser();
        for(String s : args) {
            StatementNode stmt = parser.parseStatement(s);
            stmt.treePrint();
        }
    }
}
```

A new [SQLParser](http://foundationdb.github.io/sql-parser/javadoc/com/foundationdb/sql/parser/SQLParser.html)
is instantiated and each command line argument is
[parsed](http://foundationdb.github.io/sql-parser/javadoc/com/foundationdb/sql/parser/SQLParser.html#parseStatement%28java.lang.String%29)
and [printed](http://foundationdb.github.io/sql-parser/javadoc/com/foundationdb/sql/parser/QueryTreeNode.html#treePrint%28%29)
to standard output. The result is a debug dump of all nodes in the underlying Abstract Syntax Tree.
More advanced usages will generally parse a statement and then pass a custom
[Visitor](http://foundationdb.github.io/sql-parser/javadoc/com/foundationdb/sql/parser/Visitor.html) to the
[accept()](http://foundationdb.github.io/sql-parser/javadoc/com/foundationdb/sql/parser/QueryTreeNode.html#accept%28com.foundationdb.sql.parser.Visitor%29) method.

To compile and run the example from the command line, copy the code into a
file named `ParserHello.java` and ensure the parser jar file, found in
the root directory of the binary package, is in your `classpath`.

For example,

    $ export CLASSPATH="fdb-sql-parser-1.0.16.jar:."

Compile:

    $ javac ParserHello.java

Run (output trimmed):

    $ java ParserHello "SELECT a FROM b"
    com.foundationdb.sql.parser.CursorNode@5889dee2
    statementType: SELECT
    resultSet:
        com.foundationdb.sql.parser.SelectNode@4387f4d7
        resultColumns:
            [0]:
            com.foundationdb.sql.parser.ResultColumn@5123968
            name: a
            expression:
                com.foundationdb.sql.parser.ColumnReference@6f76dd71
                columnName: a
        fromList:
            [0]:
            com.foundationdb.sql.parser.FromBaseTable@18317b1d
            tableName: b


## More Information

To get in touch,

    - Visit our Q&A site at [community.foundationdb.com](http://community.foundationdb.com)
    - Hop on #foundationdb on irc.freenode.net

