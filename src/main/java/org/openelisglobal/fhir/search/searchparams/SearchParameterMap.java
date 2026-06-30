package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.rest.api.SortSpec;
import com.google.common.reflect.TypeToken;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.LinkedCaseInsensitiveMap;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SearchParameterMap implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    private SortSpec sortSpec;

    @Getter
    @Setter
    private int fromIndex = 0;

    @Getter
    @Setter
    private int toIndex = Integer.MAX_VALUE;

    private final Map<String, List<PropParam<?>>> params = new LinkedCaseInsensitiveMap<>();

    public SearchParameterMap addParameter(@Nonnull String key, @Nonnull Object param) {
        return addParameter(key, null, param);
    }

    public SearchParameterMap addParameter(@Nonnull String key, String propertyName, @Nonnull Object param) {
        if (key == null || param == null) {
            return this;
        }

        List<PropParam<?>> params = this.params.getOrDefault(key, new ArrayList<>());
        params.add(PropParam.builder().param(param).propertyName(propertyName).build());
        this.params.put(key, params);
        return this;
    }

    public Set<Map.Entry<String, List<PropParam<?>>>> getParameters() {
        return params.entrySet();
    }

    @SuppressWarnings("UnstableApiUsage")
    public <T> List<PropParam<T>> getParameters(@Nonnull String key) {
        TypeToken<T> type = new TypeToken<T>(getClass()) {
        };

        @SuppressWarnings("unchecked")
        List<PropParam<T>> result = this.params.getOrDefault(key, new ArrayList<>()).stream()
                .filter(param -> type.getRawType().isAssignableFrom(param.getClass()))
                .map(param -> (PropParam<T>) param).collect(Collectors.toList());

        return result;
    }

    public SearchParameterMap setSortSpec(SortSpec sortSpec) {
        this.sortSpec = sortSpec;
        return this;
    }
}
