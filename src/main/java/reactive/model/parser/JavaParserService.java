package reactive.model.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.reactivex.rxjava3.core.Single;
import reactive.model.report.ClassDepsReport;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Servizio per il parsing dei file Java utilizzando JavaParser.
 */
public class JavaParserService {
    private final JavaParser parser;

    public JavaParserService() {
        this.parser = createConfiguredParser(new CombinedTypeSolver(new ReflectionTypeSolver(false)));
    }

    /**
     * Configura il JavaParser con il source path per risolvere le dipendenze
     *
     * @param projectRoot La directory radice del progetto
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
     * Parsa un file Java e restituisce la CompilationUnit
     *
     * @param javaFile Il file Java da parsare
     * @return Un Single con la CompilationUnit risultante o errore
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
     * Inferisce il nome del pacchetto da un file Java
     *
     * @param javaFile Il file Java
     * @return Un Single con il nome del pacchetto o vuoto se non trovato
     */
    public Single<String> inferPackageName(File javaFile) {
        return parseJavaFile(javaFile)
                .map(cu -> cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse(""));
    }

    /**
     * Inferisce il nome del pacchetto da una directory contenente file Java
     *
     * @param packageDir La directory del pacchetto
     * @return Un Single con il nome del pacchetto o il nome della directory se non trovato
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
     * Crea un JavaParser configurato con un resolver di simboli
     *
     * @param typeSolver Il risolutore di tipo da utilizzare
     * @return Il JavaParser configurato
     */
    private JavaParser createConfiguredParser(CombinedTypeSolver typeSolver) {
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        JavaParser parser = new JavaParser();
        parser.getParserConfiguration().setSymbolResolver(symbolSolver);
        return parser;
    }
}