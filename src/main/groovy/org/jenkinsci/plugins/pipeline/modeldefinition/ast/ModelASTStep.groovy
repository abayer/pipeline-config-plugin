package org.jenkinsci.plugins.pipeline.modeldefinition.ast

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.transform.ToString
import net.sf.json.JSONObject
import org.jenkinsci.plugins.pipeline.modeldefinition.validator.ModelValidator

/**
 * Represents an individual step within any of the various blocks that can contain steps.
 *
 * @author Kohsuke Kawaguchi
 * @author Andrew Bayer
 */
@ToString(includeSuper = true, includeSuperProperties = true)
@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public class ModelASTStep extends ModelASTElement {
    /**
     * A list of step names which are banned from being executed within a step block.
     */
    public final Map<String, String> blockedSteps = [
        "stage":      "The stage step cannot be used in step blocks in Pipeline Config",
        "properties": "The properties step cannot be used in step blocks in Pipeline Config",
        "parallel":   "The parallel step can only be used as the only top-level step in a stage's step block"
    ]

    String name;
    ModelASTArgumentList args;

    ModelASTStep(Object sourceLocation) {
        super(sourceLocation)
    }

    @Override
    public JSONObject toJSON() {
        return new JSONObject()
            .accumulate("name", name)
            .accumulate("arguments", args.toJSON())
    }

    @Override
    public void validate(ModelValidator validator) {
        validator.validateElement(this)

        args?.validate(validator)
    }

    @Override
    public String toGroovy() {
        return "${name}(${args.toGroovy()})"
    }

    @Override
    public void removeSourceLocation() {
        super.removeSourceLocation()
        args.removeSourceLocation()
    }
}
