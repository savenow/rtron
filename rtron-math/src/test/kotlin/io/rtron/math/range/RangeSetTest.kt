/*
 * Copyright 2019-2023 Chair of Geoinformatics, Technical University of Munich
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

package io.rtron.math.range

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class RangeSetTest {

    @Nested
    inner class TestContains {

        @Test
        fun `contains primitive value`() {
            val rangeA = Range.closedOpen(1.0, 1.3)
            val rangeB = Range.closedOpen(10.0, 12.0)
            val rangeC = Range.closed(1.3, 2.0)
            val rangeSet = RangeSet.of(rangeA, rangeB, rangeC)

            assertTrue(rangeSet.contains(1.3))
        }
    }

    @Nested
    inner class TestUnion {

        @Test
        fun `simple union of two disconnected range sets`() {
            val rangeA = Range.closedOpen(1.0, 1.3)
            val rangeB = Range.closed(1.4, 2.0)
            val rangeSetA = RangeSet.of(rangeA)
            val rangeSetB = RangeSet.of(rangeB)

            val actualUnion = rangeSetA.union(rangeSetB)

            assertThat(actualUnion.asRanges())
                .containsExactlyInAnyOrder(rangeA, rangeB)
        }

        @Test
        fun `simple union of two connected range sets`() {
            val rangeA = Range.closedOpen(1.0, 1.3)
            val rangeB = Range.closed(1.3, 2.0)
            val rangeSetA = RangeSet.of(rangeA)
            val rangeSetB = RangeSet.of(rangeB)
            val expectedRange = Range.closed(1.0, 2.0)

            val actualUnion = rangeSetA.union(rangeSetB)

            assertThat(actualUnion.asRanges()).containsOnly(expectedRange)
        }
    }

    @Nested
    inner class TestIntersection {

        @Test
        fun `two disconnected range sets do not intersect`() {
            val rangeA = Range.closedOpen(1.0, 1.3)
            val rangeB = Range.closed(1.4, 2.0)
            val rangeSetA = RangeSet.of(rangeA)
            val rangeSetB = RangeSet.of(rangeB)

            assertFalse(rangeSetA.intersects(rangeSetB))
        }

        @Test
        fun `two connected range sets do not intersect`() {
            val rangeA = Range.closedOpen(1.0, 1.3)
            val rangeB = Range.closed(1.3, 2.0)
            val rangeSetA = RangeSet.of(rangeA)
            val rangeSetB = RangeSet.of(rangeB)

            assertFalse(rangeSetA.intersects(rangeSetB))
        }

        @Test
        fun `two connected and closed range sets do intersect`() {
            val rangeA = Range.closed(1.0, 1.3)
            val rangeB = Range.closed(1.3, 2.0)
            val rangeSetA = RangeSet.of(rangeA)
            val rangeSetB = RangeSet.of(rangeB)

            assertTrue(rangeSetA.intersects(rangeSetB))
        }
    }
}
