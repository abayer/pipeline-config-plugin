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
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted


/**
 * A container for one or more {@link Stage}s to be executed within the build, in the order they're declared.
 *
 * @author Andrew Bayer
 */
@ToString
@EqualsAndHashCode
@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public class Stages implements NestedModel, Serializable {
    @Whitelisted
    List<Stage> stages = []

    @Whitelisted
    Stages stages(List<Stage> s) {
        this.stages = s
        return this
    }

    @Whitelisted
    List<Stage> getStages() {
        return stages
    }

    Map<String,Stage> getStageNameMap() {
        return stages.collectEntries { s ->
            [s.name, s]
        }
    }

    List<Stage> getLinearOrderedStages() {
        StageDependencyGraph graph = new StageDependencyGraph()
        stages.each { s ->
            graph.addStage(s.name)

            if (s.dependsOn != null && s.dependsOn.depends != null) {
                s.dependsOn.depends.each { d ->
                    graph.addDependency(d, s.name)
                }
            }
        }

        def stageMap = getStageNameMap()

        return graph.linearOrderedStages.collect { stageMap.get(it) }
    }

    @Override
    @Whitelisted
    public void modelFromMap(Map<String,Object> m) {
        m.each { k, v ->
            this."${k}"(v)
        }
    }

}
