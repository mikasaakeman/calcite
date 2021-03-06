/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.test;

import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.test.SqlTestFactory;
import org.apache.calcite.sql.test.SqlTester;
import org.apache.calcite.sql.test.SqlValidatorTester;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.type.SqlTypeUtil;
import org.apache.calcite.sql.validate.implicit.AbstractTypeCoercion;
import org.apache.calcite.sql.validate.implicit.TypeCoercion;
import org.apache.calcite.test.catalog.MockCatalogReader;
import org.apache.calcite.util.Pair;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Test cases for implicit type coercion. see {@link TypeCoercion} doc
 * or <a href="https://docs.google.com/spreadsheets/d/1GhleX5h5W8-kJKh7NMJ4vtoE78pwfaZRJl88ULX_MgU/edit?usp=sharing">CalciteImplicitCasts</a>
 * for conversion details.
 */
class TypeCoercionTest extends SqlValidatorTestCase {
  private TypeCoercion typeCoercion;
  private RelDataTypeFactory dataTypeFactory;
  private SqlTestFactory.MockCatalogReaderFactory catalogReaderFactory;

  // type category.
  private ImmutableList<RelDataType> numericTypes;
  private ImmutableList<RelDataType> atomicTypes;
  private ImmutableList<RelDataType> allTypes;
  private ImmutableList<RelDataType> charTypes;
  private ImmutableList<RelDataType> binaryTypes;
  private ImmutableList<RelDataType> booleanTypes;

  // single types
  private RelDataType nullType;
  private RelDataType booleanType;
  private RelDataType tinyintType;
  private RelDataType smallintType;
  private RelDataType intType;
  private RelDataType bigintType;
  private RelDataType floatType;
  private RelDataType doubleType;
  private RelDataType decimalType;
  private RelDataType dateType;
  private RelDataType timeType;
  private RelDataType timestampType;
  private RelDataType binaryType;
  private RelDataType varbinaryType;
  private RelDataType charType;
  private RelDataType varcharType;
  private RelDataType varchar20Type;

  TypeCoercionTest() {
    // tool tester impl.
    SqlTester tester1 = new SqlValidatorTester(SqlTestFactory.INSTANCE);
    this.typeCoercion = tester1.getValidator().getTypeCoercion();
    this.dataTypeFactory = tester1.getValidator().getTypeFactory();
    initializeSingleTypes();
    initializeCategoryTypes();
    // sql validator tester.
    catalogReaderFactory = (factory, caseSensitive) ->
        new TCatalogReader(this.dataTypeFactory, caseSensitive).init();
    tester = getTester();
  }

  //~ fields initialize ------------------------------------------------------

  private void initializeSingleTypes() {
    nullType = dataTypeFactory.createSqlType(SqlTypeName.NULL);
    booleanType = dataTypeFactory.createSqlType(SqlTypeName.BOOLEAN);
    tinyintType = dataTypeFactory.createSqlType(SqlTypeName.TINYINT);
    smallintType = dataTypeFactory.createSqlType(SqlTypeName.SMALLINT);
    intType = dataTypeFactory.createSqlType(SqlTypeName.INTEGER);
    bigintType = dataTypeFactory.createSqlType(SqlTypeName.BIGINT);
    floatType = dataTypeFactory.createSqlType(SqlTypeName.FLOAT);
    doubleType = dataTypeFactory.createSqlType(SqlTypeName.DOUBLE);
    decimalType = dataTypeFactory.createSqlType(SqlTypeName.DECIMAL);
    dateType = dataTypeFactory.createSqlType(SqlTypeName.DATE);
    timeType = dataTypeFactory.createSqlType(SqlTypeName.TIME);
    timestampType = dataTypeFactory.createSqlType(SqlTypeName.TIMESTAMP);
    binaryType = dataTypeFactory.createSqlType(SqlTypeName.BINARY);
    varbinaryType = dataTypeFactory.createSqlType(SqlTypeName.VARBINARY);
    charType = dataTypeFactory.createSqlType(SqlTypeName.CHAR);
    varcharType = dataTypeFactory.createSqlType(SqlTypeName.VARCHAR);
    varchar20Type = dataTypeFactory.createSqlType(SqlTypeName.VARCHAR, 20);
  }

