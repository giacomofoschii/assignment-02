package reactive.model;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.File;
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
    private JavaParser parser;
    private final Set<String> excludedPackages = Set.of(
            "java.lang", "java.util", "java.io", "java.math",
            "java.time", "java.text", "java.nio", "java.net",
            "javafx", "org.graphstream"
    );

    private void configureSymbolResolver(String projectPath) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(new File(projectPath)));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setSymbolResolver(symbolSolver);
        parser = new JavaParser(configuration);
    }

    // Get all Java files from the given directory recursively
    public Flowable<Path> getJavaFiles(String projectPath) {
        configureSymbolResolver(projectPath);

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
                    if (shouldIncludeType(qualifiedName)) {
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

    private boolean isPrimitiveType(String typeName) {
        return Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char").contains(typeName);
    }

    private boolean isJavaBuiltInType(String typeName) {
        return Set.of("List", "Vector", "Map", "Set", "Queue", "Deque", "Collections",
                "Arrays", "Objects", "Optional", "Stream", "Collectors", "Function",
                "Predicate", "Consumer", "Supplier", "Future", "Callable", "Runnable").contains(typeName);
    }

    private boolean isArrayType(String typeName) {
        return typeName.endsWith("[]");
    }

    private boolean shouldIncludeType(String typeName) {
        if (typeName == null
                || typeName.isEmpty()
                || typeName.equals("void")
                || isPrimitiveType(typeName)
                || isArrayType(typeName)
                || isJavaBuiltInType(typeName)) {
            return false;
        }
        for (String excludedPackage : this.excludedPackages) {
            if (typeName.startsWith(excludedPackage)) {
                return false;
            }
        }
        return true;
    }
}