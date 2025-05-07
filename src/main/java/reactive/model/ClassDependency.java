package reactive.model;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a class and its dependencies to other classes
 */
public class ClassDependency {
    private final String className;
    private final Set<String> dependencies;

    public ClassDependency(String className, Set<String> dependencies) {
        this.className = className;
        this.dependencies = dependencies;
    }

    public String getClassName() {
        return this.className;
    }

    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(this.dependencies);
    }

    public int getDependencyCount() {
        return dependencies.size();
    }

    @Override
    public String toString() {
        return "ClassDependency{" + "className='" + className + '\'' + ", dependencies=" + dependencies + '}';
    }
}