  private void initializeCategoryTypes() {
    // INT
    ImmutableList.Builder<RelDataType> builder = ImmutableList.builder();
    for (SqlTypeName typeName : SqlTypeName.INT_TYPES) {
      builder.add(dataTypeFactory.createSqlType(typeName));
    }
    numericTypes = builder.build();
    // ATOMIC
    ImmutableList.Builder<RelDataType> builder3 = ImmutableList.builder();
    for (SqlTypeName typeName : SqlTypeName.DATETIME_TYPES) {
      builder3.add(dataTypeFactory.createSqlType(typeName));
    }
    builder3.addAll(numericTypes);
    for (SqlTypeName typeName : SqlTypeName.STRING_TYPES) {
      builder3.add(dataTypeFactory.createSqlType(typeName));
    }
    for (SqlTypeName typeName : SqlTypeName.BOOLEAN_TYPES) {
      builder3.add(dataTypeFactory.createSqlType(typeName));
    }
    atomicTypes = builder3.build();
    // COMPLEX
    ImmutableList.Builder<RelDataType> builder4 = ImmutableList.builder();
    builder4.add(dataTypeFactory.createArrayType(intType, -1));
    builder4.add(dataTypeFactory.createArrayType(varcharType, -1));
    builder4.add(dataTypeFactory.createMapType(varcharType, varcharType));
    builder4.add(dataTypeFactory.createStructType(ImmutableList.of(Pair.of("a1", varcharType))));
    List<? extends Map.Entry<String, RelDataType>> ll =
        ImmutableList.of(Pair.of("a1", varbinaryType), Pair.of("a2", intType));
    builder4.add(dataTypeFactory.createStructType(ll));
    ImmutableList<RelDataType> complexTypes = builder4.build();
    // ALL
    SqlIntervalQualifier intervalQualifier =
        new SqlIntervalQualifier(TimeUnit.DAY, TimeUnit.MINUTE, SqlParserPos.ZERO);
    allTypes = combine(atomicTypes, complexTypes,
        ImmutableList.of(nullType, dataTypeFactory.createSqlIntervalType(intervalQualifier)));

    // CHARACTERS
    ImmutableList.Builder<RelDataType> builder6 = ImmutableList.builder();
    for (SqlTypeName typeName : SqlTypeName.CHAR_TYPES) {
      builder6.add(dataTypeFactory.createSqlType(typeName));
    }
    charTypes = builder6.build();
    // BINARY
    ImmutableList.Builder<RelDataType> builder7 = ImmutableList.builder();
    for (SqlTypeName typeName : SqlTypeName.BINARY_TYPES) {
      builder7.add(dataTypeFactory.createSqlType(typeName));
    }
    binaryTypes = builder7.build();
    // BOOLEAN
    ImmutableList.Builder<RelDataType> builder8 = ImmutableList.builder();
    for (SqlTypeName typeName : SqlTypeName.BOOLEAN_TYPES) {
      builder8.add(dataTypeFactory.createSqlType(typeName));
    }
    booleanTypes = builder8.build();
  }

  //~ Tool methods -----------------------------------------------------------

  private RelDataType arrayType(RelDataType type) {
    return dataTypeFactory.createArrayType(type, -1);
  }

  private RelDataType mapType(RelDataType keyType, RelDataType valType) {
    return dataTypeFactory.createMapType(keyType, valType);
  }

  private RelDataType recordType(String name, RelDataType type) {
    return dataTypeFactory.createStructType(ImmutableList.of(Pair.of(name, type)));
  }

  private RelDataType recordType(List<? extends Map.Entry<String, RelDataType>> pairs) {
    return dataTypeFactory.createStructType(pairs);
  }

  private RelDataType decimalType(int precision, int scale) {
    return dataTypeFactory.createSqlType(SqlTypeName.DECIMAL, precision, scale);
  }

  /** Decision method for {@link AbstractTypeCoercion#implicitCast}. */
  private void shouldCast(
      RelDataType from,
      SqlTypeFamily family,
      RelDataType expected) {
    if (family == null) {
      // ROW type do not have a family.
      return;
    }
    RelDataType castedType = ((AbstractTypeCoercion) typeCoercion).implicitCast(from, family);
    boolean equals = castedType != null
        && (from.equals(castedType)
        || SqlTypeUtil.equalSansNullability(dataTypeFactory, castedType, expected)
        || expected.getSqlTypeName().getFamily().contains(castedType));
    assert equals
        : "Failed to cast from "
        + from.getSqlTypeName()
        + " to "
        + family;
  }

  private void shouldNotCast(
      RelDataType from,
      SqlTypeFamily family) {
    if (family == null) {
      // ROW type do not have a family.
      return;
    }
    RelDataType castedType = ((AbstractTypeCoercion) typeCoercion).implicitCast(from, family);
    assert castedType == null
        : "Should not be able to cast from "
        + from.getSqlTypeName()
        + " to "
        + family;
  }

  private void checkShouldCast(RelDataType checked, List<RelDataType> types) {
    for (RelDataType type : allTypes) {
      if (contains(types, type)) {
        shouldCast(checked, type.getSqlTypeName().getFamily(), type);
      } else {
        shouldNotCast(checked, type.getSqlTypeName().getFamily());
      }
    }
  }

  // some data types has the same type family, i.e. TIMESTAMP and
  // TIMESTAMP_WITH_LOCAL_TIME_ZONE all have TIMESTAMP family.
  private static boolean contains(List<RelDataType> types, RelDataType type) {
    for (RelDataType type1 : types) {
      if (type1.equals(type)
          || type1.getSqlTypeName().getFamily() == type.getSqlTypeName().getFamily()) {
        return true;
      }
    }
    return false;
  }

  private boolean equals(Object o1, Object o2) {
    if (o1 == null && o2 != null
        || o1 != null && o2 == null) {
      return false;
    }
    return o1 == o2;
  }

  private String toStringNullable(Object o1) {
    if (o1 == null) {
      return "NULL";
    }
    return o1.toString();
  }

  /** Decision method for finding a common type. */
  private void checkCommonType(
      RelDataType type1,
      RelDataType type2,
      RelDataType expected,
      boolean isSymmetric) {
    RelDataType result = typeCoercion.getTightestCommonType(type1, type2);
    assert equals(result, expected)
        : "Expected "
        + toStringNullable(expected)
        + " as common type for "
        + type1.toString()
        + " and "
        + type2.toString()
        + ", but found "
        + toStringNullable(result);
    if (isSymmetric) {
      RelDataType result1 = typeCoercion.getTightestCommonType(type2, type1);
      assert equals(result1, expected)
          : "Expected "
          + toStringNullable(expected)
          + " as common type for "
          + type2.toString()
          + " and "
          + type1.toString()
          + ", but found "
          + toStringNullable(result1);
    }
  }

