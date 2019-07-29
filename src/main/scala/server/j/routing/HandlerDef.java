package server.j.routing;

import scala.collection.Seq;
import server.libs.Scala;

import java.util.List;

public abstract class HandlerDef {
    public abstract ClassLoader classLoader();

    public abstract String routerPackage();

    public abstract String controller();

    public abstract String method();

    protected abstract Seq<Class<?>> parameterTypes();

    public abstract String verb();

    public abstract String path();

    public abstract String comments();

    protected abstract Seq<String> modifiers();

    public List<Class<?>> getParameterTypes() {
        return Scala.asJava(parameterTypes());
    }

    public List<String> getModifiers() {
        return Scala.asJava(modifiers());
    }
}
