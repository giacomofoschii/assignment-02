package lib.report;

import lib.utils.TypeDependency;
import lib.utils.TypeDependency.DependencyType;

import java.util.*;

/**
 * Contains the list of types (classes or interfaces) used by a single class or interface.
 * Result of a class-level dependency analysis.
 */
public class ClassDepsReport {

    private final String className;
    private final Set<TypeDependency> dependencies;

    public ClassDepsReport(String className) {
        this.className = className;
        this.dependencies = new HashSet<>();
    }

    public String getClassName() {
        return className;
    }

    public void addDependency(TypeDependency dependency) {
        dependencies.add(dependency);
    }

    public int getDependencyCount() {
        return dependencies.size();
    }

    public Set<TypeDependency> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("-----------------------CLASS------------------------\n");
        sb.append("Class Name: ").append(className).append("\n");
        sb.append("Dependency Count: ").append(this.getDependencyCount()).append("\n");

        // Group by type
        Map<DependencyType, List<TypeDependency>> grouped = new HashMap<>();
        for (TypeDependency dep : dependencies) {
            grouped.computeIfAbsent(dep.type(), k -> new ArrayList<>()).add(dep);
        }

        // Print each group
        for (Map.Entry<DependencyType, List<TypeDependency>> entry : grouped.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(":\n");
            for (TypeDependency dep : entry.getValue()) {
                sb.append("    ").append(dep.targetType())
                        .append(" (at ").append(dep.location()).append(")\n");
            }
        }
        sb.append("-----------------------END-CLASS------------------------\n");
        return sb.toString();
    }
}
