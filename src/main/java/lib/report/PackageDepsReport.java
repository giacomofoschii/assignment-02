package lib.report;

import java.util.*;

/**
 * Contains the list of types used by all classes and interfaces in a package.
 * Result of a package-level dependency analysis.
 */
public class PackageDepsReport {
    private final String packageName;
    private final Map<String, ClassDepsReport> classReports;

    public PackageDepsReport(String packageName) {
        this.packageName = packageName;
        this.classReports = new HashMap<>();
    }

    public String getPackageName() {
        return packageName;
    }

    public Map<String, ClassDepsReport> getClassReports() {
        return Collections.unmodifiableMap(classReports);
    }

    public void addClassReport(ClassDepsReport classReport) {
        classReports.put(classReport.getClassName(), classReport);
    }

    public int getClassCount() {
        return classReports.size();
    }

    public int getTotalDependencyCount() {
        return classReports.values().stream()
                .mapToInt(ClassDepsReport::getDependencyCount)
                .sum();
    }

    public Set<String> getAllDependencies() {
        Set<String> allDependencies = new HashSet<>();
        for (ClassDepsReport classReport : classReports.values()) {
            allDependencies.addAll(classReport.getDependencies());
        }
        return allDependencies;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Package Name: ").append(packageName).append("\n");
        sb.append("Class Count: ").append(this.getClassCount()).append("\n");
        sb.append("Total Dependencies: ").append(this.getTotalDependencyCount()).append("\n");
        sb.append("Class Reports: \n");
        for (ClassDepsReport classReport : classReports.values()) {
            sb.append(classReport.toString()).append("\n");
        }
        return sb.toString();
    }
}
