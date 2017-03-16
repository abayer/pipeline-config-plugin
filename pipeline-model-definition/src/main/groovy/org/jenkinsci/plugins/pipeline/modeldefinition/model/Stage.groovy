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
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTPostStage
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStage
import org.jenkinsci.plugins.pipeline.modeldefinition.steps.CredentialWrapper
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.job.WorkflowRun

import javax.annotation.CheckForNull
import javax.annotation.Nonnull

/**
 * An individual stage to be executed within the build.
 *
 * @author Andrew Bayer
 */
@ToString
@EqualsAndHashCode
@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public class Stage implements Serializable {

    String name

    String steps

    Agent agent

    PostStage post

    StageConditionals when

    Tools tools

    Environment environment

    /**
     * Helper method for translating the key/value pairs in the {@link Environment} into a list of "key=value" strings
     * suitable for use with the withEnv step.
     *
     * @return a list of "key=value" strings.
     */
    List<String> getEnvVars(Root root, CpsScript script) {
        if (environment != null) {
            return environment.resolveEnvVars(script, true, root.environment).findAll {
                it.key in environment.keySet()
            }.collect { k, v ->
                "${k}=${v}"
            }
        } else {
            return []
        }
    }

    @Nonnull
    Map<String, CredentialWrapper> getEnvCredentials() {
        Map<String, CredentialWrapper> m = [:]
        environment.each {k, v ->
            if (v instanceof  CredentialWrapper) {
                m["${k}"] = v;
            }
        }
        return m
    }

    @CheckForNull
    public static Stage fromAST(@Nonnull WorkflowRun r, @CheckForNull ModelASTStage ast, @CheckForNull Root root) {
        if (ast != null) {
            Stage s = new Stage()
            s.name = ast.name

            s.environment = Environment.fromAST(r, ast.environment)
            s.when = StageConditionals.fromAST(ast.when, root)
            s.agent = Agent.fromAST(ast.agent)
            s.tools = Tools.fromAST(ast.tools)
            s.post = PostStage.fromAST(ast.post)
            s.steps = root?.appendImports(ast.getStepsAsString()) ?: ast.getStepsAsString()

            return s
        } else {
            return null
        }
    }
}