  /** Decision method for finding a wider type. */
  private void checkWiderType(
      RelDataType type1,
      RelDataType type2,
      RelDataType expected,
      boolean stringPromotion,
      boolean isSymmetric) {
    RelDataType result = typeCoercion.getWiderTypeForTwo(type1, type2, stringPromotion);
    assert equals(result, expected)
        : "Expected "
        + toStringNullable(expected)
        + " as common type for " + type1.toString()
        + " and " + type2.toString()
        + ", but found " + toStringNullable(result);
    if (isSymmetric) {
      RelDataType result1 = typeCoercion.getWiderTypeForTwo(type2, type1, stringPromotion);
      assert equals(result1, expected)
          : "Expected "
          + toStringNullable(expected)
          + " as common type for " + type2.toString()
          + " and " + type1.toString()
          + ", but found " + toStringNullable(result1);
    }
  }

  @Override public SqlTester getTester() {
    return new SqlValidatorTester(SqlTestFactory.INSTANCE
        .withCatalogReader(getCatalogReaderFactory()));
  }

  private static ImmutableList<RelDataType> combine(
      List<RelDataType> list0,
      List<RelDataType> list1) {
    return ImmutableList.<RelDataType>builder()
        .addAll(list0)
        .addAll(list1)
        .build();
  }

  private static ImmutableList<RelDataType> combine(
      List<RelDataType> list0,
      List<RelDataType> list1,
      List<RelDataType> list2) {
    return ImmutableList.<RelDataType>builder()
        .addAll(list0)
        .addAll(list1)
        .addAll(list2)
        .build();
  }

  SqlTestFactory.MockCatalogReaderFactory getCatalogReaderFactory() {
    return catalogReaderFactory;
  }

  //~ Tests ------------------------------------------------------------------

  /**
   * Test case for {@link TypeCoercion#getTightestCommonType}.
   */
  @Test void testGetTightestCommonType() {
    // NULL
    checkCommonType(nullType, nullType, nullType, true);
    // BOOLEAN
    checkCommonType(nullType, booleanType, booleanType, true);
    checkCommonType(booleanType, booleanType, booleanType, true);
    checkCommonType(intType, booleanType, null, true);
    checkCommonType(bigintType, booleanType, null, true);
    // INT
    checkCommonType(nullType, tinyintType, tinyintType, true);
    checkCommonType(nullType, intType, intType, true);
    checkCommonType(nullType, bigintType, bigintType, true);
    checkCommonType(smallintType, intType, intType, true);
    checkCommonType(smallintType, bigintType, bigintType, true);
    checkCommonType(intType, bigintType, bigintType, true);
    checkCommonType(bigintType, bigintType, bigintType, true);
    // FLOAT/DOUBLE
    checkCommonType(nullType, floatType, floatType, true);
    checkCommonType(nullType, doubleType, doubleType, true);
    // Use RelDataTypeFactory#leastRestrictive to find the common type, it's not symmetric but
    // it's ok because precision does not become lower.
    checkCommonType(floatType, doubleType, floatType, false);
    checkCommonType(floatType, floatType, floatType, true);
    checkCommonType(doubleType, doubleType, doubleType, true);
    // EXACT + FRACTIONAL
    checkCommonType(intType, floatType, floatType, true);
    checkCommonType(intType, doubleType, doubleType, true);
    checkCommonType(bigintType, floatType, floatType, true);
    checkCommonType(bigintType, doubleType, doubleType, true);
    // Fixed precision decimal
    RelDataType decimal54 = dataTypeFactory.createSqlType(SqlTypeName.DECIMAL, 5, 4);
    RelDataType decimal71 = dataTypeFactory.createSqlType(SqlTypeName.DECIMAL, 7, 1);
    checkCommonType(decimal54, decimal71, null, true);
    checkCommonType(decimal54, doubleType, null, true);
    checkCommonType(decimal54, intType, null, true);
    // CHAR/VARCHAR
    checkCommonType(nullType, charType, charType, true);
    checkCommonType(charType, varcharType, varcharType, true);
    checkCommonType(intType, charType, null, true);
    checkCommonType(doubleType, charType, null, true);
    // TIMESTAMP
    checkCommonType(nullType, timestampType, timestampType, true);
    checkCommonType(timestampType, timestampType, timestampType, true);
    checkCommonType(dateType, timestampType, timestampType, true);
    checkCommonType(intType, timestampType, null, true);
    checkCommonType(varcharType, timestampType, null, true);
    // STRUCT
    checkCommonType(nullType, mapType(intType, charType), mapType(intType, charType), true);
    checkCommonType(nullType, recordType(ImmutableList.of()), recordType(ImmutableList.of()),
        true);
    checkCommonType(charType, mapType(intType, charType), null, true);
    checkCommonType(arrayType(intType), recordType(ImmutableList.of()), null, true);

    checkCommonType(recordType("a", intType), recordType("b", intType), null, true);
    checkCommonType(recordType("a", intType), recordType("a", intType),
        recordType("a", intType), true);
    checkCommonType(recordType("a", arrayType(intType)), recordType("a", arrayType(intType)),
        recordType("a", arrayType(intType)), true);
  }

