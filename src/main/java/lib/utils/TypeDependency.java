package lib.utils;

import java.util.*;

/**
 * Represents a single dependencies between classes
 */
public class TypeDependency {
    private final String sourceType;
    private final String targetType;
    private final DependencyType type;
    private final String location;

    public enum DependencyType {
        EXTENDS,
        IMPLEMENTS,
        INSTANTIATION
    }

    public TypeDependency(String sourceType, String targetType, DependencyType type, String location) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.type = type;
        this.location = location;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getTargetType() {
        return targetType;
    }

    public DependencyType getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeDependency that = (TypeDependency) o;
        return Objects.equals(sourceType, that.sourceType) &&
                Objects.equals(targetType, that.targetType) &&
                type == that.type &&
                Objects.equals(location, that.location);
    }

    @Override
    public String toString() {
        return sourceType + " -> " + targetType + " (" + type + " at " + location + ")";
    }
}