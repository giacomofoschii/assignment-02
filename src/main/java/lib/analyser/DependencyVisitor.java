package lib.analyser;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import lib.report.ClassDepsReport;
import lib.utils.TypeDependency;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static lib.utils.TypeDependency.DependencyType;

/**
 * Visitor per l'analisi delle dipendenze in un file .java
 */
public class DependencyVisitor extends VoidVisitorAdapter<Void> {
    private final ClassDepsReport report;
    private final String sourceClassName;
    private final Set<String> excludedPackages;

    public DependencyVisitor(ClassDepsReport report, String sourceClassName) {
        this.report = report;
        this.sourceClassName = sourceClassName;
        this.excludedPackages = new HashSet<>(Arrays.asList(
                "java.lang", "java.util", "java.io", "java.math",
                "java.time", "java.text", "java.nio", "java.net"
        ));
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        // Check for class or interface dependencies
        for (ClassOrInterfaceType extendedType : n.getExtendedTypes()) {
            try {
                String typeName = resolveTypeName(extendedType);
                if (!shouldExcludeType(typeName)) {
                    report.addDependency(new TypeDependency(
                            sourceClassName, typeName, DependencyType.EXTENDS,
                            "class " + n.getNameAsString()
                    ));
                }
            } catch (Exception ignored) {
            }
        }

        for (ClassOrInterfaceType implementedType : n.getImplementedTypes()) {
            try {
                String typeName = resolveTypeName(implementedType);
                if (!shouldExcludeType(typeName)) {
                    report.addDependency(new TypeDependency(
                            sourceClassName, typeName, DependencyType.IMPLEMENTS,
                            "class " + n.getNameAsString()
                    ));
                }
            } catch (Exception ignored) {
            }
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(ObjectCreationExpr n, Void arg) {
        // Analizza le istanziazioni (new)
        try {
            String typeName = resolveTypeName(n.getType());
            if (!shouldExcludeType(typeName)) {
                report.addDependency(new TypeDependency(
                        sourceClassName, typeName, DependencyType.INSTANTIATION,
                        n.getParentNode().map(Object::toString).orElse("unknown")
                ));
            }
        } catch (Exception ignored) {
        }

        super.visit(n, arg);
    }

    public void addPackageToExclude(String packageName) {
        this.excludedPackages.add(packageName);
    }

    private String resolveTypeName(ClassOrInterfaceType type) {
        try {
            ResolvedReferenceTypeDeclaration resolved = (ResolvedReferenceTypeDeclaration) type.resolve();
            return resolved.getQualifiedName();
        } catch (Exception e) {
            // Fallback al nome semplice se la risoluzione fallisce
            return type.getNameAsString();
        }
    }

    private boolean shouldExcludeType(String typeName) {// Escludi tipi primitivi e void
        if (typeName == null
                || typeName.isEmpty()
                || typeName.equals("void")
                || isPrimitiveType(typeName)) {
            return true;
        }
        // Escludi tipi dei pacchetti base (java.lang, etc.)
        for (String excludedPackage : this.excludedPackages) {
            if (typeName.startsWith(excludedPackage + ".")) {
                return true;
            }
        }
        // Escludi riferimenti alla stessa classe
        return typeName.equals(sourceClassName);}

    private boolean isPrimitiveType(String typeName) {
        return Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char").contains(typeName);}}