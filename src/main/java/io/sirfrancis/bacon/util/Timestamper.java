package io.sirfrancis.bacon.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adam on 3/10/15.
 */
public class Timestamper {
	public static String getTimestamp() {
		return new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());
	}
}
