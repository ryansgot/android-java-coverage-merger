package com.fsryan.gradle.coverage

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.UnknownDomainObjectException
import org.slf4j.LoggerFactory

class MergedReportConfigExtension {

    private static final LOGGER = LoggerFactory.getLogger(MergedReportConfigExtension.class)

    NamedDomainObjectContainer<ClassFilter> classFilters

    MergedReportConfigExtension(NamedDomainObjectContainer<ClassFilter> classFilters) {
        this.classFilters = classFilters;
    }

    def classFilters(final Closure configureClosure) {
        classFilters.configure(configureClosure)
    }

    List<String> includesFor(variant) {
        return listPropertyOf(variant, "includes")
    }

    List<String> excludesFor(variant) {
        return listPropertyOf(variant, "excludes")
    }

    private List<String> listPropertyOf(variant, String propertyName) {
        List<String> ret = filterValuesFromKey(variant.buildType.name, propertyName)
        if (variant.flavorName != null && !variant.flavorName.isEmpty()) {
            ret.addAll(filterValuesFromKey(variant.flavorName, propertyName))
        }
        return ret
    }

    private List<String> filterValuesFromKey(String filterKey, String propertyName) {
        try {
            ClassFilter typeFilter = classFilters.getByName(filterKey)
            if (typeFilter != null) {
                return typeFilter.properties.get(propertyName)
            }
        } catch (UnknownDomainObjectException udoe) {
            LOGGER.debug("Did not find object $filterKey", udoe)
        }

        return []
    }
}