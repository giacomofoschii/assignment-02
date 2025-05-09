package reactive.model;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import common.ParserConfigurator;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Model class for dependency analysis using reactive streams
 */
public class ReactiveDependencyAnalyser {
    private JavaParser parser;
    private final ParserConfigurator parserConfigurator = new ParserConfigurator();


    // Get all Java files from the given directory recursively
    public Flowable<Path> getJavaFiles(String projectPath) {
        this.parser = parserConfigurator.createParserWithResolvers(List.of(new File(projectPath)));

        return Flowable.defer(() -> {
            try (Stream<Path> paths = Files.walk(Paths.get(projectPath))) {
                Set<Path> javaFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .collect(Collectors.toSet());
                return Flowable.fromIterable(javaFiles);
            } catch (IOException e) {
                return Flowable.error(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    // Parse a Java file to extract class dependencies
    public ClassDependency parseClassDependencies(Path file) {
        try {
            CompilationUnit cu = parser.parse(file).getResult().orElseThrow();
            String className = cu.getPrimaryType()
                    .map(TypeDeclaration::getNameAsString)
                    .orElse(file.getFileName().toString().replace(".java", ""));
            String packageName = cu.getPackageDeclaration()
                    .map(pkg -> pkg.getName().asString())
                    .orElse("default");
            String fullClassName = packageName + "." + className;

            // Find all class/interface type references
            Set<String> dependencies = new HashSet<>();
            cu.findAll(ClassOrInterfaceType.class).forEach(type -> {
                String depName = type.getNameAsString();
                if (!depName.equals(className)) {
                    String qualifiedName = resolveTypeName(type, depName);
                    if (parserConfigurator.shouldIncludeType(qualifiedName)) {
                        dependencies.add(qualifiedName);
                    }
                }
            });

            return new ClassDependency(fullClassName, dependencies);
        } catch (IOException e) {
            return new ClassDependency(file.getFileName().toString().replace(".java", ""), new HashSet<>());
        }
    }

    private String resolveTypeName(ClassOrInterfaceType type, String defaultName) {
        try {
            return type.resolve().asReferenceType().getQualifiedName();
        } catch (Exception ignored) {
        }
        return type.getScope()
                .map(scope -> scope.asString() + "." + defaultName)
                .orElse(defaultName);
    }
}