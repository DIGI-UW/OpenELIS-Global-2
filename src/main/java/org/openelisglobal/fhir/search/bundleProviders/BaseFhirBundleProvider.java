package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.InstantType;
import org.openelisglobal.common.util.ConfigurationProperties;

/**
 * Generic lazy-loading bundle provider for OpenELIS FHIR searches.
 *
 * @param <E> OpenELIS persistence entity type
 * @param <R> FHIR resource type
 */
public abstract class BaseFhirBundleProvider<E, R extends IBaseResource> implements PagedBundleProvider {

    private static final int DEFAULT_PAGE_SIZE = resolveDefaultPageSize();

    /**
     * Unique identifier for this search result set.
     *
     * The value must remain stable for the lifetime of this bundle provider.
     */
    private final String searchUuid = UUID.randomUUID().toString();

    /**
     * Time at which this search provider was created.
     */
    private final Date publishedDate = new Date();

    /**
     * Cached number of matching records.
     */
    private Integer cachedTotalSize;

    /**
     * Preferred page size returned to HAPI FHIR.
     */
    private int preferredPageSize = DEFAULT_PAGE_SIZE;

    /**
     * Offset requested with {@code _offset}, or null when the client did not ask
     * for one.
     */
    private Integer currentPageOffset;

    /**
     * Page size that goes with {@link #currentPageOffset}.
     */
    private Integer currentPageSize;

    /**
     * Loads entities from the database for the requested range.
     *
     * @param offset   first result offset
     * @param pageSize maximum number of results
     * @return matching entities
     */
    protected abstract List<E> loadEntities(int offset, int pageSize);

    /**
     * Counts all entities matching the current search.
     *
     * @return total result count
     */
    protected abstract long countEntities();

    /**
     * Converts an OpenELIS entity into a FHIR resource.
     *
     * @param entity OpenELIS entity
     * @return FHIR resource
     */
    protected abstract R transformEntity(E entity);

    /**
     * Loads and transforms only the range requested by HAPI FHIR.
     *
     * HAPI treats {@code fromIndex} as inclusive and {@code toIndex} as exclusive.
     *
     * <p>
     * Once {@code _offset} is in play HAPI stops asking for a range at all: it
     * calls this with the whole of {@code 0..MAX_VALUE} and returns whatever comes
     * back, because in that mode the provider owns the paging. The requested range
     * is therefore ignored in favour of the offset the client asked for - reading
     * it literally is what made every page after the first return the entire result
     * set.
     */
    @Override
    public List<IBaseResource> getResources(int fromIndex, int toIndex) {

        validateRange(fromIndex, toIndex);

        int from = effectiveOffset(fromIndex);
        int pageSize = effectivePageSize(fromIndex, toIndex);

        if (pageSize == 0) {
            return Collections.emptyList();
        }

        List<E> entities = loadEntities(from, pageSize);

        if (entities == null || entities.isEmpty()) {

            return Collections.emptyList();
        }

        return entities.stream().filter(Objects::nonNull).map(this::transformEntity).filter(Objects::nonNull)
                .map(IBaseResource.class::cast).toList();
    }

    /**
     * Compatibility overload for HAPI versions that provide RequestDetails when
     * requesting bundle resources.
     */
    public List<IBaseResource> getResources(int fromIndex, int toIndex, RequestDetails requestDetails) {

        return getResources(fromIndex, toIndex);
    }

    /**
     * Returns the number of matching FHIR resources.
     *
     * The count is cached so the database count query is executed only once for
     * this bundle provider instance.
     */
    @Override
    public Integer size() {

        if (cachedTotalSize == null) {

            long total = countEntities();

            if (total < 0) {
                throw new IllegalStateException("Search result count must not be negative");
            }

            cachedTotalSize = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.toIntExact(total);
        }

        return cachedTotalSize;
    }

    /**
     * Returns the preferred result page size.
     */
    @Override
    public Integer preferredPageSize() {
        return preferredPageSize;
    }

    /**
     * Declares which page of results this provider is serving.
     *
     * <p>
     * HAPI builds the {@code next} and {@code previous} links from these, so a
     * provider that does not report them hands the client links it will not honour.
     * Passing a null offset leaves the provider in its unpaged mode.
     *
     * @param offset zero-based offset requested with {@code _offset}
     * @param count  page size requested with {@code _count}
     */
    @Override
    public void setCurrentPage(Integer offset, Integer count) {
        if (offset == null) {
            currentPageOffset = null;
            currentPageSize = null;
            return;
        }
        if (offset < 0) {
            throw new IllegalArgumentException("_offset must be zero or greater");
        }
        int size = count == null || count <= 0 ? preferredPageSize : count;
        currentPageOffset = offset;
        currentPageSize = size;
    }

    @Override
    public Integer getCurrentPageOffset() {
        return currentPageOffset;
    }

    @Override
    public Integer getCurrentPageSize() {
        return currentPageSize;
    }

    /**
     * First row a subclass should load for the range HAPI asked for.
     *
     * <p>
     * Subclasses override {@code getResources} to attach includes, and must resolve
     * the range through this and {@link #effectivePageSize(int, int)} rather than
     * subtracting the arguments: in offset mode HAPI asks for everything and
     * expects the provider to have applied {@code _offset} and {@code _count}.
     */
    protected int effectiveOffset(int fromIndex) {
        return currentPageOffset == null ? fromIndex : currentPageOffset;
    }

    /**
     * Number of rows a subclass should load for the range HAPI asked for.
     *
     * @see #effectiveOffset(int)
     */
    protected int effectivePageSize(int fromIndex, int toIndex) {
        return currentPageOffset == null ? toIndex - fromIndex : currentPageSize;
    }

    /**
     * Sets the preferred page size returned to HAPI FHIR.
     *
     * @param preferredPageSize page size greater than zero
     */
    public void setPreferredPageSize(int preferredPageSize) {

        if (preferredPageSize <= 0) {
            throw new IllegalArgumentException("Preferred page size must be greater than zero");
        }

        this.preferredPageSize = preferredPageSize;
    }

    /**
     * Returns the time at which this search result provider was created.
     */

    @Override
    public IPrimitiveType<Date> getPublished() {
        return new InstantType(new Date(publishedDate.getTime()));
    }

    /**
     * Returns a stable identifier for this search operation.
     */
    @Override
    public String getUuid() {
        return searchUuid;
    }

    /**
     * Returns true when the current search has no matching results.
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Clears the cached result count.
     *
     * This can be used by subclasses when the underlying search data changes.
     */
    protected void clearCachedSize() {
        cachedTotalSize = null;
    }

    private void validateRange(int fromIndex, int toIndex) {

        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex must be zero or greater");
        }

        if (toIndex < fromIndex) {
            throw new IllegalArgumentException("toIndex must be greater than or equal to fromIndex");
        }
    }

    /**
     * Reads the configured default page size while protecting application startup
     * from a missing or invalid configuration value.
     */
    private static int resolveDefaultPageSize() {

        try {
            int configuredPageSize = Integer
                    .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

            if (configuredPageSize <= 0) {
                return (Integer) null;
            }

            return configuredPageSize;

        } catch (RuntimeException exception) {
            throw new RuntimeException("Failed to read default page size from configuration", exception);
        }
    }
}
