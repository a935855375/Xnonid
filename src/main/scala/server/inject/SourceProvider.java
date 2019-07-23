
package server.inject;

import java.util.*;

/**
 * Provides access to the calling line of code.
 * https://github.com/google/guice/blob/3.0/core/src/com/google/inject/internal/util/SourceProvider.java
 *
 * @author crazybob@google.com (Bob Lee)
 */
public final class SourceProvider {

    /**
     * Indicates that the source is unknown.
     */
    public static final Object UNKNOWN_SOURCE = "[unknown source]";

    private final Set<String> classNamesToSkip;

    public static final SourceProvider DEFAULT_INSTANCE =
            new SourceProvider(Collections.singleton(SourceProvider.class.getName()));

    private SourceProvider(Collection<String> classesToSkip) {
        this.classNamesToSkip = Collections.unmodifiableSet(new HashSet<String>(classesToSkip));
    }

    /**
     * Returns a new instance that also skips {@code moreClassesToSkip}.
     *
     * @param moreClassesToSkip a list of classes to skip in from source provider.
     * @return the source provider skipping {@code moreClassesToSkip}.
     */
    public SourceProvider plusSkippedClasses(Class... moreClassesToSkip) {
        Set<String> toSkip = new HashSet<String>(classNamesToSkip);
        toSkip.addAll(asStrings(moreClassesToSkip));
        return new SourceProvider(toSkip);
    }

    /**
     * Returns the class names as Strings
     */
    private static List<String> asStrings(Class... classes) {
        List<String> strings = new ArrayList<String>();
        for (Class c : classes) {
            strings.add(c.getName());
        }
        return strings;
    }

    /**
     * Returns the calling line of code. The selected line is the nearest to the top of the stack that
     * is not skipped.
     *
     * @return a stack trace element containing the caller.
     */
    public StackTraceElement get() {
        for (final StackTraceElement element : new Throwable().getStackTrace()) {
            String className = element.getClassName();
            if (!classNamesToSkip.contains(className)) {
                return element;
            }
        }
        throw new AssertionError();
    }
}
