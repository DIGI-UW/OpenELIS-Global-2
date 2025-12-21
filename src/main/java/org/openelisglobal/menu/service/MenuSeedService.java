package org.openelisglobal.menu.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.menu.valueholder.MenuSeedConfig;
import org.openelisglobal.menu.valueholder.MenuSeedConfig.MenuItemDTO;

/**
 * Service interface for seeding menu entries from a configuration file at
 * application startup. This allows implementations to ship OpenELIS with a
 * predefined menu structure without requiring manual UI configuration or
 * Liquibase changesets.
 */
public interface MenuSeedService {
    /**
     * Seeds menus from configuration file at application startup. This method is
     * called automatically after bean construction via @PostConstruct.
     *
     * <p>
     * The seeding process:
     *
     * <ol>
     * <li>Checks if seeding is enabled
     * <li>Reads and parses the menu configuration file
     * <li>Creates menu entries that don't exist (by elementId)
     * <li>Preserves existing menu entries
     * <li>Forces menu cache rebuild
     * </ol>
     */
    void seedMenusFromConfig();

    /**
     * Parses the menu configuration file into a MenuSeedConfig object.
     *
     * @param configFile The JSON configuration file
     * @return Parsed menu configuration
     * @throws IOException If file cannot be read or parsed
     */
    MenuSeedConfig parseMenuConfig(File configFile) throws IOException;

    /**
     * Recursively seeds menus from the configuration. Creates menu entries that
     * don't exist, preserving existing ones.
     *
     * @param menuItemDTOs List of menu item definitions to seed
     * @param parentMenu   Parent menu for hierarchical structure (null for root
     *                     menus)
     * @return Count of menus created
     */
    int seedMenus(List<MenuItemDTO> menuItemDTOs, Menu parentMenu);

    /**
     * Creates a Menu entity from a MenuItemDTO.
     *
     * @param dto        The menu item DTO from configuration
     * @param parentMenu The parent menu (null for root menus)
     * @return Populated Menu entity ready for persistence
     */
    Menu createMenuFromDTO(MenuItemDTO dto, Menu parentMenu);
}
