package org.deltacv.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Minimal TOML helper backed by Jackson TOML. Provides a thin API
 * compatible with the previous toml4j usages in this project:
 *   new Toml().read(file) -> returns Toml
 *   getString/getLong/getBoolean/getList/getTable/toMap
 */
public class Toml {

    private final Map<String, Object> data;

    private static final ObjectMapper MAPPER = new ObjectMapper(new TomlFactory());

    public Toml() {
        this.data = Collections.emptyMap();
    }

    private Toml(Map<String, Object> data) {
        this.data = data == null ? Collections.emptyMap() : data;
    }

    public Toml read(File f) {
        try (InputStream in = java.nio.file.Files.newInputStream(f.toPath())) {
            Map<String, Object> m = MAPPER.readValue(in, new TypeReference<Map<String, Object>>() {});
            return new Toml(m);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read TOML file: " + f.getAbsolutePath(), e);
        }
    }

    public Toml read(InputStream in) {
        try {
            Map<String, Object> m = MAPPER.readValue(in, new TypeReference<Map<String, Object>>() {});
            return new Toml(m);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read TOML from stream", e);
        }
    }

    @SuppressWarnings("unchecked")
    public String getString(String key) {
        Object v = data.get(key);
        return v == null ? null : v.toString();
    }

    public String getString(String key, String def) {
        String s = getString(key);
        return s == null ? def : s;
    }

    public boolean contains(String key) {
        return data != null && data.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public Long getLong(String key) {
        Object v = data.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    public Boolean getBoolean(String key) {
        Object v = data.get(key);
        if (v == null) return null;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    public boolean getBoolean(String key, boolean def) {
        Boolean b = getBoolean(key);
        return b == null ? def : b;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        Object v = data.get(key);
        if (v instanceof List) return (List<T>) v;
        return null;
    }

    @SuppressWarnings("unchecked")
    public Toml getTable(String key) {
        Object v = data.get(key);
        if (v instanceof Map) return new Toml((Map<String, Object>) v);
        return null;
    }

    public Map<String, Object> toMap() {
        return data == null ? Collections.emptyMap() : data;
    }
}


