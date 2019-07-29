/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package server.libs;

class CrossScala {
    /**
     * Converts a Java List to Scala Seq.
     *
     * @param list the java list.
     * @param <T>  the element type.
     * @return the converted Seq.
     */
    public static <T> scala.collection.immutable.Seq<T> toSeq(java.util.List<T> list) {
        return scala.collection.JavaConverters.asScalaBufferConverter(list).asScala().toList();
    }

    /**
     * Converts a Java Array to Scala Seq.
     *
     * @param array the java array.
     * @param <T>   the element type.
     * @return the converted Seq.
     */
    public static <T> scala.collection.immutable.Seq<T> toSeq(T[] array) {
        return toSeq(java.util.Arrays.asList(array));
    }

    /**
     * Converts a Java varargs to Scala varargs.
     *
     * @param array the java array.
     * @param <T>   the element type.
     * @return the Scala varargs
     */
    @SafeVarargs
    public static <T> scala.collection.immutable.Seq<T> varargs(T... array) {
        return toSeq(array);
    }
}
