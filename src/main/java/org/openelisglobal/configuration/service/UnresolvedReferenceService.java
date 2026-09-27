package org.openelisglobal.configuration.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.configuration.valueholder.UnresolvedReference;

public interface UnresolvedReferenceService extends BaseObjectService<UnresolvedReference, String> {

    /**
     * Writes the references the resolver noticed since the last flush (see
     * {@link ImportRunContext}) as open decision items of the current import run,
     * folding repeats of the same kind and spelling into the existing open item.
     */
    void recordPending(String domain, String fileName, int lineNumber);

    List<UnresolvedReference> getOpen();

    /**
     * Closes the open items whose name the catalog can now resolve - a specimen a
     * later file created, a lab unit someone added since. Called at the end of a
     * load, so the queue holds only what still needs a person. Its own lookups
     * queue nothing: a name that still does not resolve keeps the item it already
     * has.
     */
    void closeResolvable();

    /**
     * Closes an item: {@code USE_EXISTING} and {@code ALIAS} point it at
     * {@code targetId} (ALIAS also remembers the spelling for later imports),
     * {@code SKIP} drops it. Returns the closed item.
     */
    UnresolvedReference resolve(String id, String resolution, String targetId, String sysUserId);
}
