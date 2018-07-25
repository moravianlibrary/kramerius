package org.kramerius;

import java.sql.Timestamp;
import java.util.Date;

/**
 * @author Jakub Kremlacek
 */
public class TimeUtils {

    static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(TimeUtils.class.getName());

    public static void printTimeToLog(String text) {
        LOGGER.info("$$$ "+ new Timestamp(new Date().getTime()).toString() + " " + text);
    }
}
