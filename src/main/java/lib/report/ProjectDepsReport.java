package lib.report;

import java.util.*;

/**
 * Contains the list of types used across all packages in a Java project.
 * Result of a project-level dependency analysis.
 */
public class ProjectDepsReport {
    private final String projectName;
    private final Map<String, PackageDepsReport> packageReports;

    public ProjectDepsReport(String projectName) {
        this.projectName = projectName;
        this.packageReports = new HashMap<>();
    }

    public String getProjectName() {
        return projectName;
    }

    public Map<String, PackageDepsReport> getPackageReports() {
        return Collections.unmodifiableMap(packageReports);
    }

    public void addPackageReport(PackageDepsReport packageReport) {
        packageReports.put(packageReport.getPackageName(), packageReport);
    }

    public int getPackageCount() {
        return packageReports.size();
    }

    public int getClassCount() {
        return packageReports.values().stream()
                .mapToInt(PackageDepsReport::getClassCount)
                .sum();
    }

    public int getTotalDependencyCount() {
        return packageReports.values().stream()
                .mapToInt(PackageDepsReport::getTotalDependencyCount)
                .sum();
    }

    public Map<String, Set<String>> getPackageDependencyGraph() {
        Map<String, Set<String>> graph = new HashMap<>();

        for (PackageDepsReport report : packageReports.values()) {
            String pkg = report.getPackageName();
            Set<String> dependencies = report.getDependentPackages();
            graph.put(pkg, dependencies);
        }
        return graph;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Project Name: ").append(projectName).append("\n");
        sb.append("Package Count: ").append(this.getPackageCount()).append("\n");
        sb.append("Class Count: ").append(this.getClassCount()).append("\n");
        sb.append("Total Dependencies: ").append(this.getTotalDependencyCount()).append("\n");
        sb.append("\nPackage dependency graph:\n");
        Map<String, Set<String>> graph = getPackageDependencyGraph();
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" -> ");
            sb.append(entry.getValue()).append("\n");
        }
        sb.append("Package Reports: \n");
        for (PackageDepsReport packageReport : packageReports.values()) {
            sb.append(packageReport.toString()).append("\n");
        }
        return sb.toString();
    }
}
