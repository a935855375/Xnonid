package server.core.exception;

/**
 * A UsefulException is something useful to display in the User browser.
 */
public abstract class UsefulException extends RuntimeException {

    /**
     * Exception title.
     */
    public String title;

    /**
     * Exception description.
     */
    public String description;

    /**
     * Exception cause if defined.
     */
    public Throwable cause;

    /**
     * Unique id for this exception.
     */
    public String id;

    public UsefulException(String message, Throwable cause) {
        super(message, cause);
    }

    public UsefulException(String message) {
        super(message);
    }

    public String toString() {
        return "@" + id + ": " + getMessage();
    }
}

