import React, { useEffect, useState } from "react";
import { Accordion, AccordionItem, Loading, Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Effective-privilege summary for the selected roles (spec 012 T042).
 *
 * For each selected role id, fetches GET /rest/roles/{id}/privileges — the
 * resolved set including parent-role inheritance, with Global Administrator
 * expanded to the whole catalog — unions the results and renders them grouped
 * by category. Read-only by design: privilege definitions are managed by
 * migrations, the admin UI only assigns roles.
 */
function RolePrivilegesPanel({ selectedRoleIds }) {
  const [privilegesByCategory, setPrivilegesByCategory] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!selectedRoleIds || selectedRoleIds.length === 0) {
      setPrivilegesByCategory({});
      return;
    }
    let cancelled = false;
    setLoading(true);
    Promise.all(
      selectedRoleIds.map(
        (roleId) =>
          new Promise((resolve) => {
            getFromOpenElisServer(`/rest/roles/${roleId}/privileges`, (res) =>
              resolve(res || []),
            );
          }),
      ),
    ).then((results) => {
      if (cancelled) {
        return;
      }
      const byName = {};
      results.flat().forEach((privilege) => {
        if (privilege && privilege.name) {
          byName[privilege.name] = privilege;
        }
      });
      const grouped = {};
      Object.values(byName).forEach((privilege) => {
        const category = privilege.category || "other";
        if (!grouped[category]) {
          grouped[category] = [];
        }
        grouped[category].push(privilege);
      });
      Object.values(grouped).forEach((list) =>
        list.sort((a, b) => a.name.localeCompare(b.name)),
      );
      setPrivilegesByCategory(grouped);
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [JSON.stringify(selectedRoleIds)]);

  const categories = Object.keys(privilegesByCategory).sort();
  const total = categories.reduce(
    (sum, category) => sum + privilegesByCategory[category].length,
    0,
  );

  if (!selectedRoleIds || selectedRoleIds.length === 0) {
    return null;
  }

  return (
    <div data-testid="role-privileges-panel">
      <FormattedMessage id="systemuserrole.privileges.effective" />
      {" (" + total + ")"}
      <br />
      {loading ? (
        <Loading small withOverlay={false} />
      ) : (
        <Accordion size="sm">
          {categories.map((category) => (
            <AccordionItem
              key={category}
              title={
                category + " (" + privilegesByCategory[category].length + ")"
              }
            >
              {privilegesByCategory[category].map((privilege) => (
                <Tag
                  key={privilege.name}
                  type="gray"
                  size="sm"
                  title={privilege.description || privilege.name}
                >
                  {privilege.name}
                </Tag>
              ))}
            </AccordionItem>
          ))}
        </Accordion>
      )}
    </div>
  );
}

export default RolePrivilegesPanel;
