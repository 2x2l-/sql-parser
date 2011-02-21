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

   Derby - Class org.apache.derby.impl.sql.compile.UnaryDateTimestampOperatorNode

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
import com.akiban.sql.types.DataTypeDescriptor;

import java.sql.Types;

/**
 * This class implements the timestamp(x) and date(x) functions.
 *
 * These two functions implement a few special cases of string conversions beyond the normal string to
 * date/timestamp casts.
 */
public class UnaryDateTimestampOperatorNode extends UnaryOperatorNode
{
  private static final String TIMESTAMP_METHOD_NAME = "getTimestamp";
  private static final String DATE_METHOD_NAME = "getDate";
    
  /**
   * @param operand The operand of the function
   * @param targetType The type of the result. Timestamp or Date.
   *
   * @exception StandardException Thrown on error
   */

  public void init(Object operand, Object targetType) throws StandardException {
    setType((DataTypeDescriptor)targetType);
    switch(getType().getJDBCTypeId()) {
    case Types.DATE:
      super.init(operand, "date", DATE_METHOD_NAME);
      break;

    case Types.TIMESTAMP:
      super.init(operand, "timestamp", TIMESTAMP_METHOD_NAME);
      break;

    default:
      assert false;
      super.init(operand);
    }
  }
    
}
