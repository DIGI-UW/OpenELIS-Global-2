/**
 * The shell's React contexts (`openelis_core/contexts`): the signed-in user
 * with roles and permissions, the configuration properties and the toast
 * notifications. A module reads them with `useContext`; the `sdk` export wraps
 * the common cases as hooks.
 */
export { default as UserSessionDetailsContext } from "../UserSessionDetailsContext";
export {
  ConfigurationContext,
  NotificationContext,
} from "../components/layout/contexts";
