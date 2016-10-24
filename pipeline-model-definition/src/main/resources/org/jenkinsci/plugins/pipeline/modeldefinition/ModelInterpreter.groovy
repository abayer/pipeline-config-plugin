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
package org.jenkinsci.plugins.pipeline.modeldefinition

import com.cloudbees.groovy.cps.impl.CpsClosure
import hudson.FilePath
import hudson.Launcher
import hudson.model.Result
import org.jenkinsci.plugins.pipeline.modeldefinition.model.Agent
import org.jenkinsci.plugins.pipeline.modeldefinition.model.Root
import org.jenkinsci.plugins.pipeline.modeldefinition.model.Stage
import org.jenkinsci.plugins.pipeline.modeldefinition.model.Tools
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException

/**
 * CPS-transformed code for actually performing the build.
 *
 * @author Andrew Bayer
 */
public class ModelInterpreter implements Serializable {
    private CpsScript script

    public ModelInterpreter(CpsScript script) {
        this.script = script
    }

    def call(CpsClosure closure) {
        // Attach the stages model to the run for introspection etc.
        Utils.attachExecutionModel(script)

        ClosureModelTranslator m = new ClosureModelTranslator(Root.class, script)

        closure.delegate = m
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()

        Root root = m.toNestedModel()
        Throwable firstError

        if (root != null) {
            def jobProps = []

            if (root.jobProperties != null) {
                jobProps.addAll(root.jobProperties.properties)
            }
            if (root.triggers != null) {
                jobProps.add(script.pipelineTriggers(root.triggers.triggers))
            }
            if (root.parameters != null) {
                jobProps.add(script.parameters(root.parameters.parameters))
            }
            if (!jobProps.isEmpty()) {
                script.properties(jobProps)
            }

            // Entire build, including notifications, runs in the withEnv.
            script.withEnv(root.getEnvVars()) {
                // Stage execution and post-build actions run in try/catch blocks, so we still run post-build actions
                // even if the build fails, and we still send notifications if the build and/or post-build actions fail.
                // We save the caught error, if any, for throwing at the end of the build.
                nodeOrDockerOrNone(root.agent) {
                    toolsBlock(root.agent, root.tools) {
                        // If we have an agent and script.scm isn't null, run checkout scm
                        if (root.agent.hasAgent() && Utils.hasScmContext(script)) {
                                script.checkout script.scm
                        }

                        for (int i = 0; i < root.stages.getStages().size(); i++) {
                            Stage thisStage = root.stages.getStages().get(i)

                            script.stage(thisStage.name) {
                                if (firstError == null) {
                                    nodeOrDockerOrNone(thisStage.agent) {
                                        try {
                                            catchRequiredContextForNode(root.agent) {
                                                setUpDelegate(thisStage.steps.closure).call()
                                            }.call()
                                        } catch (Exception e) {
                                            script.echo "Error in stages execution: ${e.getMessage()}"
                                            script.getProperty("currentBuild").result = Result.FAILURE
                                            if (firstError == null) {
                                                firstError = e
                                            }
                                        } finally {
                                            // And finally, run the post stage steps.
                                            List<Closure> postClosures = thisStage.satisfiedPostStageConditions(root, script.getProperty("currentBuild"))

                                            catchRequiredContextForNode(thisStage.agent != null ? thisStage.agent : root.agent, false) {
                                                if (postClosures.size() > 0) {
                                                    script.echo("Post stage") //TODO should this be a nested stage instead?
                                                    try {
                                                        for (int ni = 0; ni < postClosures.size(); ni++) {
                                                            setUpDelegate(postClosures.get(ni)).call()
                                                        }
                                                    } catch (Exception e) {
                                                        script.echo "Error in stage post: ${e.getMessage()}"
                                                        script.getProperty("currentBuild").result = Result.FAILURE
                                                        if (firstError == null) {
                                                            firstError = e
                                                        }
                                                    }
                                                }
                                            }.call()
                                        }
                                    }.call()
                                }
                            }
                        }

                        try {
                            catchRequiredContextForNode(root.agent) {
                                List<Closure> postBuildClosures = root.satisfiedPostBuilds(script.getProperty("currentBuild"))
                                if (postBuildClosures.size() > 0) {
                                    script.stage("Post Build Actions") {
                                        for (int i = 0; i < postBuildClosures.size(); i++) {
                                            setUpDelegate(postBuildClosures.get(i)).call()
                                        }
                                    }
                                }
                            }.call()
                        } catch (Exception e) {
                            script.echo "Error in postBuild execution: ${e.getMessage()}"
                            script.getProperty("currentBuild").result = Result.FAILURE
                            if (firstError == null) {
                                firstError = e
                            }
                        }
                    }.call()
                }.call()

                try {
                    // And finally, run the notifications.
                    List<Closure> notificationClosures = root.satisfiedNotifications(script.getProperty("currentBuild"))

                    catchRequiredContextForNode(root.agent, true) {
                        if (notificationClosures.size() > 0) {
                            script.stage("Notifications") {
                                for (int i = 0; i < notificationClosures.size(); i++) {
                                    setUpDelegate(notificationClosures.get(i)).call()
                                }
                            }
                        }
                    }.call()
                } catch (Exception e) {
                    script.echo "Error in notifications execution: ${e.getMessage()}"
                    script.getProperty("currentBuild").result = Result.FAILURE
                    if (firstError == null) {
                        firstError = e
                    }
                }
            }
            if (firstError != null) {
                throw firstError
            }
        }
    }

