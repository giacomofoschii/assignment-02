package reactive.model;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Model class for dependency analysis using reactive streams
 */
public class ReactiveDependencyAnalyser {
    private static final Set<String> EXCLUDED_PACKAGES = Set.of(
            "java.lang", "java.util", "java.io", "java.math",
            "java.time", "java.text", "java.nio", "java.net",
            "javafx", "org.graphstream"
    );

    // Get all Java files from the given directory recursively
    public Flowable<Path> getJavaFiles(String projectPath) {
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
            JavaParser parser = new JavaParser();
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
                if (!depName.equals(className) && !isJavaBuiltInType(depName)) {
                    // Try to resolve the full name if possible
                    String fullName = type.getScope()
                            .map(scope -> scope.asString() + "." + depName)
                            .orElse(depName);
                    if (EXCLUDED_PACKAGES.stream().noneMatch(fullName::startsWith)) {
                        dependencies.add(fullName);
                    }
                }
            });

            return new ClassDependency(fullClassName, dependencies);
        } catch (IOException e) {
            return new ClassDependency(file.getFileName().toString().replace(".java", ""), new HashSet<>());
        }
    }

    // Check if a type is a Java built-in type
    private boolean isJavaBuiltInType(String typeName) {
        return typeName.equals("String") ||
                typeName.equals("Object") ||
                typeName.equals("Throwable") ||
                typeName.equals("Exception") ||
                typeName.equals("RuntimeException") ||
                typeName.equals("Error");
    }
}