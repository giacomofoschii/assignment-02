package asynchronous.report;

import asynchronous.util.TypeDependency;
import asynchronous.util.TypeDependency.DependencyType;

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
        return this.className;
    }

    public Set<TypeDependency> getDependencies() {
        return Collections.unmodifiableSet(this.dependencies);
    }

    public void addDependency(TypeDependency dependency) {
        dependencies.add(dependency);
    }

    public int getDependencyCount() {
        return dependencies.size();
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
            grouped.computeIfAbsent(dep.getType(), k -> new ArrayList<>()).add(dep);
        }

        // Print each group
        for (Map.Entry<DependencyType, List<TypeDependency>> entry : grouped.entrySet()) {
            sb.append("\t").append(entry.getKey()).append(":\n");
            for (TypeDependency dep : entry.getValue()) {
                sb.append("\t").append(dep.getTargetType())
                        .append(" (code: ").append(dep.getSourceCode())
                        .append(" at line: ").append(dep.getLineNumber())
                        .append(")\n");
            }
        }
        sb.append("---------------------END-CLASS----------------------\n");
        return sb.toString();
    }
}
