package org.jenkinsci.plugins.pipeline.modeldefinition.ast;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.pipeline.modeldefinition.validator.ModelValidator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents what context in which to run the build - i.e., which label to run on, what Docker agent to run in, etc.
 * Corresponds to Agent.
 *
 * @author Andrew Bayer
 */
public final class ModelASTAgent extends ModelASTNestableMap {
    public ModelASTAgent(Object sourceLocation) {
        super(sourceLocation);
    }

    @Override
    public void validate(ModelValidator validator) {
        validator.validateElement(this);
        super.validate(validator);
    }

    public List<String> getKeyNames() {
        List<String> names = new ArrayList<>();
        for (ModelASTKey k : getEntries().keySet()) {
            names.add(k.getKey());
        }
        return names;
    }

    public ModelASTKey keyFromName(String name) {
        for (ModelASTKey k : getEntries().keySet()) {
            if (name.equals(k.getKey())) {
                return k;
            }
        }
        return null;
    }

    @Override
    public String toGroovy() {
        StringBuilder argStr = new StringBuilder("agent ");

        List<String> keys = getKeyNames();
        if (keys.size() == 1 && (keys.contains("none") || keys.contains("any"))) {
            argStr.append(keys.get(0));
            argStr.append("\n");
        } else {
            argStr.append(super.toGroovy());
        }

        return argStr.toString();
    }

    @Override
    public String toString() {
        return "ModelASTAgent{" +
                "entries=" + getEntries() +
                "}";
    }
}
