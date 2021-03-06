/*
 * Copyright 2019 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.beam.sdk.extensions.smb;

import static org.apache.beam.sdk.transforms.display.DisplayDataMatchers.hasDisplayItem;

import com.google.api.services.bigquery.model.TableRow;
import java.util.Arrays;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.extensions.smb.BucketMetadata.HashType;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

/** Unit tests for {@link JsonBucketMetadata}. */
public class JsonBucketMetadataTest {

  @Test
  public void testExtractKey() throws Exception {
    final TableRow user =
        new TableRow()
            .set("user", "Alice")
            .set("age", 10)
            .set(
                "location",
                new TableRow()
                    .set("currentCountry", "US")
                    .set("prevCountries", Arrays.asList("CN", "MX")));

    Assert.assertEquals(
        (Integer) 10,
        new JsonBucketMetadata<>(
                1,
                1,
                Integer.class,
                HashType.MURMUR3_32,
                "age",
                SortedBucketIO.DEFAULT_FILENAME_PREFIX)
            .extractKey(user));

    Assert.assertEquals(
        "US",
        new JsonBucketMetadata<>(
                1,
                1,
                String.class,
                HashType.MURMUR3_32,
                "location.currentCountry",
                SortedBucketIO.DEFAULT_FILENAME_PREFIX)
            .extractKey(user));

    /*
    FIXME: BucketMetadata should allow custom coder?
    Assert.assertEquals(
        Arrays.asList("CN", "MX"),
        new JsonBucketMetadata<>(
                1, 1, ArrayList.class, HashType.MURMUR3_32, "location.prevCountries")
            .extractKey(user));
     */
  }

  @Test
  public void testCoding() throws Exception {
    final JsonBucketMetadata<String> metadata =
        new JsonBucketMetadata<>(
            1,
            1,
            1,
            String.class,
            HashType.MURMUR3_32,
            "favorite_color",
            SortedBucketIO.DEFAULT_FILENAME_PREFIX);

    final BucketMetadata<String, TableRow> copy = BucketMetadata.from(metadata.toString());
    Assert.assertEquals(metadata.getVersion(), copy.getVersion());
    Assert.assertEquals(metadata.getNumBuckets(), copy.getNumBuckets());
    Assert.assertEquals(metadata.getNumShards(), copy.getNumShards());
    Assert.assertEquals(metadata.getKeyClass(), copy.getKeyClass());
    Assert.assertEquals(metadata.getHashType(), copy.getHashType());
  }

  @Test
  public void testVersionDefault() throws Exception {
    final JsonBucketMetadata<String> metadata =
        new JsonBucketMetadata<>(
            1,
            1,
            String.class,
            HashType.MURMUR3_32,
            "favorite_color",
            SortedBucketIO.DEFAULT_FILENAME_PREFIX);

    Assert.assertEquals(BucketMetadata.CURRENT_VERSION, metadata.getVersion());
  }

  @Test
  public void testDisplayData() throws Exception {
    final JsonBucketMetadata<String> metadata =
        new JsonBucketMetadata<>(
            2,
            1,
            String.class,
            HashType.MURMUR3_32,
            "favorite_color",
            SortedBucketIO.DEFAULT_FILENAME_PREFIX);

    final DisplayData displayData = DisplayData.from(metadata);
    MatcherAssert.assertThat(displayData, hasDisplayItem("numBuckets", 2));
    MatcherAssert.assertThat(displayData, hasDisplayItem("numShards", 1));
    MatcherAssert.assertThat(
        displayData, hasDisplayItem("version", BucketMetadata.CURRENT_VERSION));
    MatcherAssert.assertThat(displayData, hasDisplayItem("keyField", "favorite_color"));
    MatcherAssert.assertThat(displayData, hasDisplayItem("keyClass", String.class));
    MatcherAssert.assertThat(
        displayData, hasDisplayItem("hashType", HashType.MURMUR3_32.toString()));
    MatcherAssert.assertThat(displayData, hasDisplayItem("keyCoder", StringUtf8Coder.class));
  }

  @Test
  public void testSameSourceCompatibility() throws Exception {
    final JsonBucketMetadata<String> metadata1 =
        new JsonBucketMetadata<>(
            2,
            1,
            String.class,
            HashType.MURMUR3_32,
            "favorite_country",
            SortedBucketIO.DEFAULT_FILENAME_PREFIX);

    final JsonBucketMetadata<String> metadata2 =
        new JsonBucketMetadata<>(
            2,
            1,
            String.class,
            HashType.MURMUR3_32,
            "favorite_color",
            SortedBucketIO.DEFAULT_FILENAME_PREFIX);

    final JsonBucketMetadata<String> metadata3 =
        new JsonBucketMetadata<>(
            4,
            1,
            String.class,
            HashType.MURMUR3_32,
            "favorite_color",
            SortedBucketIO.DEFAULT_FILENAME_PREFIX);

    final JsonBucketMetadata<Long> metadata4 =
        new JsonBucketMetadata<>(
            4,
            1,
            Long.class,
            HashType.MURMUR3_32,
            "favorite_color",
            SortedBucketIO.DEFAULT_FILENAME_PREFIX);

    Assert.assertFalse(metadata1.isPartitionCompatible(metadata2));
    Assert.assertTrue(metadata2.isPartitionCompatible(metadata3));
    Assert.assertFalse(metadata3.isPartitionCompatible(metadata4));
  }
}