  /** Test case for {@link TypeCoercion#getWiderTypeForTwo}
   * and {@link TypeCoercion#getWiderTypeFor}. */
  @Test void testWiderTypeFor() {
    // DECIMAL please see details in SqlTypeFactoryImpl#leastRestrictiveSqlType.
    checkWiderType(decimalType(5, 4), decimalType(7, 1), decimalType(10, 4), true, true);
    checkWiderType(decimalType(5, 4), doubleType, doubleType, true, true);
    checkWiderType(decimalType(5, 4), intType, decimalType(14, 4), true, true);
    checkWiderType(decimalType(5, 4), bigintType, decimalType(19, 0), true, true);
    // Array
    checkWiderType(arrayType(smallintType), arrayType(doubleType), arrayType(doubleType),
        true, true);
    checkWiderType(arrayType(timestampType), arrayType(varcharType), arrayType(varcharType),
        true, true);
    checkWiderType(arrayType(intType), arrayType(bigintType), arrayType(bigintType),
        true, true);
    // No string promotion
    checkWiderType(intType, charType, null, false, true);
    checkWiderType(timestampType, charType, null, false, true);
    checkWiderType(arrayType(bigintType), arrayType(charType), null, false, true);
    checkWiderType(arrayType(charType), arrayType(timestampType), null, false, true);
    // String promotion
    checkWiderType(intType, charType, varcharType, true, true);
    checkWiderType(timestampType, charType, varcharType, true, true);
    checkWiderType(arrayType(bigintType), arrayType(varcharType), arrayType(varcharType),
        true, true);
    checkWiderType(arrayType(charType), arrayType(timestampType), arrayType(varcharType),
        true, true);
  }

  /** Test set operations: UNION, INTERSECT, EXCEPT type coercion. */
  @Test void testSetOperations() {
    // union
    sql("select 1 from (values(true)) union select '2' from (values(true))")
        .type("RecordType(VARCHAR NOT NULL EXPR$0) NOT NULL");
    sql("select 1 from (values(true)) union select '2' from (values(true))"
        + "union select '3' from (values(true))")
        .type("RecordType(VARCHAR NOT NULL EXPR$0) NOT NULL");
    sql("select 1, '2' from (values(true, false)) union select '3', 4 from (values(true, false))")
        .type("RecordType(VARCHAR NOT NULL EXPR$0, VARCHAR NOT NULL EXPR$1) NOT NULL");
    sql("select '1' from (values(true)) union values 2")
        .type("RecordType(VARCHAR NOT NULL EXPR$0) NOT NULL");
    sql("select (select 1+2 from (values true)) tt from (values(true)) union values '2'")
        .type("RecordType(VARCHAR NOT NULL TT) NOT NULL");
    // union with star
    sql("select * from (values(1, '3')) union select * from (values('2', 4))")
        .type("RecordType(VARCHAR NOT NULL EXPR$0, VARCHAR NOT NULL EXPR$1) NOT NULL");
    sql("select 1 from (values(true)) union values (select '1' from (values (true)) as tt)")
        .type("RecordType(VARCHAR EXPR$0) NOT NULL");
    // union with func
    sql("select LOCALTIME from (values(true)) union values '1'")
        .type("RecordType(VARCHAR NOT NULL LOCALTIME) NOT NULL");
    sql("select t1_int, t1_decimal, t1_smallint, t1_double from t1 "
        + "union select t2_varchar20, t2_decimal, t2_float, t2_bigint from t2 "
        + "union select t1_varchar20, t1_decimal, t1_float, t1_double from t1 "
        + "union select t2_varchar20, t2_decimal, t2_smallint, t2_double from t2")
        .type("RecordType(VARCHAR NOT NULL T1_INT,"
            + " DECIMAL(19, 0) NOT NULL T1_DECIMAL,"
            + " FLOAT NOT NULL T1_SMALLINT,"
            + " DOUBLE NOT NULL T1_DOUBLE) NOT NULL");
    // (int) union (int) union (varchar(20))
    sql("select t1_int from t1 "
        + "union select t2_int from t2 "
        + "union select t1_varchar20 from t1")
        .columnType("VARCHAR NOT NULL");

    // (varchar(20)) union (int) union (int)
    sql("select t1_varchar20 from t1 "
        + "union select t2_int from t2 "
        + "union select t1_int from t1")
        .columnType("VARCHAR NOT NULL");

    // date union timestamp
    sql("select t1_date, t1_timestamp from t1\n"
        + "union select t2_timestamp, t2_date from t2")
        .type("RecordType(TIMESTAMP(0) NOT NULL T1_DATE,"
            + " TIMESTAMP(0) NOT NULL T1_TIMESTAMP) NOT NULL");

    // intersect
    sql("select t1_int, t1_decimal, t1_smallint, t1_double from t1 "
        + "intersect select t2_varchar20, t2_decimal, t2_float, t2_bigint from t2 ")
        .type("RecordType(VARCHAR NOT NULL T1_INT,"
            + " DECIMAL(19, 0) NOT NULL T1_DECIMAL,"
            + " FLOAT NOT NULL T1_SMALLINT,"
            + " DOUBLE NOT NULL T1_DOUBLE) NOT NULL");
    // except
    sql("select t1_int, t1_decimal, t1_smallint, t1_double from t1 "
        + "except select t2_varchar20, t2_decimal, t2_float, t2_bigint from t2 ")
        .type("RecordType(VARCHAR NOT NULL T1_INT,"
            + " DECIMAL(19, 0) NOT NULL T1_DECIMAL,"
            + " FLOAT NOT NULL T1_SMALLINT,"
            + " DOUBLE NOT NULL T1_DOUBLE) NOT NULL");
  }

