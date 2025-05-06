package reactive.model;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import reactive.model.parser.*;
import common.report.*;
import common.util.DependencyVisitor;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is responsible for analyzing the dependencies of a Java project.
 * It uses JavaParser to extract dependencies from the source files and models
 * them using a reactive approach with RxJava.
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

    /**
     * Analyzes a single Java class file to extract its dependencies.
     *
     * @param classFile The Java source file to analyze
     * @return An Observable that emits a ClassDepsReport
     */
    public Observable<ClassDepsReport> analyzeClass(File classFile) {
        if (!classFile.exists() || !classFile.isFile() || !classFile.getName().endsWith(".java")) {
            return Observable.error(new IllegalArgumentException("Invalid Java file: " + classFile.getPath()));
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

    /**
     * Extracts class information from a parsed CompilationUnit.
     *
     * @param cu The parsed CompilationUnit
     * @param classFile The source Java file
     * @return A Single that emits a ClassDepsReport
     */
    private Single<ClassDepsReport> extractClassInfo(CompilationUnit cu, File classFile) {
        return Single.fromCallable(() -> {
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getName().asString())
                    .orElse("");

            Optional<ClassOrInterfaceDeclaration> mainClassOpt = cu.findFirst(ClassOrInterfaceDeclaration.class,
                    c -> !c.isNestedType());

            if (mainClassOpt.isEmpty()) {
                return new ClassDepsReport(packageName + ".UnknownClass");
            }

            String className = packageName.isEmpty() ?
                    mainClassOpt.get().getNameAsString() :
                    packageName + "." + mainClassOpt.get().getNameAsString();

            ClassDepsReport report = new ClassDepsReport(className);

            // Apply visitor pattern to extract dependencies
            cu.accept(new DependencyVisitor(report, className), null);

            return report;
        });
    }

    /**
     * Finds all Java files in a directory (non-recursively).
     *
     * @param directory The directory to search in
     * @return An Observable that emits Java files
     */
    private Observable<File> findJavaFiles(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".java"));
        return files == null ? Observable.empty() : Observable.fromArray(files);
    }

    /**
     * Analyzes all Java classes in a package directory.
     *
     * @param packageFolder The package directory to analyze
     * @return An Observable that emits a PackageDepsReport
     */
    public Observable<PackageDepsReport> analyzePackage(File packageFolder) {
        if (!packageFolder.exists() || !packageFolder.isDirectory()) {
            return Observable.error(new IllegalArgumentException("Invalid package folder: " + packageFolder.getPath()));
        }

        return parserService.inferPackageNameFromDir(packageFolder)
                .flatMapObservable(packageName -> {
                    PackageDepsReport packageReport = new PackageDepsReport(packageName);

                    return findJavaFiles(packageFolder)
                            .flatMap(this::analyzeClass)
                            .doOnNext(packageReport::addClassReport)
                            .doOnComplete(() -> {
                                // Handle empty packages
                                if (packageReport.getClassCount() == 0) {
                                    classCounter.incrementAndGet();
                                }
                            })
                            .ignoreElements()
                            .andThen(Observable.just(packageReport));
                });
    }

    /**
     * Finds all package directories in a project (recursively).
     *
     * @param projectDir The project root directory
     * @return An Observable that emits package directories
     */
    private Observable<File> findPackageFolders(File projectDir) {
        return Observable.create(emitter -> {
            findPackageDirsRecursive(projectDir, emitter);
            emitter.onComplete();
        });
    }

    /**
     * Helper method to recursively find package directories.
     *
     * @param dir The directory to search in
     * @param emitter The emitter to emit found directories
     */
    private void findPackageDirsRecursive(File dir, ObservableEmitter<File> emitter) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }

        // Check if directory contains Java files
        File[] javaFiles = dir.listFiles((d, name) -> name.endsWith(".java"));
        if (javaFiles != null && javaFiles.length > 0) {
            emitter.onNext(dir);
        }

        // Recursively search subdirectories
        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                findPackageDirsRecursive(subDir, emitter);
            }
        }
    }

    /**
     * Analyzes all packages in a project to extract dependencies.
     *
     * @param projectFolder The project root directory
     * @return An Observable that emits a ProjectDepsReport
     */
    public Observable<ProjectDepsReport> analyzeProject(File projectFolder) {
        if (!projectFolder.exists() || !projectFolder.isDirectory()) {
            return Observable.error(new IllegalArgumentException("Invalid project folder: " + projectFolder.getPath()));
        }

        // Configure the parser with the project path
        parserService.configureSourcePath(projectFolder);

        // Reset counters
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