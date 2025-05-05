package asynchronous.report;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("-----------------------PACKAGE------------------------\n");
        sb.append("Package Name: ").append(packageName).append("\n");
        sb.append("Class Count: ").append(this.getClassCount()).append("\n");
        sb.append("Total Dependencies: ").append(this.getTotalDependencyCount()).append("\n");
        sb.append("Class Reports: \n");
        for (ClassDepsReport classReport : classReports.values()) {
            sb.append("\t").append(classReport.toString().replace("\n", "\n\t")).append("\n");
        }
        sb.append("---------------------END-PACKAGE----------------------\n");
        return sb.toString();
    }
}