  /** Test arithmetic expressions with string type arguments. */
  @Test void testArithmeticExpressionsWithStrings() {
    // for null type in binary arithmetic.
    expr("1 + null").ok();
    expr("1 - null").ok();
    expr("1 / null").ok();
    expr("1 * null").ok();
    expr("MOD(1, null)").ok();

    sql("select 1+'2', 2-'3', 2*'3', 2/'3', MOD(4,'3') "
        + "from (values (true, true, true, true, true))")
        .type("RecordType(INTEGER NOT NULL EXPR$0, "
            + "INTEGER NOT NULL EXPR$1, "
            + "INTEGER NOT NULL EXPR$2, "
            + "INTEGER NOT NULL EXPR$3, "
            + "DECIMAL(19, 19) "
            + "NOT NULL EXPR$4) NOT NULL");
    expr("select abs(t1_varchar20) from t1").ok();
    expr("select sum(t1_varchar20) from t1").ok();
    expr("select avg(t1_varchar20) from t1").ok();
    tester.setFor(SqlStdOperatorTable.STDDEV_POP);
    tester.setFor(SqlStdOperatorTable.STDDEV_SAMP);
    expr("select STDDEV_POP(t1_varchar20) from t1").ok();
    expr("select STDDEV_SAMP(t1_varchar20) from t1").ok();
    expr("select -(t1_varchar20) from t1").ok();
    expr("select +(t1_varchar20) from t1").ok();
    tester.setFor(SqlStdOperatorTable.VAR_POP);
    tester.setFor(SqlStdOperatorTable.VAR_SAMP);
    expr("select VAR_POP(t1_varchar20) from t1").ok();
    expr("select VAR_SAMP(t1_varchar20) from t1").ok();
    // test divide with strings
    expr("'12.3'/5")
        .columnType("INTEGER NOT NULL");
    expr("'12.3'/cast(5 as bigint)")
        .columnType("BIGINT NOT NULL");
    expr("'12.3'/cast(5 as float)")
        .columnType("FLOAT NOT NULL");
    expr("'12.3'/cast(5 as double)")
        .columnType("DOUBLE NOT NULL");
    expr("'12.3'/5.1")
        .columnType("DECIMAL(19, 18) NOT NULL");
    expr("12.3/'5.1'")
        .columnType("DECIMAL(19, 0) NOT NULL");
    // test binary arithmetic with two strings.
    expr("'12.3' + '5'")
        .columnType("DECIMAL(19, 19) NOT NULL");
    expr("'12.3' - '5'")
        .columnType("DECIMAL(19, 19) NOT NULL");
    expr("'12.3' * '5'")
        .columnType("DECIMAL(19, 19) NOT NULL");
    expr("'12.3' / '5'")
        .columnType("DECIMAL(19, 0) NOT NULL");
  }

  /** Test cases for binary comparison expressions. */
  @Test void testBinaryComparisonCoercion() {
    expr("'2' = 3").columnType("BOOLEAN NOT NULL");
    expr("'2' > 3").columnType("BOOLEAN NOT NULL");
    expr("'2' >= 3").columnType("BOOLEAN NOT NULL");
    expr("'2' < 3").columnType("BOOLEAN NOT NULL");
    expr("'2' <= 3").columnType("BOOLEAN NOT NULL");
    expr("'2' is distinct from 3").columnType("BOOLEAN NOT NULL");
    expr("'2' is not distinct from 3").columnType("BOOLEAN NOT NULL");
    // NULL operand
    expr("'2' = null").columnType("BOOLEAN");
    expr("'2' > null").columnType("BOOLEAN");
    expr("'2' >= null").columnType("BOOLEAN");
    expr("'2' < null").columnType("BOOLEAN");
    expr("'2' <= null").columnType("BOOLEAN");
    expr("'2' is distinct from null").columnType("BOOLEAN NOT NULL");
    expr("'2' is not distinct from null").columnType("BOOLEAN NOT NULL");
    // BETWEEN operator
    expr("'2' between 1 and 3").columnType("BOOLEAN NOT NULL");
    expr("NULL between 1 and 3").columnType("BOOLEAN");
    sql("select '2019-09-23' between t1_date and t1_timestamp from t1")
        .columnType("BOOLEAN NOT NULL");
    sql("select t1_date between '2019-09-23' and t1_timestamp from t1")
        .columnType("BOOLEAN NOT NULL");
    sql("select cast('2019-09-23' as date) between t1_date and t1_timestamp from t1")
        .columnType("BOOLEAN NOT NULL");
    sql("select t1_date between cast('2019-09-23' as date) and t1_timestamp from t1")
        .columnType("BOOLEAN NOT NULL");
  }

