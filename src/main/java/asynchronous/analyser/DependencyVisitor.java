package asynchronous.analyser;

import asynchronous.util.TypeDependency;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import asynchronous.report.ClassDepsReport;

import java.util.*;

import static asynchronous.util.TypeDependency.DependencyType.*;

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
                "java.time", "java.text", "java.nio", "java.net",
                "javafx", "org.graphstream"
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
                            sourceClassName, typeName, EXTENDS,
                            "extends " + extendedType,
                            extendedType.getBegin().map(pos -> pos.line).orElse(-1)
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
                            sourceClassName, typeName, IMPLEMENTS,
                            "implements " + implementedType,
                            implementedType.getBegin().map(pos -> pos.line).orElse(-1)
                    ));
                }
            } catch (Exception ignored) {
            }
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(FieldDeclaration n, Void arg) {
        // Check field declarations
        for (VariableDeclarator variable : n.getVariables()) {
            if (variable.getType().isClassOrInterfaceType()) {
                ClassOrInterfaceType type = variable.getType().asClassOrInterfaceType();
                try {
                    String typeName = resolveTypeName(type);
                    if (shouldExcludeType(typeName)) {
                        report.addDependency(new TypeDependency(
                                sourceClassName, typeName, FIELD,
                                variable.getType() + " " + variable.getName(),
                                variable.getBegin().map(pos -> pos.line).orElse(-1)
                        ));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        // Check return type
        Type returnType = n.getType();
        try {
            String typeName = resolveTypeName(returnType);
            if (shouldExcludeType(typeName)) {
                report.addDependency(new TypeDependency(
                        sourceClassName, typeName, METHOD_RETURN,
                        returnType + " " + n.getName() + "()",
                        returnType.getBegin().map(pos -> pos.line).orElse(-1)
                ));
            }
        } catch (Exception ignored) {
        }

        // Check parameters
        for (Parameter parameter : n.getParameters()) {
            try {
                String typeName = resolveTypeName(parameter.getType());
                if (shouldExcludeType(typeName)) {
                    report.addDependency(new TypeDependency(
                            sourceClassName, typeName, METHOD_PARAMETER,
                            parameter.toString(),
                            parameter.getBegin().map(pos -> pos.line).orElse(-1)
                    ));
                }
            } catch (Exception ignored) {
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
                        sourceClassName, typeName, INSTANTIATION,
                        "new " + n.getType() + "()",
                        n.getBegin().map(pos -> pos.line).orElse(-1)
                ));
            }
        } catch (Exception ignored) {
        }

        super.visit(n, arg);
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
        } catch (Exception ignored) {
        }

        return type.asString();
    }

    // Exclude void and primitive types
    private boolean shouldExcludeType(String typeName) {
        if (typeName == null
                || typeName.isEmpty()
                || typeName.equals("void")
                || isPrimitiveType(typeName)
                || isArrayType(typeName)) {
            return false;
        }
        // Exclude base package types (java.lang, etc.)
        for (String excludedPackage : this.excludedPackages) {
            if (typeName.startsWith(excludedPackage)) {
                return false;
            }
        }
        // Exclude types from the same class
        return !typeName.equals(sourceClassName);}

    private boolean isPrimitiveType(String typeName) {
        return Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char").contains(typeName);
    }

    private boolean isArrayType(String typeName) {
        return typeName.endsWith("[]");
    }
}
