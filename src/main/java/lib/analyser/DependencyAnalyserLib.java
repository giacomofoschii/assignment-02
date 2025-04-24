package lib.analyser;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import io.vertx.core.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import lib.report.*;

/**
 * Main class that provides asynchronous methods for analysing dependencies of classes,
 * packages and Java projects.
 * It uses specific analysers to perform the analysis asynchronously.
 */
public class DependencyAnalyserLib {
    private final Vertx vertx;
    private final JavaParser parser;

    public DependencyAnalyserLib(Vertx vertx) {
        this.vertx = vertx;
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver(false)); // Excluded primitive types
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        this.parser = new JavaParser();
        this.parser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    public void configureSourceRepositories(List<File> rootDirs) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver(false));
        for (File rootDir : rootDirs) {
            if (rootDir.exists() && rootDir.isDirectory()) {
                typeSolver.add(new JavaParserTypeSolver(rootDir));}}
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        this.parser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    private Future<String> readFile(Path path) {
        final Promise<String> filePromise = Promise.promise();

        this.vertx.fileSystem().readFile(path.toString(), read -> {
            if (read.succeeded())
                filePromise.complete(read.result().toString("UTF-8"));
            else
                filePromise.fail(read.cause());
        });

        return filePromise.future();
    }

    private ParseResult<CompilationUnit> parseSourceCode(String sourceCode) {
        return this.parser.parse(sourceCode);
    }


    public Future<ClassDepsReport> getClassDependencies(Path classSrcFile) {
        Promise<ClassDepsReport> promise = Promise.promise();

//        this.vertx.executeBlocking(blockingPromise -> {
//            try {
//                ParseResult<CompilationUnit> parseResult = this.parser.parse(classSrcFile);
//
//                if (!parseResult.isSuccessful()) {
//                    blockingPromise.fail("Failed to parse " + classSrcFile.getFileName() + ": "
//                            + parseResult.getProblems());
//                    return;
//                }
//
//                CompilationUnit cu = parseResult.getResult().isPresent() ? parseResult.getResult().get() : null;
//
//                // Extract the class name from the CompilationUnit
//                if (cu != null) {
//                    String className = cu.getPackageDeclaration()
//                            .map(pd -> pd.getName().asString() + ".")
//                            .orElse("") + getMainClassName(cu);
//                    ClassDepsReport classReport = new ClassDepsReport(className);
//
//                    // Visit the AST to find dependencies
//                    cu.accept(new DependencyVisitor(classReport, className), null);
//
//                    blockingPromise.complete(classReport);
//                } else {
//                    blockingPromise.fail("Compilation unit is null for " + classSrcFile.getFileName());
//                }
//            } catch (FileNotFoundException e) {
//                blockingPromise.fail("File not found: " + classSrcFile);
//            } catch (Exception e) {
//                blockingPromise.fail("Error analyzing " + classSrcFile.getFileName() + ": " + e.getMessage());
//            }
//        }, promise);

        this.readFile(classSrcFile).onSuccess(sourceCode -> {
            ParseResult<CompilationUnit> parseResult = this.parseSourceCode(sourceCode);

            if (parseResult == null || !parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
                promise.fail("Failed to parse " + classSrcFile.getFileName() + ": " +
                        (parseResult != null ? parseResult.getProblems() : "ParseResult is null"));
                return;
            }

            CompilationUnit cu = parseResult.getResult().get();
            String className = cu.getPackageDeclaration()
                    .map(pd -> pd.getName().asString() + ".")
                    .orElse("") + getMainClassName(cu);
            ClassDepsReport classReport = new ClassDepsReport(className);

            // Visit the AST to find dependencies
            cu.accept(new DependencyVisitor(classReport, className), null);

            promise.complete(classReport);
        }).onFailure(cause -> {
            promise.fail("Error reading file " + classSrcFile.getFileName() + ": " + cause.getMessage());
        });

        return promise.future();
    }

    public Future<PackageDepsReport> getPackageDependencies(Path packageSrcFolder) {
        Promise<PackageDepsReport> promise = Promise.promise();

        if(!packageSrcFolder.toFile().isDirectory()
                || !packageSrcFolder.toFile().exists()) {
            return Future.failedFuture(packageSrcFolder + " is not a directory");
        }

        String packageName = inferPackageName(packageSrcFolder.toFile());
        PackageDepsReport packageReport = new PackageDepsReport(packageName);

        File[] javaFiles = packageSrcFolder.toFile().listFiles((dir, name) -> name.endsWith(".java"));
        if (javaFiles == null || javaFiles.length == 0) {
            return Future.succeededFuture(packageReport);
        }
        List<Future<ClassDepsReport>> classDepsFutures = new ArrayList<>();
        for (File javaFile : javaFiles) {
            classDepsFutures.add(this.getClassDependencies(javaFile.toPath()));
        }
        CompositeFuture.all(new ArrayList<>(classDepsFutures)).onSuccess(result -> {
            for (int i = 0; i < result.size(); i++) {
                ClassDepsReport classReport = result.resultAt(i);
                packageReport.addClassReport(classReport);
            }
            promise.complete(packageReport);
        }).onFailure(promise::fail);

        return promise.future();
    }

    public Future<ProjectDepsReport> getProjectDependencies(Path projectSrcFolder) {
        Promise<ProjectDepsReport> promise = Promise.promise();

        if(!projectSrcFolder.toFile().isDirectory()
                || !projectSrcFolder.toFile().exists()) {
            return Future.failedFuture(projectSrcFolder + " is not a directory");
        }

        this.configureSourceRepositories(Collections.singletonList(projectSrcFolder.toFile()));
        List<Path> packageDirs = findPackageDirectories(projectSrcFolder);

        String projectName = projectSrcFolder.getFileName().toString();
        ProjectDepsReport projectReport = new ProjectDepsReport(projectName);

        List<Future<PackageDepsReport>> packageDepsFutures = new ArrayList<>();
        for (Path packageDir : packageDirs) {
            packageDepsFutures.add(this.getPackageDependencies(packageDir));
        }
        CompositeFuture.all(new ArrayList<>(packageDepsFutures)).onSuccess(result -> {
            for (int i = 0; i < result.size(); i++) {
                PackageDepsReport packageReport = result.resultAt(i);
                projectReport.addPackageReport(packageReport);
            }
            promise.complete(projectReport);
        }).onFailure(promise::fail);

        return promise.future();
    }

    private String getMainClassName(CompilationUnit cu) {
        Optional<ClassOrInterfaceDeclaration> mainClass = cu.findFirst(ClassOrInterfaceDeclaration.class,
                c -> !c.isNestedType());
        return mainClass.map(ClassOrInterfaceDeclaration::getNameAsString).orElse("UnknownClass");
    }

    private String inferPackageName(File packageDir) {
        try {
            // Search for a Java file in the package directory
            File[] javaFiles = packageDir.listFiles((dir, name) -> name.endsWith(".java"));
            if (javaFiles != null && javaFiles.length > 0) {
                ParseResult<CompilationUnit> parseResult = this.parser.parse(javaFiles[0]);
                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();
                    if (cu.getPackageDeclaration().isPresent()) {
                        return cu.getPackageDeclaration().get().getNameAsString();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return packageDir.getName();
    }

    private List<Path> findPackageDirectories(Path projectDir) {
        List<Path> packageDirs = new ArrayList<>();

        this.findPackageDirsRecursive(projectDir.toFile(), packageDirs);

        return packageDirs;
    }

    private void findPackageDirsRecursive(File dir, List<Path> packageDirs) {
        // Check if the directory contains Java files
        File[] javaFiles = dir.listFiles((d, name) -> name.endsWith(".java"));
        if (javaFiles != null && javaFiles.length > 0) {
            packageDirs.add(dir.toPath());
        }

        // Explore the subdirectories
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                this.findPackageDirsRecursive(subdir, packageDirs);
            }
        }
    }
}