  /** Test case for case when expression and COALESCE operator. */
  @Test void testCaseWhen() {
    // coalesce
    // double int float
    sql("select COALESCE(t1_double, t1_int, t1_float) from t1")
        .type("RecordType(DOUBLE NOT NULL EXPR$0) NOT NULL");
    // bigint int decimal
    sql("select COALESCE(t1_bigint, t1_int, t1_decimal) from t1")
        .type("RecordType(DECIMAL(19, 0) NOT NULL EXPR$0) NOT NULL");
    // null int
    sql("select COALESCE(null, t1_int) from t1")
        .type("RecordType(INTEGER EXPR$0) NOT NULL");
    // timestamp varchar
    sql("select COALESCE(t1_varchar20, t1_timestamp) from t1")
        .type("RecordType(VARCHAR NOT NULL EXPR$0) NOT NULL");
    // null float int
    sql("select COALESCE(null, t1_float, t1_int) from t1")
        .type("RecordType(FLOAT EXPR$0) NOT NULL");
    // null int decimal double
    sql("select COALESCE(null, t1_int, t1_decimal, t1_double) from t1")
        .type("RecordType(DOUBLE EXPR$0) NOT NULL");
    // null float double varchar
    sql("select COALESCE(null, t1_float, t1_double, t1_varchar20) from t1")
        .type("RecordType(VARCHAR EXPR$0) NOT NULL");
    // timestamp int varchar
    sql("select COALESCE(t1_timestamp, t1_int, t1_varchar20) from t1")
        .type("RecordType(TIMESTAMP(0) NOT NULL EXPR$0) NOT NULL");
    // timestamp date
    sql("select COALESCE(t1_timestamp, t1_date) from t1")
        .type("RecordType(TIMESTAMP(0) NOT NULL EXPR$0) NOT NULL");
    // date timestamp
    sql("select COALESCE(t1_timestamp, t1_date) from t1")
        .type("RecordType(TIMESTAMP(0) NOT NULL EXPR$0) NOT NULL");
    // null date timestamp
    sql("select COALESCE(t1_timestamp, t1_date) from t1")
        .type("RecordType(TIMESTAMP(0) NOT NULL EXPR$0) NOT NULL");

    // case when
    // smallint int char
    sql("select case "
        + "when 1 > 0 then t2_smallint "
        + "when 2 > 3 then t2_int "
        + "else t2_varchar20 end from t2")
        .type("RecordType(VARCHAR NOT NULL EXPR$0) NOT NULL");
    // boolean int char
    sql("select case "
        + "when 1 > 0 then t2_boolean "
        + "when 2 > 3 then t2_int "
        + "else t2_varchar20 end from t2")
        .type("RecordType(VARCHAR NOT NULL EXPR$0) NOT NULL");
    // float decimal
    sql("select case when 1 > 0 then t2_float else t2_decimal end from t2")
        .type("RecordType(DOUBLE NOT NULL EXPR$0) NOT NULL");
    // bigint decimal
    sql("select case when 1 > 0 then t2_bigint else t2_decimal end from t2")
        .type("RecordType(DECIMAL(19, 0) NOT NULL EXPR$0) NOT NULL");
    // date timestamp
    sql("select case when 1 > 0 then t2_date else t2_timestamp end from t2")
        .type("RecordType(TIMESTAMP(0) NOT NULL EXPR$0) NOT NULL");
  }

