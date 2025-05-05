package asynchronous.report;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("-----------------------PROJECT------------------------\n");
        sb.append("Project Name: ").append(projectName).append("\n");
        sb.append("Package Count: ").append(this.getPackageCount()).append("\n");
        sb.append("Class Count: ").append(this.getClassCount()).append("\n");
        sb.append("Total Dependencies: ").append(this.getTotalDependencyCount()).append("\n");
        sb.append("Package Reports: \n");
        for (PackageDepsReport packageReport : packageReports.values()) {
            sb.append("\t").append(packageReport.toString().replace("\n", "\n\t")).append("\n");
        }
        sb.append("---------------------END-PROJECT----------------------\n");
        return sb.toString();
    }
}
