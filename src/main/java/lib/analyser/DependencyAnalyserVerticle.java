package lib.analyser;

import io.vertx.core.*;
import lib.report.*;

import java.nio.file.Path;

public class DependencyAnalyserVerticle extends AbstractVerticle {

    private static final String CURRENT_PATH = System.getProperty("user.dir");
    private static final Path CLASS_PATH = Path.of(CURRENT_PATH + "\\src\\main\\java\\lib\\report\\ClassDepsReport.java");
    private static final Path PACKAGE_PATH = Path.of(CURRENT_PATH + "\\src\\main\\java\\lib\\report");
    private static final Path PROJECT_PATH = Path.of(CURRENT_PATH);

    @Override
    public void start(Promise<Void> startPromise) {
        final DependencyAnalyserLib dependencyAnalyser = new DependencyAnalyserLib(this.vertx);

        final Future<ClassDepsReport> classReport = dependencyAnalyser.getClassDependencies(CLASS_PATH);
        final Future<PackageDepsReport> packageReport = dependencyAnalyser.getPackageDependencies(PACKAGE_PATH);
        final Future<ProjectDepsReport> projectReport = dependencyAnalyser.getProjectDependencies(PROJECT_PATH);

        CompositeFuture.all(classReport, packageReport, projectReport)
                .onSuccess(res -> {
                    System.out.println(classReport.result().toString());
                    System.out.println(packageReport.result().toString());
                    System.out.println(projectReport.result().toString());
                    startPromise.complete();
                })
                .onFailure(System.err::println);
    }
}
