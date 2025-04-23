package lib.report;

import java.util.*;

/**
 * Contains the list of types (classes or interfaces) used by a single class or interface.
 * Result of a class-level dependency analysis.
 */
public class ClassDepsReport {

    private final String className;
    private final Set<String> dependencies;

    public ClassDepsReport(String className, Set<String> dependencies) {
        this.className = className;
        this.dependencies = dependencies;
    }

    public String getClassName() {
        return className;
    }

    public void addDependencies(Set<String> dependencies) {
        this.dependencies.addAll(dependencies);
    }

    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    public int getDependencyCount() {
        return dependencies.size();
    }

    public boolean hasDependency(String dependency) {
        return dependencies.contains(dependency);
    }

    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Class Name: ").append(className).append("\n");
        sb.append("Dependency Count: ").append(this.getDependencyCount()).append("\n");
        sb.append("Dependencies: ");
        for (String type : dependencies) {
            sb.append(type).append(", ");
        }
        return sb.toString();
    }
}
