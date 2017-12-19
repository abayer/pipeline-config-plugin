/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
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
package org.jenkinsci.plugins.pipeline.modeldefinition.model

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.jenkinsci.plugins.pipeline.modeldefinition.agent.DeclarativeAgent
import org.jenkinsci.plugins.pipeline.modeldefinition.agent.DeclarativeAgentDescriptor
import org.jenkinsci.plugins.pipeline.modeldefinition.agent.impl.None
import org.jenkinsci.plugins.pipeline.modeldefinition.options.DeclarativeOption
import org.jenkinsci.plugins.pipeline.modeldefinition.options.impl.SkipDefaultCheckout
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted
import org.jenkinsci.plugins.structs.SymbolLookup
import org.jenkinsci.plugins.structs.describable.UninstantiatedDescribable

import javax.annotation.CheckForNull

/**
 * What context the build should run in - i.e., on a given label, within a container of a given Docker agent, or without
 * any automatic management of node/agent/etc.
 *
 * @author Andrew Bayer
 */
@ToString
@EqualsAndHashCode
@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
class Agent extends MappedClosure<Object,Agent> implements Serializable {

    @Whitelisted
    Agent(Map<String,Object> inMap) {
        resultMap = inMap
    }

    @Deprecated
    DeclarativeAgent getDeclarativeAgent(Object context) {
        return getDeclarativeAgent(null, context)
    }

    /**
     * Get the appropriate instantiated {@link DeclarativeAgent} corresponding to our arguments.
     *
     * @return The instantiated declarative agent or null if not found.
     */
    DeclarativeAgent getDeclarativeAgent(@CheckForNull Root root, Object context) {
        String foundSymbol = findSymbol()
        if (foundSymbol != null) {
            DeclarativeAgentDescriptor foundDescriptor = DeclarativeAgentDescriptor.byName(foundSymbol)
            def val = getMap().get(foundSymbol)
            def argMap = [:]
            if (val instanceof Map) {
                argMap.putAll(val)
            } else {
                argMap.put(UninstantiatedDescribable.ANONYMOUS_KEY, val)
            }

            DeclarativeAgent a = DeclarativeAgentDescriptor.instanceForDescriptor(foundDescriptor, argMap)

            boolean doCheckout = true
            Map<String,DeclarativeOption> options = [:]

            if (context instanceof Root) {
                a.setInStage(false)
                options = ((Root)context).options?.options
            } else if (context instanceof Stage) {
                a.setInStage(true)
                options = ((Stage)context).options?.options
            }
            if (options != null && !options.isEmpty()) {
                SkipDefaultCheckout skip = (SkipDefaultCheckout) options.get("skipDefaultCheckout")
                if (skip?.isSkipDefaultCheckout()) {
                    doCheckout = true
                }
            }
            a.setDoCheckout(doCheckout)

            return a
        } else {
            return null
        }
    }

    /**
     * Needed to handle the combination of describable ordinals *and* Descriptor lookup.
     * @return The first symbol (in descriptor-ordinal-order searching) found in the map.
     */
    private String findSymbol() {
        String sym = null
        DeclarativeAgentDescriptor.all().each { d ->
            SymbolLookup.getSymbolValue(d)?.each { s ->
                if (getMap().containsKey(s) && sym == null) {
                    sym = s
                }
            }
        }

        return sym
    }

    boolean hasAgent() {
        DeclarativeAgent a = getDeclarativeAgent(null, null)
        return a != null && !None.class.isInstance(a)
    }
}
