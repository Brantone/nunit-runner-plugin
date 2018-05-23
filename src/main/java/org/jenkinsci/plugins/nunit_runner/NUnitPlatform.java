package org.jenkinsci.plugins.nunit_runner;

public enum NUnitPlatform {
    ARM("ARM"),
    X86("x86"),
    X64("x64");

    private final String name;


    NUnitPlatform(String s) {
        name = s;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
