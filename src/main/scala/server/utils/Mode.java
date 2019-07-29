package server.utils;

/** Application mode, either `DEV`, `TEST`, or `PROD`. */
public enum Mode {
    DEV,
    TEST,
    PROD;

    public server.Mode asScala() {
        if (this == DEV) {
            return server.Mode.Dev$.MODULE$;
        } else if (this == PROD) {
            return server.Mode.Prod$.MODULE$;
        }
        return server.Mode.Test$.MODULE$;
    }
}
