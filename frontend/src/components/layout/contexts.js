import { createContext } from "react";

/**
 * The two contexts LayoutProvider provides, declared apart from it.
 *
 * <p>Declaring them here lets a component LayoutProvider renders read one
 * without importing the provider back, which would be a cycle resolved only by
 * the timing of when the reference is dereferenced. Every consumer in the app
 * imports them from here.
 */
export const ConfigurationContext = createContext(null);
export const NotificationContext = createContext(null);
