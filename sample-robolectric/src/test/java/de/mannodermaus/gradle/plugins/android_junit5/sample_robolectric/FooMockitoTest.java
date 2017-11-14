package de.mannodermaus.gradle.plugins.android_junit5.sample_robolectric;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class FooMockitoTest {

    @Test
    public void mockitoCodeCoverage() {
        final int result = new Foo().junit4mockito();

        assertEquals(-1, result);
    }

}
