package org.jenkinsci.plugins.pipeline.modeldefinition.ast

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.jenkinsci.plugins.pipeline.modeldefinition.validator.ModelValidator

import javax.annotation.Nonnull

/**
 * Represents the named parameters for a step in a map of {@link ModelASTKey}s and {@link ModelASTValue}s.
 *
 * @author Kohsuke Kawaguchi
 * @author Andrew Bayer
 */
@ToString(includeSuper = true, includeSuperProperties = true)
@EqualsAndHashCode(callSuper = true)
@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public final class ModelASTNamedArgumentList extends ModelASTArgumentList {
    Map<ModelASTKey,ModelASTValue> arguments = [:]

    public ModelASTNamedArgumentList(Object sourceLocation) {
        super(sourceLocation)
    }

    @Override
    public JSONArray toJSON() {
        JSONArray a = new JSONArray()

        arguments.each { k,v ->
            JSONObject o = new JSONObject()
            o.accumulate("key", k.toJSON())
            o.accumulate("value", v.toJSON())
            a.add(o)
        }
        return a
    }

    /**
     * Checks if a given key name is present.
     *
     * @param keyName The name of a key to check for.
     * @return True if a {@link ModelASTKey} with that name is present in the map.
     */
    public boolean containsKeyName(@Nonnull String keyName) {
        return arguments.any { k, v ->
            keyName.equals(k.key)
        }
    }

    @Override
    public void validate(ModelValidator validator) {
        // Nothing to validate directly
        arguments.each { k, v ->
            k?.validate(validator)
            v?.validate(validator)
        }

    }

    @Override
    public String toGroovy() {
        return arguments.collect { k, v ->
            "${k.toGroovy()}: ${v.toGroovy()}"
        }.join(", ")
    }

    @Override
    public void removeSourceLocation() {
        super.removeSourceLocation()

        arguments.each { k, v ->
            k.removeSourceLocation()
            v.removeSourceLocation()
        }
    }
}
