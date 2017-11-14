package de.mannodermaus.gradle.plugins.android_junit5.sample_robolectric;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FooJunit5Tests {

    @Test
    void junit5codeCoverage() {
        final int result = new Foo().junit5();

        assertEquals(-1, result);
    }

}
