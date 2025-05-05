package reactive.model;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import reactive.model.parser.DependencyVisitor;
import reactive.model.parser.JavaParserService;
import common.report.ClassDepsReport;
import common.report.PackageDepsReport;
import common.report.ProjectDepsReport;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is responsible for analyzing the dependencies of a Java project.
 * It uses JavaParser to extract dependencies from the source files and models
 * them using the PackageDependency, ClassDependency, and ProjectDependency classes.
 */
public class ReactiveDependencyAnalyser {
    private final JavaParserService parserService;
    private final AtomicInteger classCounter;
    private final AtomicInteger dependencyCounter;

    public ReactiveDependencyAnalyser() {
        this.parserService = new JavaParserService();
        this.classCounter = new AtomicInteger(0);
        this.dependencyCounter = new AtomicInteger(0);
    }

    public AtomicInteger getClassCounter() {
        return classCounter;
    }

    public AtomicInteger getDependencyCounter() {
        return dependencyCounter;
    }

    @Override
    public Observable<ClassDepsReport> analyzeClass(File classFile) {
        if (!classFile.exists() || !classFile.isFile() || !classFile.getName().endsWith(".java")) {
            return Observable.error(new IllegalArgumentException("Il file non è un file Java valido: " + classFile.getPath()));
        }

        return parserService.parseJavaFile(classFile)
                .flatMap(cu -> extractClassInfo(cu, classFile))
                .doOnSuccess(report -> {
                    classCounter.incrementAndGet();
                    dependencyCounter.addAndGet(report.getDependencyCount());
                })
                .toObservable()
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<PackageDepsReport> analyzePackage(File packageFolder) {
        if (!packageFolder.exists() || !packageFolder.isDirectory()) {
            return Observable.error(new IllegalArgumentException("La cartella non è valida: " + packageFolder.getPath()));
        }

        return parserService.inferPackageNameFromDir(packageFolder)
                .flatMapObservable(packageName -> {
                    PackageDepsReport packageReport = new PackageDepsReport(packageName);

                    return findJavaFiles(packageFolder)
                            .flatMap(this::analyzeClass)
                            .doOnNext(packageReport::addClassReport)
                            .doOnComplete(() -> {
                                // Potrebbe essere vuoto, gestisci questo caso
                                if (packageReport.getClassCount() == 0) {
                                    classCounter.incrementAndGet(); // Contatore di classi per aggiornare l'UI
                                }
                            })
                            .ignoreElements()
                            .andThen(Observable.just(packageReport));
                });
    }

    @Override
    public Observable<ProjectDepsReport> analyzeProject(File projectFolder) {
        if (!projectFolder.exists() || !projectFolder.isDirectory()) {
            return Observable.error(new IllegalArgumentException("La cartella di progetto non è valida: " + projectFolder.getPath()));
        }

        // Configura il parser con il path del progetto
        parserService.configureSourcePath(projectFolder);

        // Resetta i contatori
        classCounter.set(0);
        dependencyCounter.set(0);

        String projectName = projectFolder.getName();
        ProjectDepsReport projectReport = new ProjectDepsReport(projectName);

        return findPackageFolders(projectFolder)
                .flatMap(this::analyzePackage)
                .doOnNext(projectReport::addPackageReport)
                .ignoreElements()
                .andThen(Observable.just(projectReport));
    }
}
