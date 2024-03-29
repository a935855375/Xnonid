/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package server.inject;

import javax.inject.Named;
import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * An implementation of the [[javax.inject.Named]] annotation.
 *
 * <p>This allows bindings qualified by name.
 */
// See https://issues.scala-lang.org/browse/SI-8778 for why this is implemented in Java
public class NamedImpl implements Named, Serializable {

    private static final long serialVersionUID = 0;
    private final String value;

    public NamedImpl(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public int hashCode() {
        // This is specified in java.lang.Annotation.
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Named)) {
            return false;
        }

        Named other = (Named) o;
        return value.equals(other.value());
    }

    public String toString() {
        return "@" + Named.class.getName() + "(value=" + value + ")";
    }

    public Class<? extends Annotation> annotationType() {
        return Named.class;
    }
}
