package com.fsryan.gradle.coverage

class ClassFilter {
    String name
    List<String> includes = []
    List<String> excludes = []

    ClassFilter(final String name) {
        this.name = name
    }
}
