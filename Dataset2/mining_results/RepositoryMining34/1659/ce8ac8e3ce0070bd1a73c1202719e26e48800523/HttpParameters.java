package org.apache.struts2.dispatcher;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class HttpParameters implements Cloneable {

    private Map<String, Parameter> parameters;

    private HttpParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    public static Builder create(Map requestParameterMap) {
        return new Builder(requestParameterMap);
    }

    public static Builder createEmpty() {
        return new Builder(new HashMap<String, String[]>());
    }

    public Parameter get(String name) {
        if (parameters.containsKey(name)) {
            return parameters.get(name);
        } else {
            return new Parameter.EmptyHttpParameter(name);
        }
    }

    public Set<String> getNames() {
        return new TreeSet<>(parameters.keySet());
    }

    public HttpParameters remove(Set<String> paramsToRemove) {
        for (String paramName : paramsToRemove) {
            parameters.remove(paramName);
        }
        return this;
    }

    public HttpParameters remove(final String paramToRemove) {
        return remove(new HashSet<String>() {{
            add(paramToRemove);
        }});
    }

    public boolean contains(String name) {
        return parameters.containsKey(name);
    }

    public HttpParameters clone(Map<String, ?> newParams) {
        return HttpParameters.createEmpty().withParent(this).withExtraParams(newParams).build();
    }

    public Map<String, String[]> getHttpParameters() {
        Map<String, String[]> result = new HashMap<>(parameters.size());
        for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getMultipleValue());
        }
        return result;
    }

    public static class Builder {
        private Map<String, String[]> requestParameterMap;
        private HttpParameters parent;

        protected Builder(Map<String, ?> requestParameterMap) {
            this.requestParameterMap = toStringArrayMap(requestParameterMap);
        }

        private Map<String, String[]> toStringArrayMap(Map<String, ?> map) {
            Map<String, String[]> result = new TreeMap<>();
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value != null && value.getClass().isArray()) {
                    Object[] values = (Object[]) value;
                    String[] strValues = new String[values.length];
                    int i = 0;
                    for (Object v : values) {
                        strValues[i] = String.valueOf(v);
                        i++;
                    }
                    result.put(entry.getKey(), strValues);
                } else if (value != null) {
                    result.put(entry.getKey(), new String[]{String.valueOf(value)});
                }
            }
            return result;
        }

        public Builder withParent(HttpParameters parentParams) {
            if (parentParams != null) {
                parent = parentParams;
            }
            return this;
        }

        public Builder withExtraParams(Map<String, ?> params) {
            if (params != null) {
                requestParameterMap.putAll(toStringArrayMap(params));
            }
            return this;
        }

        public Builder withComparator(Comparator<String> orderedComparator) {
            requestParameterMap = new TreeMap<>(orderedComparator);
            return this;
        }

        public HttpParameters build() {
            Map<String, Parameter> parameters = (parent == null)
                    ? new HashMap<String, Parameter>()
                    : new HashMap<>(parent.parameters);

            for (Object o : requestParameterMap.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                String name = String.valueOf(entry.getKey());
                String[] value = (String[]) entry.getValue();
                parameters.put(name, new Parameter.Request(name, value));
            }

            return new HttpParameters(parameters);
        }
    }
}
