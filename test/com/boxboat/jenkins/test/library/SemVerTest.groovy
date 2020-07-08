package com.boxboat.jenkins.test.semver

import com.boxboat.jenkins.library.SemVer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

import static org.junit.Assert.assertEquals

class SemVerTest {
    static final int EQUAL = 0
    static final int LESS_THAN = -1
    static final int GREATER_THAN = 1

    @Test
    void testSemverNonPreReleaseCompareRCs() {
        SemVer sv1
        SemVer sv2
        sv1 = new SemVer("0.1.0")
        sv2 = new SemVer("0.1.0-rc1")
        assertEquals(sv1.nonPreReleaseCompare(sv2), EQUAL)

        sv1 = new SemVer("0.1.0-rc2")
        sv2 = new SemVer("0.1.0-rc1")
        assertEquals(sv1.nonPreReleaseCompare(sv2), EQUAL)

        sv1 = new SemVer("0.2.0")
        sv2 = new SemVer("0.1.0-rc1")
        assertEquals(sv1.nonPreReleaseCompare(sv2), GREATER_THAN)
        assertEquals(sv2.nonPreReleaseCompare(sv1), LESS_THAN)

        sv1 = new SemVer("0.2.0-rc0")
        sv2 = new SemVer("0.1.0-rc1")
        assertEquals(sv1.nonPreReleaseCompare(sv2), GREATER_THAN)
        assertEquals(sv2.nonPreReleaseCompare(sv1), LESS_THAN)
    }

    @Test
    void testSemverCompareRC() {
        SemVer sv1
        SemVer sv2

        sv1 = new SemVer("0.1.0")
        sv2 = new SemVer("0.1.0-rc1")
        assertEquals(sv1.compareTo(sv2), GREATER_THAN)
        assertEquals(sv2.compareTo(sv1), LESS_THAN)

        sv1 = new SemVer("0.1.0-rc2")
        sv2 = new SemVer("0.1.0-rc1")
        assertEquals(sv1.compareTo(sv2), EQUAL)

        sv1 = new SemVer("0.2.0")
        sv2 = new SemVer("0.1.0-rc1")
        assertEquals(sv1.nonPreReleaseCompare(sv2), GREATER_THAN)
        assertEquals(sv2.nonPreReleaseCompare(sv1), LESS_THAN)

        sv1 = new SemVer("0.2.0-rc0")
        sv2 = new SemVer("0.1.0-rc1")
        assertEquals(sv1.nonPreReleaseCompare(sv2), GREATER_THAN)
        assertEquals(sv2.nonPreReleaseCompare(sv1), LESS_THAN)
    }

}
