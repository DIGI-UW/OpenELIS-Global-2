package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.rest.api.SortSpec;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * Stores FHIR search parameters and their associated OpenELIS entity
 * properties.
 *
 * <p>
 * Parameter names are handled case-insensitively. Multiple values registered
 * under the same key are preserved in insertion order.
 * </p>
 */
@EqualsAndHashCode
public class SearchParameterMap implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    private SortSpec sortSpec;

    @Getter
    @Setter
    private int fromIndex;

    @Getter
    @Setter
    private int toIndex;

    private final Map<String, List<PropParam<?>>> params;

    public SearchParameterMap() {
        this.params = new LinkedCaseInsensitiveMap<>();
        this.fromIndex = 0;
        this.toIndex = Integer.MAX_VALUE;
    }

    public SearchParameterMap(SortSpec sortSpec, int fromIndex, int toIndex) {

        this();

        this.sortSpec = sortSpec;
        setRange(fromIndex, toIndex);
    }

    /**
     * Adds a parameter without an explicit persistence property name.
     */
    public SearchParameterMap addParameter(@Nonnull String key, Object param) {

        return addParameter(key, null, param);
    }

    /**
     * Adds a parameter and its associated persistence property.
     *
     * @param key          FHIR search parameter name
     * @param propertyName OpenELIS entity property path
     * @param param        HAPI FHIR parameter value
     * @return this map
     */
    public SearchParameterMap addParameter(@Nonnull String key, String propertyName, Object param) {

        if (key == null || key.isBlank() || param == null) {

            return this;
        }

        List<PropParam<?>> parameterValues = params.computeIfAbsent(key, ignored -> new ArrayList<>());

        parameterValues.add(PropParam.builder().propertyName(propertyName).param(param).build());

        return this;
    }

    /**
     * Returns all registered parameter entries.
     */
    public Set<Map.Entry<String, List<PropParam<?>>>> getParameters() {

        return Collections.unmodifiableSet(params.entrySet());
    }

    /**
     * Returns every parameter registered under the supplied key.
     */
    public List<PropParam<?>> getParameters(@Nonnull String key) {

        if (key == null || key.isBlank()) {

            return Collections.emptyList();
        }

        List<PropParam<?>> values = params.get(key);

        if (values == null || values.isEmpty()) {

            return Collections.emptyList();
        }

        return Collections.unmodifiableList(values);
    }

    /**
     * Returns parameters whose wrapped value matches the requested Java type.
     *
     * @param key           search parameter key
     * @param parameterType expected wrapped parameter type
     * @param <T>           parameter type
     */
    @SuppressWarnings("unchecked")
    public <T> List<PropParam<T>> getParameters(@Nonnull String key, @Nonnull Class<T> parameterType) {

        Objects.requireNonNull(parameterType, "Parameter type must not be null");

        return getParameters(key).stream().filter(Objects::nonNull).filter(propParam -> propParam.getParam() != null)
                .filter(propParam -> parameterType.isInstance(propParam.getParam()))
                .map(propParam -> (PropParam<T>) propParam).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns the first parameter registered under the key.
     */
    public PropParam<?> getFirstParameter(@Nonnull String key) {

        List<PropParam<?>> values = getParameters(key);

        return values.isEmpty() ? null : values.get(0);
    }

    /**
     * Returns the first wrapped value matching the requested type.
     */
    public <T> T getFirstParameterValue(@Nonnull String key, @Nonnull Class<T> parameterType) {

        return getParameters(key, parameterType).stream().findFirst().map(PropParam::getParam).orElse(null);
    }

    public boolean containsParameter(@Nonnull String key) {

        return key != null && !key.isBlank() && params.containsKey(key) && !params.get(key).isEmpty();
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public int size() {
        return params.size();
    }

    public SearchParameterMap setSortSpec(SortSpec sortSpec) {

        this.sortSpec = sortSpec;
        return this;
    }

    /**
     * Sets the requested result range.
     *
     * The from index is inclusive and the to index is exclusive.
     */
    public SearchParameterMap setRange(int fromIndex, int toIndex) {

        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex must be zero or greater");
        }

        if (toIndex < fromIndex) {
            throw new IllegalArgumentException("toIndex must be greater than or equal to fromIndex");
        }

        this.fromIndex = fromIndex;
        this.toIndex = toIndex;

        return this;
    }

    public int getPageSize() {

        if (toIndex == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return toIndex - fromIndex;
    }

    /**
     * Removes all search parameters while retaining paging and sorting values.
     */
    public void clearParameters() {
        params.clear();
    }
}
