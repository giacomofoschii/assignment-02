package lib.utils;

import java.util.*;

/**
 * Rappresenta una singola dipendenza tra classi
 *
 * @param sourceType Classe che dipende
 * @param targetType Classe da cui dipende
 * @param type       Tipo di dipendenza
 * @param location   Dove si verifica (es. nome metodo, attributo)
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