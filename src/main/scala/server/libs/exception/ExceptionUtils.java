package server.libs.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Copied from apache.commons.lang3 3.7
 */
public class ExceptionUtils {

    /**
     * Copied from apache.commons.lang3 3.7 ArrayUtils class
     *
     * <p>An empty immutable {@code String} array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Gets the stack trace from a Throwable as a String.
     *
     * <p>The result of this method vary by JDK version as this method uses {@link
     * Throwable#printStackTrace(java.io.PrintWriter)}. On JDK1.3 and earlier, the cause exception
     * will not be shown unless the specified throwable alters printStackTrace.
     *
     * @param throwable the <code>Throwable</code> to be examined
     * @return the stack trace as generated by the exception's <code>printStackTrace(PrintWriter)
     * </code> method
     */
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    /**
     * Captures the stack trace associated with the specified <code>Throwable</code> object,
     * decomposing it into a list of stack frames.
     *
     * <p>The result of this method vary by JDK version as this method uses {@link
     * Throwable#printStackTrace(java.io.PrintWriter)}. On JDK1.3 and earlier, the cause exception
     * will not be shown unless the specified throwable alters printStackTrace.
     *
     * @param throwable the <code>Throwable</code> to examine, may be null
     * @return an array of strings describing each stack frame, never null
     */
    public static String[] getStackFrames(final Throwable throwable) {
        if (throwable == null) {
            return EMPTY_STRING_ARRAY;
        }
        return getStackFrames(getStackTrace(throwable));
    }

    /**
     * Returns an array where each element is a line from the argument.
     *
     * <p>The end of line is determined by the value of {@link System#lineSeparator()}.
     *
     * @param stackTrace a stack trace String
     * @return an array where each element is a line from the argument
     */
    static String[] getStackFrames(final String stackTrace) {
        final String linebreak = System.lineSeparator();
        final StringTokenizer frames = new StringTokenizer(stackTrace, linebreak);
        final List<String> list = new ArrayList<>();
        while (frames.hasMoreTokens()) {
            list.add(frames.nextToken());
        }
        return list.toArray(new String[list.size()]);
    }
}

