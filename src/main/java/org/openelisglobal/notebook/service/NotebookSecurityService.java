package org.openelisglobal.notebook.service;

import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.test.valueholder.TestSection;

/**
 * Service for handling notebook access control based on location and roles.
 *
 * Access Control Rules: - Only admins can create/edit/delete notebook templates
 * - Users can only see templates assigned to their organization (via
 * loginLabUnit) - Entry creation requires the user to have an allowed role for
 * their lab unit - Entries inherit accessible organizations from the template -
 * Entry's primary organization is immutable after creation
 */
public interface NotebookSecurityService {

    // ========== TEMPLATE ACCESS (Admin Only for Edit) ==========

    /**
     * Check if user can edit notebook templates (admin only).
     *
     * @param sysUserId the system user ID
     * @return true if user has admin privileges
     */
    boolean canEditTemplate(String sysUserId);

    /**
     * Check if user can view a specific notebook template. User must have access to
     * at least one of the template's organizations.
     *
     * @param template     the notebook template
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can view the template
     */
    boolean canViewTemplate(NoteBook template, String sysUserId, String loginLabUnit);

    /**
     * Check if user can view a specific notebook template by ID.
     *
     * @param notebookId   the notebook template ID
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can view the template
     */
    boolean canViewTemplate(Integer notebookId, String sysUserId, String loginLabUnit);

    // ========== ENTRY ACCESS (Role + Location Based) ==========

    /**
     * Check if user can create an entry from a template. User must have access to
     * the template AND have an allowed role.
     *
     * @param template     the notebook template
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can create entries from this template
     */
    boolean canCreateEntry(NoteBook template, String sysUserId, String loginLabUnit);

    /**
     * Check if user can create an entry from a template by ID.
     *
     * @param notebookId   the notebook template ID
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can create entries from this template
     */
    boolean canCreateEntry(Integer notebookId, String sysUserId, String loginLabUnit);

    /**
     * Check if user can view a specific notebook entry. User must be admin OR have
     * access to one of the entry's organizations.
     *
     * @param entry        the notebook entry
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can view the entry
     */
    boolean canViewEntry(NotebookEntry entry, String sysUserId, String loginLabUnit);

    /**
     * Check if user can view a specific notebook entry by ID.
     *
     * @param entryId      the notebook entry ID
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can view the entry
     */
    boolean canViewEntry(Integer entryId, String sysUserId, String loginLabUnit);

    /**
     * Check if user can edit a specific notebook entry. User must be able to view
     * AND have an allowed role.
     *
     * @param entry        the notebook entry
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can edit the entry
     */
    boolean canEditEntry(NotebookEntry entry, String sysUserId, String loginLabUnit);

    /**
     * Check if user can edit a specific notebook entry by ID.
     *
     * @param entryId      the notebook entry ID
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can edit the entry
     */
    boolean canEditEntry(Integer entryId, String sysUserId, String loginLabUnit);

    // ========== PAGE ACCESS (Role Based) ==========

    /**
     * Check if user can view a specific notebook page. User must have one of the
     * page's allowed roles, or the page has no role restrictions.
     *
     * @param page         the notebook page
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can view the page
     */
    boolean canViewPage(NoteBookPage page, String sysUserId, String loginLabUnit);

    /**
     * Check if user can view a specific notebook page by ID.
     *
     * @param pageId       the notebook page ID
     * @param sysUserId    the system user ID
     * @param loginLabUnit the user's login lab unit
     * @return true if user can view the page
     */
    boolean canViewPage(Integer pageId, String sysUserId, String loginLabUnit);

    // ========== HELPER METHODS ==========

    /**
     * Check if user has global admin role.
     *
     * @param sysUserId the system user ID
     * @return true if user is a global administrator
     */
    boolean hasGlobalAdminRole(String sysUserId);

    /**
     * Get the organization that matches the login lab unit.
     *
     * @param loginLabUnit the user's login lab unit
     * @return the matching organization, or null if not found
     */
    Organization getOrganizationForLoginLabUnit(String loginLabUnit);

    /**
     * Check if an organization matches the login lab unit.
     *
     * @param organization the organization to check
     * @param loginLabUnit the user's login lab unit
     * @return true if the organization matches
     */
    boolean matchesLoginLabUnit(Organization organization, String loginLabUnit);

    /**
     * Get the department (TestSection) that matches the login lab unit.
     *
     * @param loginLabUnit the user's login lab unit
     * @return the matching TestSection, or null if not found
     */
    TestSection getDepartmentForLoginLabUnit(String loginLabUnit);
}
