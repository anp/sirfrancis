package io.sirfrancis.bacon.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by adam on 4/30/15.
 */
public class StringRandomizerTest {

	@Test
	public void testNextString() throws Exception {
		StringRandomizer randomizer = new StringRandomizer(20);
		assertTrue(randomizer.nextString().length() == 20);
	}
}
