package de.mannodermaus.gradle.plugins.android_junit5.sample_robolectric;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

public class FooJunit4Test {

    @Test
    public void junit4codeCoverage() {
        final int result = new Foo().junit4();

        assertEquals(-1, result);
    }

}
