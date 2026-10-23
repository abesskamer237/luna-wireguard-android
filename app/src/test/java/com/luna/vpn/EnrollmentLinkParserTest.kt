package com.luna.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnrollmentLinkParserTest {
    private val token = "A".repeat(43)

    @Test fun parsesHttpsApiAndOneTimeToken() {
        val uri = "luna://enroll?api=https%3A%2F%2Fapi.example.com&token=$token"
        assertEquals(EnrollmentLink("https://api.example.com", token), EnrollmentLinkParser.parse(uri))
    }

    @Test fun allowsApiPathAndNormalizesTrailingSlash() {
        val uri = "luna://enroll?api=https%3A%2F%2Fapi.example.com%2Fvpn%2F&token=$token"
        assertEquals(EnrollmentLink("https://api.example.com/vpn", token), EnrollmentLinkParser.parse(uri))
    }

    @Test fun rejectsHttpOrMalformedTokens() {
        assertNull(EnrollmentLinkParser.parse("luna://enroll?api=http%3A%2F%2Fapi.example.com&token=$token"))
        assertNull(EnrollmentLinkParser.parse("luna://enroll?api=https%3A%2F%2Fapi.example.com&token=short"))
    }

    @Test fun rejectsCredentialsFragmentsAndDuplicateFields() {
        assertNull(EnrollmentLinkParser.parse("luna://enroll?api=https%3A%2F%2Fuser%3Apass%40api.example.com&token=$token"))
        assertNull(EnrollmentLinkParser.parse("luna://enroll?api=https%3A%2F%2Fapi.example.com&token=$token#fragment"))
        assertNull(EnrollmentLinkParser.parse("luna://enroll?api=https%3A%2F%2Fapi.example.com&api=https%3A%2F%2Fevil.example&token=$token"))
    }
}
