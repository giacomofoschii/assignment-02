package common;

import com.github.javaparser.*;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;

import java.io.File;
import java.util.*;

public class ParserConfigurator {
    private final JavaParser parser;
    private final Set<String> excludedPackages = Set.of(
            "java.lang", "java.util", "java.io", "java.math",
            "java.time", "java.text", "java.nio", "java.net",
            "javafx", "org.graphstream", "com.github.javaparser"
    );

    public ParserConfigurator() {
        this.parser = createSimpleJavaParser();
    }

    public JavaParser getParser() {
        return parser;
    }

    public JavaParser createSimpleJavaParser() {
        JavaSymbolSolver symbolSolver =
                new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver(false)));
        JavaParser parser = new JavaParser();
        parser.getParserConfiguration().setSymbolResolver(symbolSolver);
        return parser;
    }

    public JavaParser createParserWithResolvers(List<File> sourceDirs) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver(false));
        typeSolver.add(new ClassLoaderTypeSolver(ClassLoader.getSystemClassLoader()));

        for (File dir : sourceDirs) {
            if (dir.exists() && dir.isDirectory()) {
                typeSolver.add(new JavaParserTypeSolver(dir));
            }
        }

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setSymbolResolver(symbolSolver);

        return new JavaParser(configuration);
    }

    // Exclude void and primitive types
    public boolean shouldIncludeType(String typeName) {
        if (typeName == null
                || typeName.isEmpty()
                || typeName.equals("void")
                || isPrimitiveType(typeName)
                || isArrayType(typeName)) {
            return false;
        }

        for (String excludedPackage : this.excludedPackages) {
            if (typeName.startsWith(excludedPackage)) {
                return false;
            }
        }

        return true;
    }

    private boolean isPrimitiveType(String typeName) {
        return Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char").contains(typeName);
    }

    private boolean isArrayType(String typeName) {
        return typeName.endsWith("[]");
    }
}
