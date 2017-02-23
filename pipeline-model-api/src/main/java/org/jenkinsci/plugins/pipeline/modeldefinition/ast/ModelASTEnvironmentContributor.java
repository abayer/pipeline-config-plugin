/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
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
 * Represents the contents of a block for a DeclarativeEnvironmentContributor within a {@link ModelASTEnvironment}
 *
 * @author Andrew Bayer
 */
public final class ModelASTEnvironmentContributor extends ModelASTElement {
    private String type;
    private List<ModelASTMethodCall> contents = new ArrayList<>();

    public ModelASTEnvironmentContributor(Object sourceLocation) {
        super(sourceLocation);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        o.accumulate("type", type);

        JSONArray a = new JSONArray();
        for (ModelASTMethodCall m : contents) {
            a.add(m.toJSON());
        }
        o.accumulate("contents", a);

        return new JSONObject().accumulate("contributor", o);
    }

    @Override
    public void validate(final ModelValidator validator) {
        validator.validateElement(this);
    }

    @Override
    public String toGroovy() {
        StringBuilder result = new StringBuilder(type + " {\n");
        for (ModelASTMethodCall m : contents) {
            result.append(m.toGroovy()).append("\n");
        }
        result.append("}\n");
        return result.toString();
    }

    @Override
    public void removeSourceLocation() {
        super.removeSourceLocation();
        for (ModelASTMethodCall m : contents) {
            m.removeSourceLocation();
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String t) {
        this.type = t;
    }

    public List<ModelASTMethodCall> getContents() {
        return contents;
    }

    public void setContents(List<ModelASTMethodCall> contents) {
        this.contents = contents;
    }

    @Override
    public String toString() {
        return "ModelASTEnvironmentContributor{" +
                "type=" + type +
                ", contents=" + contents +
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

        ModelASTEnvironmentContributor that = (ModelASTEnvironmentContributor) o;

        if (getType() != null ? !getType().equals(that.getType()) : that.getType() != null) {
            return false;
        }
        return getContents() != null ? getContents().equals(that.getContents()) : that.getContents() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        result = 31 * result + (getContents() != null ? getContents().hashCode() : 0);
        return result;
    }
}
