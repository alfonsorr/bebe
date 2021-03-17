package org.apache.spark.sql

import org.scalatest.FunSpec
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.BebeFunctions._
import com.github.mrpowers.spark.fast.tests.{ColumnComparer, DataFrameComparer}
import mrpowers.bebe.SparkSessionTestWrapper
import java.sql.{Date, Timestamp}
import com.github.mrpowers.spark.daria.sql.SparkSessionExt._
import org.apache.spark.sql.types._

class BebeFunctionsSpec
    extends FunSpec
    with SparkSessionTestWrapper
    with ColumnComparer
    with DataFrameComparer {

  import spark.implicits._

  describe("bebe_cardinality") {
    it("returns the size of an array") {
      val df = Seq(
        (Array("23", "44"), 2),
        (Array.empty[String], 0),
        (null, -1)
      ).toDF("some_strings", "expected")
        .withColumn("actual", bebe_cardinality(col("some_strings")))
      assertColumnEquality(df, "actual", "expected")
    }

    it("returns the size of a map") {
      val df = Seq(
        (Map("23" -> 23, "44" -> 44), 2),
        (Map.empty[String, Int], 0),
        (null, -1)
      ).toDF("some_kv_pairs", "expected")
        .withColumn("actual", bebe_cardinality(col("some_kv_pairs")))
      assertColumnEquality(df, "actual", "expected")
    }
  }

  describe("bebe_cot") {
    it("returns the cotangent") {
      val df = spark
        .createDF(
          List(
            (60, 3.12),
            (100, -1.7),
            (null, null)
          ),
          List(
            ("some_degree", IntegerType, true),
            ("expected", DoubleType, true)
          )
        )
        .withColumn("actual", bebe_cot(col("some_degree")))
      assertDoubleTypeColumnEquality(df, "actual", "expected", 0.01)
    }
  }

  describe("bebe_count_if") {
    it("returns the count if the predicate is true") {
      val actualDF = spark
        .createDF(
          List(
            (4),
            (3),
            (10)
          ),
          List(
            ("some_int", IntegerType, true)
          )
        )
        .agg(bebe_count_if(col("some_int") < 5).as("lt_five_count"))
      val expectedDF = spark
        .createDF(
          List(
            (2L)
          ),
          List(
            ("lt_five_count", LongType, true)
          )
        )
      assertSmallDataFrameEquality(actualDF, expectedDF, ignoreNullable = true)
    }
  }

  describe("bebe_stack") {
    it("stacks stuff") {
      val df = spark
        .createDF(
          List(
            (1, 2, 3, 4),
            (6, 7, 8, 9)
          ),
          List(
            ("col1", IntegerType, true),
            ("col2", IntegerType, true),
            ("col3", IntegerType, true),
            ("col4", IntegerType, true)
          )
        )
        .select(bebe_stack(lit(2), col("col1"), col("col2"), col("col3"), col("col4")))
      val expectedDF = spark.createDF(
        List(
          (1, 2),
          (3, 4),
          (6, 7),
          (8, 9)
        ),
        List(
          ("col0", IntegerType, true),
          ("col1", IntegerType, true)
        )
      )
      assertSmallDataFrameEquality(df, expectedDF)
    }
  }

  describe("bebe_character_length") {
    it("returns the number of characters in a string") {
      val df = spark
        .createDF(
          List(
            ("Spark SQL ", 10),
            ("", 0),
            (null, null)
          ),
          List(
            ("some_string", StringType, true),
            ("expected", IntegerType, true)
          )
        )
        .withColumn("actual", bebe_character_length(col("some_string")))
      assertColumnEquality(df, "actual", "expected")
    }

    it("errors out when run on a column type that doesn't make sense") {
      val df = spark
        .createDF(
          List(
            (33),
            (44),
            (null)
          ),
          List(
            ("some_int", IntegerType, true)
          )
        )
        .withColumn("actual", bebe_character_length(col("some_int")))
      intercept[org.apache.spark.sql.AnalysisException] {
        assertColumnEquality(df, "actual", "expected")
      }
    }
  }

  describe("bebe_chr") {
    it("returns the ASCII character of a character") {
      val df = spark
        .createDF(
          List(
            (118, "v"),
            (65, "A"),
            (null, null)
          ),
          List(
            ("some_int", IntegerType, true),
            ("expected", StringType, true)
          )
        )
        .withColumn("actual", bebe_chr(col("some_int")))
    }
  }

  describe("bebe_e") {
    it("returns Euler's number") {
      val df = spark
        .createDF(
          List(
            (118, 2.718),
            (null, 2.718)
          ),
          List(
            ("some_int", IntegerType, true),
            ("expected", DoubleType, true)
          )
        )
        .withColumn("actual", bebe_e())
      assertDoubleTypeColumnEquality(df, "actual", "expected", 0.001)
    }
  }

  describe("bebe_if_null") {
    it("returns the col2 if col1 isn't null") {
      val df = spark
        .createDF(
          List(
            (null, "expr2", "expr2"),
            ("expr1", null, "expr1"),
            ("expr1", "expr2", "expr1")
          ),
          List(
            ("col1", StringType, true),
            ("col2", StringType, true),
            ("expected", StringType, true)
          )
        )
        .withColumn("actual", bebe_if_null(col("col1"), col("col2")))
      assertColumnEquality(df, "actual", "expected")
    }
  }

  describe("bebe_inline") {
    it("explodes an array of StructTypes to a table") {
      val data = Seq(
        Row(20.0, "dog"),
        Row(3.5, "cat"),
        Row(0.000006, "ant")
      )

      val schema = StructType(
        List(
          StructField("weight", DoubleType, true),
          StructField("animal_type", StringType, true)
        )
      )

      val df = spark.createDataFrame(
        spark.sparkContext.parallelize(data),
        schema
      )

      val actualDF = df
        .withColumn(
          "animal_interpretation",
          struct(
            (col("weight") > 5).as("is_large_animal"),
            col("animal_type").isin("rat", "cat", "dog").as("is_mammal")
          )
        )
        .groupBy("animal_interpretation")
        .agg(collect_list("animal_interpretation").as("interpretations"))

      val res = actualDF.select(bebe_inline(col("interpretations")))

      val expected = spark.createDF(
        List(
          (true, true),
          (false, false),
          (false, true)
        ),
        List(
          ("is_large_animal", BooleanType, true),
          ("is_mammal", BooleanType, true)
        )
      )

      assertSmallDataFrameEquality(res, expected)
    }
  }

  describe("bebe_is_not_null") {
    it("returns true if the element isn't null") {
      val df = Seq(
        (null, false),
        ("hi", true)
      ).toDF("some_string", "expected")
        .withColumn("actual", bebe_is_not_null(col("some_string")))
      assertColumnEquality(df, "actual", "expected")
    }
  }

  describe("bebe_left") {
    it("gets the leftmost N elements from a string") {
      val df = Seq(
        ("this 23 has 44 numbers", "th"),
        ("no numbers", "no"),
        (null, null)
      ).toDF("some_string", "expected")
        .withColumn("actual", bebe_left(col("some_string"), lit(2)))
      assertColumnEquality(df, "actual", "expected")
    }
  }

  describe("bebe_like") {
    it("returns true if the pattern matches the SQL LIKE language") {
      val df = spark
        .createDF(
          List(
            ("hi!", "hi_", true),
            ("hello there person", "hello%", true),
            ("whatever", "hello%", false),
            (null, null, null)
          ),
          List(
            ("some_string", StringType, true),
            ("like_regexp", StringType, true),
            ("expected", BooleanType, true)
          )
        )
        .withColumn("actual", bebe_like(col("some_string"), col("like_regexp")))
      assertColumnEquality(df, "actual", "expected")
    }
  }

  describe("bebe_make_date") {
    it("creates a date") {
      val df = spark
        .createDF(
          List(
            (2020, 1, 1, Date.valueOf("2020-01-01")),
            (2021, 3, 5, Date.valueOf("2021-03-05")),
            (null, null, null, null)
          ),
          List(
            ("year", IntegerType, true),
            ("month", IntegerType, true),
            ("day", IntegerType, true),
            ("expected", DateType, true)
          )
        )
        .withColumn("actual", bebe_make_date(col("year"), col("month"), col("day")))
      assertColumnEquality(df, "actual", "expected")
    }
  }

  describe("bebe_make_timestamp") {
    it("creates a date") {
      val df = spark
        .createDF(
          List(
            (2020, 1, 1, 5, 3, 45, Timestamp.valueOf("2020-01-01 05:03:45")),
            (2021, 3, 5, 11, 1, 13, Timestamp.valueOf("2021-03-05 11:01:13")),
            (null, null, null, null, null, null, null)
          ),
          List(
            ("year", IntegerType, true),
            ("month", IntegerType, true),
            ("day", IntegerType, true),
            ("hours", IntegerType, true),
            ("minutes", IntegerType, true),
            ("seconds", IntegerType, true),
            ("expected", TimestampType, true)
          )
        )
        .withColumn(
          "actual",
          bebe_make_timestamp(
            col("year"),
            col("month"),
            col("day"),
            col("hours"),
            col("minutes"),
            col("seconds")
          )
        )
      assertColumnEquality(df, "actual", "expected")
    }
  }

  describe("bebe_nvl2") {
    it("Returns expr2 if expr1 is not null, or expr3 otherwise") {
      val df = spark
        .createDF(
          List(
            (null, "expr2", "expr3", "expr3"),
            ("expr1", null, "expr3", null),
            ("expr1", "expr2", "expr3", "expr2"),
            ("expr1", null, null, null)
          ),
          List(
            ("col1", StringType, true),
            ("col2", StringType, true),
            ("col3", StringType, true),
            ("expected", StringType, true)
          )
        )
        .withColumn("actual", bebe_nvl2(col("col1"), col("col2"), col("col3")))
      assertColumnEquality(df, "actual", "expected")
    }
  }

  describe("bebe_octet_length") {
    it("calculates the octet length of a string") {
      val df = spark
        .createDF(
          List(
            ("€", 3),
            ("Spark SQL", 9),
            (null, null)
          ),
          List(
            ("some_string", StringType, true),
            ("expected", IntegerType, true)
          )
        )
        .withColumn("actual", bebe_octet_length(col("some_string")))
      assertColumnEquality(df, "actual", "expected")
    }
  }

  // ADDITIONAL HELPER FUNCTIONS

  describe("beginning_of_month") {
//    it("has a good blog post example") {
//      val df = Seq(
//        (Date.valueOf("2020-01-15")),
//        (Date.valueOf("2020-01-20")),
//        (null)
//      ).toDF("some_date")
//        .withColumn("beginning_of_month", bebe_beginning_of_month(col("some_date")))
//
//      df.show()
//      df.explain(true)
//
//      val df = Seq(
//        (Date.valueOf("2020-01-15")),
//        (Date.valueOf("2020-01-20")),
//        (null)
//      ).toDF("some_date")
//        .withColumn("end_of_month", last_day(col("some_date")))
//
//      df.show()
//      df.explain(true)
//    }

    it("gets the beginning of the month of a date column") {
      val df = Seq(
        (Date.valueOf("2020-01-15"), Date.valueOf("2020-01-01")),
        (Date.valueOf("2020-01-20"), Date.valueOf("2020-01-01")),
        (null, null)
      ).toDF("some_date", "expected")
        .withColumn("actual", bebe_beginning_of_month(col("some_date")))
      assertColumnEquality(df, "actual", "expected")
    }

    it("gets the beginning of the month of a timestamp column") {
      val df = Seq(
        (Timestamp.valueOf("2020-01-15 08:01:32"), Date.valueOf("2020-01-01")),
        (Timestamp.valueOf("2020-01-20 23:03:22"), Date.valueOf("2020-01-01")),
        (null, null)
      ).toDF("some_time", "expected")
        .withColumn("actual", bebe_beginning_of_month(col("some_time")))
      assertColumnEquality(df, "actual", "expected")
    }
  }

}
