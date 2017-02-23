/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.modeldefinition.environment;

import hudson.ExtensionList;
import org.jenkinsci.plugins.pipeline.modeldefinition.withscript.WithScriptDescriptor;
import org.jenkinsci.plugins.structs.SymbolLookup;
import org.jenkinsci.plugins.structs.describable.DescribableModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base {@code Descriptor} for types of {@link DeclarativeEnvironmentContributor}s.
 */
public abstract class DeclarativeEnvironmentContributorDescriptor<C extends DeclarativeEnvironmentContributor<C>> extends WithScriptDescriptor<C> {

    public abstract boolean isBlock();

    public List<String> blockMethods() {
        return Collections.emptyList();
    }

    /**
     * Get all {@link DeclarativeEnvironmentContributorDescriptor}s.
     *
     * @return a list of all {@link DeclarativeEnvironmentContributorDescriptor}s registered.`
     */
    public static ExtensionList<DeclarativeEnvironmentContributorDescriptor> all() {
        return ExtensionList.lookup(DeclarativeEnvironmentContributorDescriptor.class);
    }

    /**
     * Get a map of name-to-{@link DescribableModel} of all known/registered descriptors.
     *
     * @return A map of name-to-{@link DescribableModel}s
     */
    public static Map<String,DescribableModel> getDescribableModels() {
        Map<String,DescribableModel> models = new HashMap<>();

        for (DeclarativeEnvironmentContributorDescriptor d : all()) {
            for (String s : SymbolLookup.getSymbolValue(d)) {
                models.put(s, new DescribableModel<>(d.clazz));
            }
        }

        return models;
    }

    /**
     * Get the descriptor for a given name or null if not found.
     *
     * @param name The name for the descriptor to look up
     * @return The corresponding descriptor or null if not found.
     */
    @Nullable
    public static DeclarativeEnvironmentContributorDescriptor byName(@Nonnull String name) {
        return (DeclarativeEnvironmentContributorDescriptor) SymbolLookup.get().findDescriptor(DeclarativeEnvironmentContributor.class, name);
    }
}
