package org.jenkinsci.plugins.nunit_runner;

public enum NUnitFramework {
    FRAMEWORK_35("framework35"),
    FRAMEWORK_40("framework40"),
    FRAMEWORK_45("framework45");

    private final String name;

    NUnitFramework(String s) {
        name = s;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
