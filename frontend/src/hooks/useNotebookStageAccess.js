import { useCallback, useMemo, useEffect, useState } from "react";
import { usePermissions } from "./usePermissions";
import {
  NOTEBOOK_STAGE_ACTIONS,
  enrichPagesWithRegistryRoles,
  isActionPermitted,
  resolvePageAllowedRoles,
  resolvePageKey,
} from "../constants/ahriWorkflowRegistry";

/**
 * SRS notebook stage access: workflowType + pageKey + requiredAction + allowedPersonas.
 * UX only — backend enforces the same rules.
 */
export function useNotebookStageAccess(
  pages,
  defaultPages = [],
  initialActivePage = 0,
  options = {},
) {
  const { isCreating = false, workflowType = "" } = options;
  const { hasPersonaForActiveDepartment } = usePermissions();
  const [activePage, setActivePage] = useState(initialActivePage);

  const resolveRolesForPage = useCallback(
    (page, pageIndex, action = null) => {
      return resolvePageAllowedRoles(workflowType, {
        ...page,
        order: page?.order ?? page?.pageOrder ?? pageIndex + 1,
        pageOrder: page?.pageOrder ?? page?.order ?? pageIndex + 1,
        pageKey: page?.pageKey ?? resolvePageKey(page),
      }, action);
    },
    [workflowType],
  );

  const canPerformAction = useCallback(
    (page, pageIndex, action) => {
      if (!action || !isActionPermitted(workflowType, page, action)) {
        return false;
      }
      const roles = resolveRolesForPage(page, pageIndex, action);
      if (roles.length === 0) {
        return false;
      }
      return hasPersonaForActiveDepartment(roles);
    },
    [hasPersonaForActiveDepartment, resolveRolesForPage, workflowType],
  );

  const hasPageAccess = useCallback(
    (page, pageIndex) => {
      const stageOrder = page?.pageOrder ?? page?.order ?? pageIndex + 1;

      if (isCreating) {
        return stageOrder === 1 && canPerformAction(page, pageIndex, NOTEBOOK_STAGE_ACTIONS.VIEW);
      }

      return canPerformAction(page, pageIndex, NOTEBOOK_STAGE_ACTIONS.VIEW);
    },
    [canPerformAction, isCreating],
  );

  const canEditPage = useCallback(
    (page, pageIndex) => canPerformAction(page, pageIndex, NOTEBOOK_STAGE_ACTIONS.EDIT),
    [canPerformAction],
  );

  const canCompletePage = useCallback(
    (page, pageIndex) => canPerformAction(page, pageIndex, NOTEBOOK_STAGE_ACTIONS.COMPLETE),
    [canPerformAction],
  );

  const effectivePages = useMemo(() => {
    let pagesToUse = defaultPages;
    if (pages && pages.length > 0) {
      pagesToUse = pages;
    }

    const enriched = enrichPagesWithRegistryRoles(workflowType, pagesToUse);

    return enriched.map((page, index) => ({
      ...page,
      pageKey: page.pageKey ?? resolvePageKey(page),
      hasAccess: hasPageAccess(page, index),
      canEdit: canEditPage(page, index),
      canComplete: canCompletePage(page, index),
      requiredRoles: resolveRolesForPage(page, index, NOTEBOOK_STAGE_ACTIONS.EDIT),
    }));
  }, [
    pages,
    defaultPages,
    hasPageAccess,
    canEditPage,
    canCompletePage,
    workflowType,
    resolveRolesForPage,
  ]);

  useEffect(() => {
    if (effectivePages.length > 0) {
      const currentPage = effectivePages[activePage];
      if (!currentPage || !currentPage.hasAccess) {
        const firstAccessibleIndex = effectivePages.findIndex(
          (page) => page.hasAccess,
        );
        if (firstAccessibleIndex >= 0 && firstAccessibleIndex !== activePage) {
          setActivePage(firstAccessibleIndex);
        }
      }
    }
  }, [effectivePages, activePage]);

  const handlePageChange = useCallback(
    (pageIndex) => {
      if (effectivePages[pageIndex] && !effectivePages[pageIndex].hasAccess) {
        return;
      }
      setActivePage(pageIndex);
    },
    [effectivePages],
  );

  const hasAnyAccessiblePage = useMemo(() => {
    return effectivePages.some((page) => page.hasAccess);
  }, [effectivePages]);

  const firstAccessiblePageIndex = useMemo(() => {
    return effectivePages.findIndex((page) => page.hasAccess);
  }, [effectivePages]);

  return {
    effectivePages,
    activePage,
    setActivePage,
    handlePageChange,
    hasPageAccess,
    canPerformAction,
    canEditPage,
    canCompletePage,
    hasAnyAccessiblePage,
    firstAccessiblePageIndex,
    NOTEBOOK_STAGE_ACTIONS,
  };
}

export default useNotebookStageAccess;
