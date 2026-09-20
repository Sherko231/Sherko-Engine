package com.samo.testing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SharedTestSupportSmokeTest {
    @Test
    void junitAndAssertJAreAvailable() {

        assertThat("test-support").startsWith("test");

    }
}
