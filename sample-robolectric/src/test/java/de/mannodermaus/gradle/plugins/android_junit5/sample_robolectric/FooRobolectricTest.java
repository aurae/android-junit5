package de.mannodermaus.gradle.plugins.android_junit5.sample_robolectric;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class FooRobolectricTest {

    @Test
    public void noCodeCoverage() {
        final int result = new Foo().junit4robolectric();

        assertEquals(-1, result);
    }

}