    Closure setUpDelegate(Closure c) {
        c.delegate = script
        c.resolveStrategy = Closure.DELEGATE_FIRST
        return c
    }

    def catchRequiredContextForNode(Agent agent, boolean inNotifications = false, Closure body) throws Exception {
        return {
            try {
                body.call()
            } catch (MissingContextVariableException e) {
                if (FilePath.class.equals(e.type) || Launcher.class.equals(e.type)) {
                    if (inNotifications) {
                        script.error("Attempted to execute a notification step that requires a node context. Notifications do not run inside a 'node { ... }' block.")
                    } else if (!agent.hasAgent()) {
                        script.error("Attempted to execute a step that requires a node context while 'agent none' was specified. " +
                            "Be sure to specify your own 'node { ... }' blocks when using 'agent none'.")
                    } else {
                        throw e
                    }
                } else {
                    throw e
                }
            }
        }
    }

    def toolsBlock(Agent agent, Tools tools, Closure body) {
        // If there's no agent, don't install tools in the first place.
        if (agent.hasAgent() && tools != null) {
            def toolEnv = []
            def toolsList = tools.getToolEntries()
            for (int i = 0; i < toolsList.size(); i++) {
                def entry = toolsList.get(i)
                String k = entry.get(0)
                String v= entry.get(1)

                String toolPath = script.tool(name:v, type:Tools.typeForKey(k))

                toolEnv.addAll(script.envVarsForTool(toolId: Tools.typeForKey(k), toolVersion: v))
            }

            return {
                script.withEnv(toolEnv) {
                    body.call()
                }
            }
        } else {
            return {
                body.call()
            }
        }
    }

    /*
    TODO: The agent handling stuff here is just waiting for step-in-Groovy support..
     */
    def nodeOrDockerOrNone(Agent agent, Closure body) {
        if (agent != null && agent.hasAgent()) {
            return {
                nodeWithLabelOrWithout(agent) {
                    dockerOrWithout(agent, body).call()
                }.call()
            }
        } else {
            return {
                body.call()
            }
        }
    }

    def dockerOrWithout(Agent agent, Closure body) {
        if (agent.docker != null) {
            return {
                script.getProperty("docker").image(agent.docker).inside(agent.dockerArgs, {
                    body.call()
                })
            }
        } else {
            return {
                body.call()
            }
        }
    }

    def nodeWithLabelOrWithout(Agent agent, Closure body) {
        if (agent?.label != null) {
            return {
                script.node(agent.label) {
                    body.call()
                }
            }
        } else {
            if (agent?.hasDocker()) {
                String dl = script.dockerLabel()?.trim()
                if (dl) {
                    return {
                        script.node(dl) {
                            body.call()
                        }
                    }
                }
            }
            return {
                script.node {
                    body.call()
                }
            }
        }
    }
}
