package lib.report;

import lib.utils.TypeDependency;

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

    public Set<String> getDependentPackages() {
        Set<String> packages = new HashSet<>();

        for (ClassDepsReport report : classReports.values()) {
            for (TypeDependency dep : report.getDependencies()) {
                String targetType = dep.targetType();
                int lastDot = targetType.lastIndexOf('.');
                if (lastDot > 0) {
                    packages.add(targetType.substring(0, lastDot));
                }
            }
        }

        return packages;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Package Name: ").append(packageName).append("\n");
        sb.append("Class Count: ").append(this.getClassCount()).append("\n");
        sb.append("Total Dependencies: ").append(this.getTotalDependencyCount()).append("\n");
        sb.append("Dependent packages").append(getDependentPackages()).append("\n");
        sb.append("Class Reports: \n");
        for (ClassDepsReport classReport : classReports.values()) {
            sb.append(classReport.toString()).append("\n");
        }
        return sb.toString();
    }
}
