package org.jenkinsci.plugins.pipeline.modeldefinition.ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.pipeline.modeldefinition.validator.ModelValidator;

/**
 * Represents a block of "foo = 'bar'" assignments to environment variables, corresponding to {@code Environment}.
 *
 * @author Andrew Bayer
 */
public final class ModelASTEnvironment extends ModelASTElement {
    private Map<ModelASTKey, ModelASTValue> variables = new LinkedHashMap<ModelASTKey, ModelASTValue>();
    private List<ModelASTEnvironmentContributor> contributors = new ArrayList<>();

    public ModelASTEnvironment(Object sourceLocation) {
        super(sourceLocation);
    }

    @Override
    public JSONArray toJSON() {
        JSONArray a = new JSONArray();

        for (Map.Entry<ModelASTKey, ModelASTValue> entry : variables.entrySet()) {
            JSONObject o = new JSONObject();
            o.accumulate("key", entry.getKey().toJSON());
            o.accumulate("value", entry.getValue().toJSON());
            a.add(o);
        }

        for (ModelASTEnvironmentContributor c : contributors) {
            a.add(c.toJSON());
        }

        return a;
    }

    @Override
    public void validate(final ModelValidator validator) {
        validator.validateElement(this);
        for (Map.Entry<ModelASTKey, ModelASTValue> entry : variables.entrySet()) {
            entry.getKey().validate(validator);
            entry.getValue().validate(validator);
        }
        for (ModelASTEnvironmentContributor contributor : contributors) {
            contributor.validate(validator);
        }
    }

    @Override
    public String toGroovy() {
        StringBuilder result = new StringBuilder("environment {\n");
        for (Map.Entry<ModelASTKey, ModelASTValue> entry : variables.entrySet()) {
            result.append(entry.getKey().toGroovy()).append(" = ").append(entry.getValue().toGroovy()).append('\n');
        }
        for (ModelASTEnvironmentContributor contributor : contributors) {
            result.append(contributor.toGroovy());
        }
        result.append("}\n");
        return result.toString();
    }

    @Override
    public void removeSourceLocation() {
        super.removeSourceLocation();
        for (Map.Entry<ModelASTKey, ModelASTValue> entry : variables.entrySet()) {
            entry.getKey().removeSourceLocation();
            entry.getValue().removeSourceLocation();
        }
        for (ModelASTEnvironmentContributor contributor : contributors) {
            contributor.removeSourceLocation();
        }
    }

    public List<ModelASTEnvironmentContributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<ModelASTEnvironmentContributor> contributors) {
        this.contributors = contributors;
    }

    public Map<ModelASTKey, ModelASTValue> getVariables() {
        return variables;
    }

    public void setVariables(Map<ModelASTKey, ModelASTValue> variables) {
        this.variables = variables;
    }

    @Override
    public String toString() {
        return "ModelASTEnvironment{" +
                "variables=" + variables +
                ", contributors=" + contributors +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ModelASTEnvironment that = (ModelASTEnvironment) o;

        if (getContributors() != null ? !getContributors().equals(that.getContributors()) : that.getContributors() != null) {
            return false;
        }

        return getVariables() != null ? getVariables().equals(that.getVariables()) : that.getVariables() == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getVariables() != null ? getVariables().hashCode() : 0);
        result = 31 * result + (getContributors() != null ? getContributors().hashCode() : 0);
        return result;
    }
}
