package lib.analyser;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import lib.report.ClassDepsReport;
import lib.utils.TypeDependency;

import java.util.*;

import static lib.utils.TypeDependency.DependencyType;

/**
 * Visitor for the analysis of dependencies in Java classes.
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
                if (shouldExcludeType(typeName)) {
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
                if (shouldExcludeType(typeName)) {
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
    public void visit(FieldDeclaration n, Void arg) {
        // Controlla i tipi dei campi
        for (VariableDeclarator variable : n.getVariables()) {
            Type type = variable.getType();
            try {
                String typeName = resolveTypeName(type);
                if (!shouldExcludeType(typeName)) {
                    report.addDependency(new TypeDependency(
                            sourceClassName, typeName, DependencyType.FIELD_TYPE,
                            "field " + variable.getNameAsString()
                    ));
                }
            } catch (Exception ignored) {
                // Ignora se non è possibile risolvere il tipo
            }
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        // Controlla il tipo di ritorno
        Type returnType = n.getType();
        try {
            String typeName = resolveTypeName(returnType);
            if (!shouldExcludeType(typeName)) {
                report.addDependency(new TypeDependency(
                        sourceClassName, typeName, DependencyType.METHOD_RETURN,
                        "method " + n.getNameAsString() + " return"
                ));
            }
        } catch (Exception ignored) {
            // Ignora se non è possibile risolvere il tipo
        }

        // Controlla i parametri
        for (Parameter parameter : n.getParameters()) {
            try {
                String typeName = resolveTypeName(parameter.getType());
                if (!shouldExcludeType(typeName)) {
                    report.addDependency(new TypeDependency(
                            sourceClassName, typeName, DependencyType.METHOD_PARAMETER,
                            "method " + n.getNameAsString() + " param " + parameter.getNameAsString()
                    ));
                }
            } catch (Exception ignored) {
                // Ignora se non è possibile risolvere il tipo
            }
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(ObjectCreationExpr n, Void arg) {
        // Analyze object creation expressions
        try {
            String typeName = resolveTypeName(n.getType());
            if (shouldExcludeType(typeName)) {
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

    private String resolveTypeName(Type type) {
        try {
            if (type.isClassOrInterfaceType()) {
                ResolvedType resolvedType = type.resolve();
                if (resolvedType.isReferenceType()) {
                    ResolvedReferenceType referenceType = resolvedType.asReferenceType();
                    return referenceType.getQualifiedName();
                }
            }
        } catch (Exception e) {
            // Fallback al nome semplice se la risoluzione fallisce
        }

        return type.asString();
    }

    private String resolveTypeName(ClassOrInterfaceType type) {
        try {
            ResolvedReferenceTypeDeclaration resolved = (ResolvedReferenceTypeDeclaration) type.resolve();
            return resolved.getQualifiedName();
        } catch (Exception e) {
            return type.getNameAsString();
        }
    }

    // Exclude void and primitive types
    private boolean shouldExcludeType(String typeName) {
        if (typeName == null
                || typeName.isEmpty()
                || typeName.equals("void")
                || isPrimitiveType(typeName)) {
            return false;
        }
        // Exclude base package types (java.lang, etc.)
        for (String excludedPackage : this.excludedPackages) {
            if (typeName.startsWith(excludedPackage + ".")) {
                return false;
            }
        }
        // Exclude types from the same class
        return !typeName.equals(sourceClassName);}

    private boolean isPrimitiveType(String typeName) {
        return Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char").contains(typeName);}}