package lib.utils;

public enum DependencyType {
    EXTENDS,
    IMPLEMENTS,
    INSTANTIATION,
    METHOD_PARAMETER,
    METHOD_RETURN,
    FIELD_TYPE,
    METHOD_INVOCATION,
    FIELD_ACCESS
}
