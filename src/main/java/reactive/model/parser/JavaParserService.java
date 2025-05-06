package reactive.model.parser;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import io.reactivex.rxjava3.core.Single;

import java.io.File;

/**
 * Parse Java files and infer package names
 */
public class JavaParserService {
    private final JavaParser parser;

    public JavaParserService() {
        this.parser = createConfiguredParser(new CombinedTypeSolver(new ReflectionTypeSolver(false)));
    }

    /**
     * configure the source path for the JavaParser
     *
     * @param projectRoot root directory of the project
     */
    public void configureSourcePath(File projectRoot) {
        if (projectRoot != null && projectRoot.exists() && projectRoot.isDirectory()) {
            CombinedTypeSolver typeSolver = new CombinedTypeSolver();
            typeSolver.add(new ReflectionTypeSolver(false));
            typeSolver.add(new JavaParserTypeSolver(projectRoot));

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
            this.parser.getParserConfiguration().setSymbolResolver(symbolSolver);
        }
    }

    /**
     * Parse a Java file and return the CompilationUnit
     *
     * @param javaFile The Java file to parse
     * @return A Single with the CompilationUnit or an error if parsing fails
     */
    public Single<CompilationUnit> parseJavaFile(File javaFile) {
        return Single.fromCallable(() -> {
            ParseResult<CompilationUnit> result = parser.parse(javaFile);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                return result.getResult().get();
            } else {
                throw new RuntimeException("Errore di parsing: " +
                        result.getProblems().toString() + " nel file " + javaFile.getName());
            }
        });
    }

    /**
     * Infer the package name from a Java file
     * @param javaFile The Java file to parse
     * @return A Single with the package name or an empty string if not found
     */
    public Single<String> inferPackageName(File javaFile) {
        return parseJavaFile(javaFile)
                .map(cu -> cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse(""));
    }

   /**
     * Infer the package name from a directory containing Java files
     * @param packageDir The directory containing Java files
     * @return A Single with the package name or the directory name if not found
     */
    public Single<String> inferPackageNameFromDir(File packageDir) {
        File[] javaFiles = packageDir.listFiles((dir, name) -> name.endsWith(".java"));
        if (javaFiles != null && javaFiles.length > 0) {
            return inferPackageName(javaFiles[0])
                    .onErrorReturn(e -> packageDir.getName());
        }
        return Single.just(packageDir.getName());
    }
    /**
     * create a configured JavaParser instance
     *
     * @param typeSolver the type solver to use
     * @return a configured JavaParser instance
     */
    private JavaParser createConfiguredParser(CombinedTypeSolver typeSolver) {
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        JavaParser parser = new JavaParser();
        parser.getParserConfiguration().setSymbolResolver(symbolSolver);
        return parser;
    }
}