  /** Test case for {@link AbstractTypeCoercion#implicitCast} */
  @Test void testImplicitCasts() {
    // TINYINT
    RelDataType checkedType1 = dataTypeFactory.createSqlType(SqlTypeName.TINYINT);
    checkShouldCast(checkedType1, combine(numericTypes, charTypes));
    shouldCast(checkedType1, SqlTypeFamily.DECIMAL,
        dataTypeFactory.decimalOf(checkedType1));
    shouldCast(checkedType1, SqlTypeFamily.NUMERIC, checkedType1);
    shouldCast(checkedType1, SqlTypeFamily.INTEGER, checkedType1);
    shouldCast(checkedType1, SqlTypeFamily.EXACT_NUMERIC, checkedType1);
    shouldNotCast(checkedType1, SqlTypeFamily.APPROXIMATE_NUMERIC);

    // SMALLINT
    RelDataType checkedType2 = smallintType;
    checkShouldCast(checkedType2, combine(numericTypes, charTypes));
    shouldCast(checkedType2, SqlTypeFamily.DECIMAL,
        dataTypeFactory.decimalOf(checkedType2));
    shouldCast(checkedType2, SqlTypeFamily.NUMERIC, checkedType2);
    shouldCast(checkedType2, SqlTypeFamily.INTEGER, checkedType2);
    shouldCast(checkedType2, SqlTypeFamily.EXACT_NUMERIC, checkedType2);
    shouldNotCast(checkedType2, SqlTypeFamily.APPROXIMATE_NUMERIC);

    // INT
    RelDataType checkedType3 = intType;
    checkShouldCast(checkedType3, combine(numericTypes, charTypes));
    shouldCast(checkedType3, SqlTypeFamily.DECIMAL,
        dataTypeFactory.decimalOf(checkedType3));
    shouldCast(checkedType3, SqlTypeFamily.NUMERIC, checkedType3);
    shouldCast(checkedType3, SqlTypeFamily.INTEGER, checkedType3);
    shouldCast(checkedType3, SqlTypeFamily.EXACT_NUMERIC, checkedType3);
    shouldNotCast(checkedType3, SqlTypeFamily.APPROXIMATE_NUMERIC);

    // BIGINT
    RelDataType checkedType4 = bigintType;
    checkShouldCast(checkedType4, combine(numericTypes, charTypes));
    shouldCast(checkedType4, SqlTypeFamily.DECIMAL,
        dataTypeFactory.decimalOf(checkedType4));
    shouldCast(checkedType4, SqlTypeFamily.NUMERIC, checkedType4);
    shouldCast(checkedType4, SqlTypeFamily.INTEGER, checkedType4);
    shouldCast(checkedType4, SqlTypeFamily.EXACT_NUMERIC, checkedType4);
    shouldNotCast(checkedType4, SqlTypeFamily.APPROXIMATE_NUMERIC);

    // FLOAT/REAL
    RelDataType checkedType5 = floatType;
    checkShouldCast(checkedType5, combine(numericTypes, charTypes));
    shouldCast(checkedType5, SqlTypeFamily.DECIMAL,
        dataTypeFactory.decimalOf(checkedType5));
    shouldCast(checkedType5, SqlTypeFamily.NUMERIC, checkedType5);
    shouldNotCast(checkedType5, SqlTypeFamily.INTEGER);
    shouldCast(checkedType5, SqlTypeFamily.EXACT_NUMERIC,
        dataTypeFactory.decimalOf(checkedType5));
    shouldCast(checkedType5, SqlTypeFamily.APPROXIMATE_NUMERIC, checkedType5);

    // DOUBLE
    RelDataType checkedType6 = doubleType;
    checkShouldCast(checkedType6, combine(numericTypes, charTypes));
    shouldCast(checkedType6, SqlTypeFamily.DECIMAL,
        dataTypeFactory.decimalOf(checkedType6));
    shouldCast(checkedType6, SqlTypeFamily.NUMERIC, checkedType6);
    shouldNotCast(checkedType6, SqlTypeFamily.INTEGER);
    shouldCast(checkedType6, SqlTypeFamily.EXACT_NUMERIC,
        dataTypeFactory.decimalOf(checkedType5));
    shouldCast(checkedType6, SqlTypeFamily.APPROXIMATE_NUMERIC, checkedType6);

    // DECIMAL(10, 2)
    RelDataType checkedType7 = decimalType(10, 2);
    checkShouldCast(checkedType7, combine(numericTypes, charTypes));
    shouldCast(checkedType7, SqlTypeFamily.DECIMAL,
        dataTypeFactory.decimalOf(checkedType7));
    shouldCast(checkedType7, SqlTypeFamily.NUMERIC, checkedType7);
    shouldNotCast(checkedType7, SqlTypeFamily.INTEGER);
    shouldCast(checkedType7, SqlTypeFamily.EXACT_NUMERIC, checkedType7);
    shouldNotCast(checkedType7, SqlTypeFamily.APPROXIMATE_NUMERIC);

    // BINARY
    RelDataType checkedType8 = binaryType;
    checkShouldCast(checkedType8, combine(binaryTypes, charTypes));
    shouldNotCast(checkedType8, SqlTypeFamily.DECIMAL);
    shouldNotCast(checkedType8, SqlTypeFamily.NUMERIC);
    shouldNotCast(checkedType8, SqlTypeFamily.INTEGER);

    // BOOLEAN
    RelDataType checkedType9 = booleanType;
    checkShouldCast(checkedType9, combine(booleanTypes, charTypes));
    shouldNotCast(checkedType9, SqlTypeFamily.DECIMAL);
    shouldNotCast(checkedType9, SqlTypeFamily.NUMERIC);
    shouldNotCast(checkedType9, SqlTypeFamily.INTEGER);

    // CHARACTER
    RelDataType checkedType10 = varcharType;
    ImmutableList.Builder<RelDataType> builder = ImmutableList.builder();
    for (RelDataType type : atomicTypes) {
      if (!SqlTypeUtil.isBoolean(type)) {
        builder.add(type);
      }
    }
    checkShouldCast(checkedType10, builder.build());
    shouldCast(checkedType10, SqlTypeFamily.DECIMAL,
        SqlTypeUtil.getMaxPrecisionScaleDecimal(dataTypeFactory));
    shouldCast(checkedType10, SqlTypeFamily.NUMERIC,
        SqlTypeUtil.getMaxPrecisionScaleDecimal(dataTypeFactory));
    shouldNotCast(checkedType10, SqlTypeFamily.BOOLEAN);

    // DATE
    RelDataType checkedType11 = dateType;
    checkShouldCast(
        checkedType11,
        combine(ImmutableList.of(timestampType, checkedType11),
            charTypes));
    shouldNotCast(checkedType11, SqlTypeFamily.DECIMAL);
    shouldNotCast(checkedType11, SqlTypeFamily.NUMERIC);
    shouldNotCast(checkedType11, SqlTypeFamily.INTEGER);

    // TIME
    RelDataType checkedType12 = timeType;
    checkShouldCast(
        checkedType12,
        combine(ImmutableList.of(checkedType12), charTypes));
    shouldNotCast(checkedType12, SqlTypeFamily.DECIMAL);
    shouldNotCast(checkedType12, SqlTypeFamily.NUMERIC);
    shouldNotCast(checkedType12, SqlTypeFamily.INTEGER);

    // TIMESTAMP
    RelDataType checkedType13 = timestampType;
    checkShouldCast(
        checkedType13,
        combine(ImmutableList.of(dateType, checkedType13),
            charTypes));
    shouldNotCast(checkedType13, SqlTypeFamily.DECIMAL);
    shouldNotCast(checkedType13, SqlTypeFamily.NUMERIC);
    shouldNotCast(checkedType13, SqlTypeFamily.INTEGER);

    // NULL
    RelDataType checkedType14 = nullType;
    checkShouldCast(checkedType14, allTypes);
    shouldCast(checkedType14, SqlTypeFamily.DECIMAL, decimalType);
    shouldCast(checkedType14, SqlTypeFamily.NUMERIC, intType);

    // INTERVAL
    RelDataType checkedType15 = dataTypeFactory.createSqlIntervalType(
        new SqlIntervalQualifier(TimeUnit.YEAR, TimeUnit.MONTH, SqlParserPos.ZERO));
    checkShouldCast(checkedType15, ImmutableList.of(checkedType15));
    shouldNotCast(checkedType15, SqlTypeFamily.DECIMAL);
    shouldNotCast(checkedType15, SqlTypeFamily.NUMERIC);
    shouldNotCast(checkedType15, SqlTypeFamily.INTEGER);
  }

