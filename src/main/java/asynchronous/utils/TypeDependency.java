package asynchronous.utils;

import java.util.*;

/**
 * Represents a single dependencies between classes
 */
public class TypeDependency {

    public enum DependencyType {
        IMPORT,
        EXTENDS,
        IMPLEMENTS,
        INSTANTIATION,
        FIELD,
        METHOD_PARAMETER,
        METHOD_RETURN
    }

    private final String sourceType;
    private final String targetType;
    private final DependencyType type;
    private final String previewCode;
    private final int lineNum;

    public TypeDependency(String sourceType, String targetType, DependencyType type, String previewCode, int lineNum) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.type = type;
        this.previewCode = previewCode;
        this.lineNum = lineNum;
    }

    public String getSourceType() {
        return this.sourceType;
    }

    public String getTargetType() {
        return this.targetType;
    }

    public DependencyType getType() {
        return this.type;
    }

    public String getSourceCode() {return this.previewCode;}

    public int getLineNumber() {
        return this.lineNum;
    }

    public boolean hasLineNumber() {
        return this.lineNum > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeDependency that = (TypeDependency) o;
        return Objects.equals(sourceType, that.sourceType) &&
                Objects.equals(targetType, that.targetType) &&
                type == that.type && lineNum == that.lineNum &&
                Objects.equals(previewCode, that.previewCode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.sourceType).append(" -> ").append(targetType).append(" (").append(type);

        if (this.previewCode != null && hasLineNumber()) {
            sb.append(": ").append(this.previewCode);
            sb.append(" at line: ").append(this.lineNum);
        }
        sb.append(")");
        return sb.toString();
    }
}