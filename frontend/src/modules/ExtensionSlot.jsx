import React, { useContext } from "react";
import UserSessionDetailsContext from "../UserSessionDetailsContext";
import { hasPermission } from "../components/utils/Utils";
import LazyRemote from "./LazyRemote";
import { useModuleHost } from "./ModuleHostProvider";

/**
 * A named place in a core page where modules may render. The core renders
 * `<ExtensionSlot name="patient.header.actions" context={{ patientId }} />`;
 * every started module that declares a contribution to that slot (and whose
 * permission the user holds) renders there, in manifest order, each inside its
 * own error boundary so one broken contribution never hides the others or the
 * page. Renders nothing when no module contributes.
 */
export default function ExtensionSlot({
  name,
  context = {},
  wrapper: Wrapper,
}) {
  const host = useModuleHost();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const contributions = (host.slots[name] || []).filter(
    (contribution) =>
      !contribution.permission ||
      hasPermission(userSessionDetails, contribution.permission),
  );
  if (contributions.length === 0) {
    return null;
  }
  const items = contributions.map((contribution) => (
    <LazyRemote
      key={`${contribution.moduleId}:${contribution.expose}`}
      moduleId={contribution.moduleId}
      remoteName={contribution.remoteName}
      expose={contribution.expose}
      fallback={null}
      silent
      slot={name}
      {...context}
    />
  ));
  return Wrapper ? <Wrapper>{items}</Wrapper> : <>{items}</>;
}

/** The slots core pages offer today, with the context each passes. */
export const SLOTS = {
  "home.dashboard.cards": "{ user }",
  "patient.header.actions": "{ patientId }",
};
