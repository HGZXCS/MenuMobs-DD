package superbas11.menumobs.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import superbas11.menumobs.Reference;

/**
 * Small logging helper. Checks the mod config before printing debug/info output.
 */
public class LogHelper {
    public static final Logger LOGGER = LogManager.getLogger(Reference.MODID);
    public static boolean allowDebugOutput = false;

    private LogHelper() {
    }

    public static void severe(String message) {
        LOGGER.error(message);
    }

    public static void severe(String message, Throwable t) {
        LOGGER.error(message, t);
    }

    public static void warning(String message) {
        LOGGER.warn(message);
    }

    public static void warning(String message, Throwable t) {
        LOGGER.warn(message, t);
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void debug(String message) {
        if (allowDebugOutput)
            LOGGER.info("[debug] " + message);
    }
}
