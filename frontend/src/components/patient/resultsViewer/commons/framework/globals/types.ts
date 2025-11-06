import { LifeCycleFn } from "single-spa";

declare global {
  interface Window {
    /**
     * Easily copies a text from an element.
     * @param source The source element carrying the text.
     */
    copyText(source: HTMLElement): void;
    /**
     * Gets the OpenMRS SPA base path with a trailing slash.
     */
    getOpenmrsSpaBase(): string;
    /**
     * Starts the OpenMRS SPA application.
     * @param config The configuration to use for running.
     */
    initializeSpa(config: SpaConfig): void;
    /**
     * Gets the API base path.
     */
    openmrsBase: string;
    /**
     * Gets the SPA base path.
     */
    spaBase: string;
    /**
     * Gets the determined SPA environment.
     */
    spaEnv: SpaEnvironment;
    /**
     * Gets the published SPA version.
     */
    spaVersion?: string;
    /**
     * Gets a set of options from the import-map-overrides package.
     */
    importMapOverrides: {
      getCurrentPageMap: () => Promise<ImportMap>;
      addOverride(moduleName: string, url: string): void;
    };
    /**
     * Gets the installed modules, which are tuples consisting of the module's name and exports.
     */
    installedModules: Array<[string, any]>;
    /**
     * The remotes from Webpack Module Federation.
     */
    __remotes__: Record<string, string>;
  }
}

export type SpaEnvironment = "production" | "development" | "test";

export interface ImportMap {
  imports: Record<string, string>;
}

export interface Lifecycle {
  bootstrap: LifeCycleFn<any>;
  mount: LifeCycleFn<any>;
  unmount: LifeCycleFn<any>;
  update?: LifeCycleFn<any>;
}

export interface SpaConfig {
  /**
   * The base path or URL for the OpenMRS API / endpoints.
   */
  apiUrl: string;
  /**
   * The base path for the SPA root path.
   */
  spaPath: string;
  /**
   * The environment to use.
   * @default production
   */
  env?: SpaEnvironment;
  /**
   * URLs of configurations to load in the system.
   */
  configUrls?: Array<string>;
  /**
   * Defines if offline should be supported by installing a service worker.
   * @default true
   */
  offline?: boolean;
}

export interface ResourceLoader<T = any> {
  (): Promise<T>;
}

export interface ComponentDefinition {
  /**
   * The module/app that defines the component
   */
  appName: string;
  /**
   * Defines a function to use for actually loading the component's lifecycle.
   */
  load(): Promise<any>;
  /**
   * Defines the online support / properties of the component.
   */
  online?: boolean | object;
  /**
   * Defines the offline support / properties of the component.
   */
  offline?: boolean | object;
  /**
   * Defines the access privilege(s) required for this component, if any.
   * If more than one privilege is provided, the user must have all specified permissions.
   */
  privilege?: string | string[];
  /**
   * Defines resources that are loaded when the component should mount.
   */
  resources?: Record<string, ResourceLoader>;
}

export interface ExtensionDefinition extends ComponentDefinition {
  /** The name of the extension being registered */
  name: string;
  /** A slot to attach to */
  slot?: string;
  /** Slots to attach to */
  slots?: Array<string>;
  /** The meta data used for reflection by other components */
  meta?: Record<string, any>;
  /** Specifies the relative order in which the extension renders in a slot */
  order?: number;
  /** @deprecated A confusing way to specify the name of the extension */
  id?: string;
}

export interface PageDefinition extends ComponentDefinition {
  /**
   * The route of the page.
   */
  route: string;
  /**
   * The order in which to load the page. This determines DOM order.
   */
  order: number;
}
