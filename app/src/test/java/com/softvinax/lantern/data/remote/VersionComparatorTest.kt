package com.softvinax.lantern.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `a higher numeric version is newer`() {
        assertTrue(VersionComparator.isNewer("1.1.0", "1.0.0-demo"))
        assertTrue(VersionComparator.isNewer("1.0.1", "1.0.0"))
        assertTrue(VersionComparator.isNewer("2.0", "1.9.9"))
    }

    @Test
    fun `the same numbers with a suffix are not newer`() {
        assertFalse(VersionComparator.isNewer("1.0.0", "1.0.0-demo"))
        assertFalse(VersionComparator.isNewer("1.0.0-demo", "1.0.0"))
    }

    @Test
    fun `a lower version is not newer`() {
        assertFalse(VersionComparator.isNewer("1.0.0", "1.1.0"))
        assertFalse(VersionComparator.isNewer("0.9", "1.0"))
    }

    @Test
    fun `malformed segments do not crash`() {
        assertFalse(VersionComparator.isNewer("", ""))
        assertFalse(VersionComparator.isNewer("draft", "1.0"))
        assertTrue(VersionComparator.isNewer("1.x.2", "1.0.1"))
    }
}
