package reactive.model;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a class and its dependencies to other classes
 */
public record ClassDependency(String className, Set<String> dependencies) {

    @Override
    public Set<String> dependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public int getDependencyCount() {
        return dependencies.size();
    }

    @Override
    public String toString() {
        return "ClassDependency{" + "className='" + className + '\'' + ", dependencies=" + dependencies + '}';
    }
}