  /** Test case for {@link TypeCoercion#builtinFunctionCoercion}. */
  @Test void testBuiltinFunctionCoercion() {
    // concat
    expr("'ab'||'cde'")
        .columnType("CHAR(5) NOT NULL");
    expr("null||'cde'")
        .columnType("VARCHAR");
    expr("1||'234'")
        .columnType("VARCHAR NOT NULL");
    expr("select ^'a'||t1_binary^ from t1")
        .fails("(?s).*Cannot apply.*");
    // smallint int double
    expr("select t1_smallint||t1_int||t1_double from t1")
        .columnType("VARCHAR");
    // boolean float smallint
    expr("select t1_boolean||t1_float||t1_smallint from t1")
        .columnType("VARCHAR");
    // decimal
    expr("select t1_decimal||t1_varchar20 from t1")
        .columnType("VARCHAR");
    // date timestamp
    expr("select t1_timestamp||t1_date from t1")
        .columnType("VARCHAR");
  }

  /** Test case for {@link TypeCoercion#querySourceCoercion}. */
  @Test void testQuerySourceCoercion() {
    final String expectRowType = "RecordType("
        + "VARCHAR(20) NOT NULL t1_varchar20, "
        + "SMALLINT NOT NULL t1_smallint, "
        + "INTEGER NOT NULL t1_int, "
        + "BIGINT NOT NULL t1_bigint, "
        + "FLOAT NOT NULL t1_float, "
        + "DOUBLE NOT NULL t1_double, "
        + "DECIMAL(19, 0) NOT NULL t1_decimal, "
        + "TIMESTAMP(0) NOT NULL t1_timestamp, "
        + "DATE NOT NULL t1_date, "
        + "BINARY(1) NOT NULL t1_binary, "
        + "BOOLEAN NOT NULL t1_boolean) NOT NULL";

    final String sql = "insert into t1 select t2_smallint, t2_int, t2_bigint, t2_float,\n"
        + "t2_double, t2_decimal, t2_int, t2_date, t2_timestamp, t2_varchar20, t2_int from t2";
    sql(sql).type(expectRowType);

    final String sql1 = "insert into ^t1^(t1_varchar20, t1_date, t1_int)\n"
        + "select t2_smallint, t2_timestamp, t2_float from t2";
    sql(sql1).fails("(?s).*Column 't1_smallint' has no default value and does not allow NULLs.*");

    final String sql2 = "update t1 set t1_varchar20=123, "
        + "t1_date=TIMESTAMP '2020-01-03 10:14:34', t1_int=12.3";
    sql(sql2).type(expectRowType);
  }

  //~ Inner Class ------------------------------------------------------------

  /** A catalog reader with table t1 and t2 whose schema contains all the test data types. */
  public class TCatalogReader extends MockCatalogReader {
    private boolean isCaseSensitive;

    TCatalogReader(RelDataTypeFactory typeFactory, boolean isCaseSensitive) {
      super(typeFactory, false);
      this.isCaseSensitive = isCaseSensitive;
    }

    public MockCatalogReader init() {
      MockSchema tSchema = new MockSchema("SALES");
      registerSchema(tSchema);
      // Register "T1" table.
      final MockTable t1 =
          MockTable.create(this, tSchema, "T1", false, 7.0, null);
      t1.addColumn("t1_varchar20", varchar20Type, true);
      t1.addColumn("t1_smallint", smallintType);
      t1.addColumn("t1_int", intType);
      t1.addColumn("t1_bigint", bigintType);
      t1.addColumn("t1_float", floatType);
      t1.addColumn("t1_double", doubleType);
      t1.addColumn("t1_decimal", decimalType);
      t1.addColumn("t1_timestamp", timestampType);
      t1.addColumn("t1_date", dateType);
      t1.addColumn("t1_binary", binaryType);
      t1.addColumn("t1_boolean", booleanType);
      registerTable(t1);

      final MockTable t2 =
          MockTable.create(this, tSchema, "T2", false, 7.0, null);
      t2.addColumn("t2_varchar20", varchar20Type, true);
      t2.addColumn("t2_smallint", smallintType);
      t2.addColumn("t2_int", intType);
      t2.addColumn("t2_bigint", bigintType);
      t2.addColumn("t2_float", floatType);
      t2.addColumn("t2_double", doubleType);
      t2.addColumn("t2_decimal", decimalType);
      t2.addColumn("t2_timestamp", timestampType);
      t2.addColumn("t2_date", dateType);
      t2.addColumn("t2_binary", binaryType);
      t2.addColumn("t2_boolean", booleanType);
      registerTable(t2);
      return this;
    }

    @Override public boolean isCaseSensitive() {
      return isCaseSensitive;
    }
  }

}
