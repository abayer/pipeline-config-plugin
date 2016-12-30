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
public class ModelASTNestableMap extends ModelASTElement implements ModelASTNestableMapValue {
    private Map<ModelASTKey, ModelASTNestableMapValue> entries = new LinkedHashMap<>();

    public ModelASTNestableMap(Object sourceLocation) {
        super(sourceLocation);
    }

    @Override
    public JSONArray toJSON() {
        final JSONArray a = new JSONArray();

        for (Map.Entry<ModelASTKey, ModelASTNestableMapValue> entry: entries.entrySet()) {
            JSONObject o = new JSONObject();
            o.accumulate("key", entry.getKey().toJSON());
            o.accumulate("value", entry.getValue().toJSON());
            a.add(o);
        }
        return a;

    }

    @Override
    public void validate(ModelValidator validator) {
        validator.validateElement(this);

        for (Map.Entry<ModelASTKey, ModelASTNestableMapValue> entry : entries.entrySet()) {
            entry.getKey().validate(validator);
            entry.getValue().validate(validator);
        }
    }

    public List<String> getKeyNames() {
        List<String> names = new ArrayList<>();
        for (ModelASTKey k : entries.keySet()) {
            names.add(k.getKey());
        }
        return names;
    }

    public ModelASTKey keyFromName(String name) {
        for (ModelASTKey k : entries.keySet()) {
            if (name.equals(k.getKey())) {
                return k;
            }
        }
        return null;
    }

    @Override
    public String toGroovy() {
        StringBuilder argStr = new StringBuilder();

        argStr.append("{\n");
        for (Map.Entry<ModelASTKey, ModelASTNestableMapValue> entry: entries.entrySet()) {
            argStr.append(entry.getKey().toGroovy()).append(" ").append(entry.getValue().toGroovy()).append("\n");
        }
        argStr.append("}\n");

        return argStr.toString();
    }

    @Override
    public void removeSourceLocation() {
        super.removeSourceLocation();
        for (Map.Entry<ModelASTKey, ModelASTNestableMapValue> entry : entries.entrySet()) {
            entry.getKey().removeSourceLocation();
            entry.getValue().removeSourceLocation();
        }
    }

    public Map<ModelASTKey, ModelASTNestableMapValue> getEntries() {
        return entries;
    }

    public void setEntries(Map<ModelASTKey, ModelASTNestableMapValue> entries) {
        this.entries = entries;
    }

    @Override
    public String toString() {
        return "ModelASTNestableMap{" +
                "entries=" + entries +
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

        ModelASTNestableMap that = (ModelASTNestableMap) o;

        return getEntries() != null ? getEntries().equals(that.getEntries()) : that.getEntries() == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getEntries() != null ? getEntries().hashCode() : 0);
        return result;
    }

}
