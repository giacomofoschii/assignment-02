package lib.utils;

import java.util.*;

/**
 * Represents a single dependencies between classes
 *
 * @param sourceType dependent class
 * @param targetType origin class
 * @param type       type of dependency
 * @param location   location of the dependency in the source code
 */
public record TypeDependency(String sourceType, String targetType, DependencyType type, String location) {

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