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
import org.jgrapht.DirectedGraph
import org.jgrapht.alg.CycleDetector
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleDirectedGraph
import org.jgrapht.graph.UnmodifiableDirectedGraph
import org.jgrapht.traverse.TopologicalOrderIterator

import javax.annotation.Nonnull


@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public class StageDependencyGraph {
    private DirectedGraph stageGraph

    public StageDependencyGraph() {
        stageGraph = new SimpleDirectedGraph<String, DefaultEdge>(DefaultEdge.class)
    }

    /**
     * Adds a new stage to the graph. Returns true if the stage has been added correctly, and false if it was already
     * present.
     *
     * @param stageName
     * @return true if added, false if already present
     */
    public boolean addStage(@Nonnull String stageName) {
        return stageGraph.addVertex(stageName)
    }

    /**
     * Adds a dependency with the target stage depending on the source stage. Adds the stages if needed. Returns true if
     * able to add the dependency, false if adding this dependency would introduce a cycle to the dependency graph,
     * which is not allowed
     *
     * @param sourceStage
     * @param targetStage
     * @return true if added successfully or already present, false if adding the dependency would create a cycle
     */
    public boolean addDependency(@Nonnull String sourceStage, @Nonnull String targetStage) {
        if (!stageGraph.containsVertex(sourceStage)) {
            addStage(sourceStage)
        }
        if (!stageGraph.containsVertex(targetStage)) {
            addStage(targetStage)
        }

        // Add the dependency to the graph, and then check for cycles. If cycles are found, remove the edge.
        stageGraph.addEdge(sourceStage, targetStage)

        CycleDetector<String, DefaultEdge> detector = new CycleDetector<String, DefaultEdge>(stageGraph)

        if (detector.detectCycles()) {
            // TODO: Log/report the cycles somehow? May need to pass in a TaskListener here.
            stageGraph.removeEdge(sourceStage, targetStage)
            return false
        } else {
            return true
        }
    }

    /**
     * Get the next set of stages to run. Can be empty if there are no stages left.
     *
     * @param parallelism Integer for parallelism to allow - i.e., how many stages to return at once. Defaults to 1.
     * @return The list of stages to run next
     */
    public List<String> getNextStages(int parallelism = 1) {
        // Return the stages to run - can be empty.
        def iter = new TopologicalOrderIterator<String, DefaultEdge>(stageGraph)
        def candidateStages = iter.findAll {
            stageGraph.incomingEdgesOf(it)?.isEmpty()
        }

        // Just return the stages if parallelism is 0 or the number of candidate stages is less than or equal to the
        // parallelism count *or* if candidate stages is null or empty.
        if (parallelism == 0
            || candidateStages == null
            || candidateStages.isEmpty()
            || candidateStages.size() <= parallelism) {
            return candidateStages
        } else {
            // Otherwise, return the first N stages where N is the parallelism count.
            return candidateStages[0..<parallelism]
        }
    }

    /**
     * Given a list of stages that have already been run, remove them from the graph.
     *
     * @param stages
     */
    public void postStageProcessing(List<String> stages) {
        stages.each {
            stageGraph.removeVertex(it)
        }
    }

    /**
     * Returns true if there are more stages yet to be run/removed.
     *
     * @return true if there are still stages in the graph, false otherwise
     */
    public boolean hasMoreStages() {
        return !stageGraph.vertexSet().isEmpty()
    }

    public List<String> getLinearOrderedStages() {
        List<String> stages = []

        while (hasMoreStages()) {
            List<String> nextStages = getNextStages(1)
            stages.add(nextStages[0])
            postStageProcessing(nextStages)
        }

        return stages
    }

    public DirectedGraph<String, DefaultEdge> getGraphCopy() {
        return new UnmodifiableDirectedGraph<String, DefaultEdge>(stageGraph)
    }

    /**
     * Generates a StageDependencyGraph from a map of stages and their dependencies.
     *
     * @param stages a list of stages
     * @return a populated StageDependencyGraph
     */
    public static StageDependencyGraph fromStageMap(Map<String,List<String>> stages) {
        def graph = new StageDependencyGraph()

        stages.each { name, deps ->
            // Make sure we add each stage even if it's not connected to anything else.
            graph.addStage(name)

            deps.each { d ->
                graph.addDependency(name, d)
            }
        }

        graph
    }

    private static final int serialVersionUID = 1L
}
