package reactive.model.parser;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import common.report.*;
import common.util.TypeDependency;

import java.util.*;

import static common.util.TypeDependency.DependencyType.*;


/**
 * Visitor for analyzing dependencies in Java files.
 * Visit the AST and collects dependencies between types.
 */
public class DependencyVisitor extends VoidVisitorAdapter<Void> {
    private final ClassDepsReport report;
    private final String sourceClassName;
    private final Set<String> excludedPackages;

    public DependencyVisitor(ClassDepsReport report, String sourceClassName) {
        this.report = report;
        this.sourceClassName = sourceClassName;
        // Base Java packages to exclude
        this.excludedPackages = new HashSet<>(Arrays.asList(
                "java.lang", "java.util", "java.io", "java.math",
                "java.time", "java.text", "java.nio", "java.net"
        ));
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        //Check if the class is an interface
        for (ClassOrInterfaceType extendedType : n.getExtendedTypes()) {
            try {
                String typeName = resolveTypeName(extendedType);
                if (shouldIncludeType(typeName)) {
                    report.addDependency(new TypeDependency(
                            sourceClassName, typeName, EXTENDS,
                            "extends " + extendedType,
                            extendedType.getBegin().map(pos -> pos.line).orElse(-1)
                    ));
                }
            } catch (Exception ignored) {
                // Ignore type resolution errors
            }
        }

        // Check if the class implements interfaces
        for (ClassOrInterfaceType implementedType : n.getImplementedTypes()) {
            try {
                String typeName = resolveTypeName(implementedType);
                if (shouldIncludeType(typeName)) {
                    report.addDependency(new TypeDependency(
                            sourceClassName, typeName, IMPLEMENTS,
                            "implements " + implementedType,
                            implementedType.getBegin().map(pos -> pos.line).orElse(-1)
                    ));
                }
            } catch (Exception ignored) {
                // Ignore type resolution errors
            }
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(FieldDeclaration n, Void arg) {
        // Check the declared fields
        for (VariableDeclarator variable : n.getVariables()) {
            if (variable.getType().isClassOrInterfaceType()) {
                ClassOrInterfaceType type = variable.getType().asClassOrInterfaceType();
                try {
                    String typeName = resolveTypeName(type);
                    if (shouldIncludeType(typeName)) {
                        report.addDependency(new TypeDependency(
                                sourceClassName, typeName, FIELD,
                                variable.getType() + " " + variable.getName(),
                                variable.getBegin().map(pos -> pos.line).orElse(-1)
                        ));
                    }
                } catch (Exception ignored) {
                    // Ignore type resolution errors
                }
            }
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        // Check the return type
        Type returnType = n.getType();
        try {
            String typeName = resolveTypeName(returnType);
            if (shouldIncludeType(typeName)) {
                report.addDependency(new TypeDependency(
                        sourceClassName, typeName, METHOD_RETURN,
                        returnType + " " + n.getName() + "()",
                        returnType.getBegin().map(pos -> pos.line).orElse(-1)
                ));
            }
        } catch (Exception ignored) {
            // Ignore type resolution errors
        }

        // Check the parameters
        for (Parameter parameter : n.getParameters()) {
            try {
                String typeName = resolveTypeName(parameter.getType());
                if (shouldIncludeType(typeName)) {
                    report.addDependency(new TypeDependency(
                            sourceClassName, typeName, METHOD_PARAMETER,
                            parameter.toString(),
                            parameter.getBegin().map(pos -> pos.line).orElse(-1)
                    ));
                }
            } catch (Exception ignored) {
                // Ignore type resolution errors
            }
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(ObjectCreationExpr n, Void arg) {
        // Analyze object creation expressions
        try {
            String typeName = resolveTypeName(n.getType());
            if (shouldIncludeType(typeName)) {
                report.addDependency(new TypeDependency(
                        sourceClassName, typeName, INSTANTIATION,
                        "new " + n.getType() + "()",
                        n.getBegin().map(pos -> pos.line).orElse(-1)
                ));
            }
        } catch (Exception ignored) {
            // Ignore type resolution errors
        }

        super.visit(n, arg);
    }

    /**
     * Resolves the name of a type to its fully qualified name.
     *
     * @param type the type to resolve
     * @return the fully qualified name of the type
     */
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
            // Ignore type resolution errors
        }

        return type.asString();
    }

    /**
     * Determine if a type should be included in the dependency analysis
     *
     * @param typeName the name of the type to check
     * @return true if the type should be included, false otherwise
     */
    private boolean shouldIncludeType(String typeName) {
        if (typeName == null
                || typeName.isEmpty()
                || typeName.equals("void")
                || isPrimitiveType(typeName)
                || isArrayType(typeName)) {
            return false;
        }

        // Exclude base packages
        for (String excludedPackage : this.excludedPackages) {
            if (typeName.startsWith(excludedPackage)) {
                return false;
            }
        }

        // Exclude types from the same class
        return !typeName.equals(sourceClassName);
    }

    private boolean isPrimitiveType(String typeName) {
        return Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char").contains(typeName);
    }

    private boolean isArrayType(String typeName) {
        return typeName.endsWith("[]");
    }
}