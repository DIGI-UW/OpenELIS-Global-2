import { createContext } from "react";

/**
 * The contexts Layout provides, declared apart from it.
 *
 * <p>Layout still creates nothing and still provides both — its own spec requires
 * that (FR-012) and the fifty-odd modules that import them from "./Layout" keep
 * working through the re-export there. Declaring them here lets a component
 * Layout itself renders read one of them without importing Layout back, which
 * would be a cycle resolved only by the timing of when the reference is
 * dereferenced.
 */
export const ConfigurationContext = createContext(null);
export const NotificationContext = createContext(null);
