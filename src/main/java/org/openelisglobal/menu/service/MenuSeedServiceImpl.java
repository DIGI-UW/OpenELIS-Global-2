package org.openelisglobal.menu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.menu.valueholder.MenuSeedConfig;
import org.openelisglobal.menu.valueholder.MenuSeedConfig.MenuItemDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of MenuSeedService responsible for seeding menu entries from a
 * configuration file at application startup. This allows implementations to
 * ship OpenELIS with a predefined menu structure without requiring manual UI
 * configuration or Liquibase changesets.
 *
 * <p>
 * The service reads menu definitions from a JSON configuration file (default:
 * /var/lib/openelis-global/menu/menu_seed.json) and creates menu entries in the
 * database if they do not already exist. Existing menus are preserved, making
 * the seeding process idempotent and safe to run on application restart.
 *
 * <p>
 * Configuration properties:
 *
 * <ul>
 * <li>org.openelisglobal.menu.seed.enabled - Enable/disable menu seeding
 * (default: false)
 * <li>org.openelisglobal.menu.seed.config.path - Path to menu seed JSON file
 * </ul>
 */
@Service
public class MenuSeedServiceImpl implements MenuSeedService {

    private static final String DEFAULT_SEED_CONFIG_PATH = "/var/lib/openelis-global/menu/menu_seed.json";

    @Value("${org.openelisglobal.menu.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${org.openelisglobal.menu.seed.config.path:" + DEFAULT_SEED_CONFIG_PATH + "}")
    private String seedConfigPath;

    @Autowired
    private MenuService menuService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    @PostConstruct
    @Override
    public void seedMenusFromConfig() {
        if (!seedEnabled) {
            LogEvent.logDebug("MenuSeedServiceImpl", "seedMenusFromConfig",
                    "Menu seeding disabled (org.openelisglobal.menu.seed.enabled=false)");
            return;
        }

        try {
            LogEvent.logInfo("MenuSeedServiceImpl", "seedMenusFromConfig",
                    "Starting menu seeding from config: " + seedConfigPath);

            File configFile = new File(seedConfigPath);
            if (!configFile.exists() || !configFile.isFile()) {
                LogEvent.logWarn("MenuSeedServiceImpl", "seedMenusFromConfig",
                        "Menu seed config file not found: " + seedConfigPath + ". Skipping menu seeding.");
                return;
            }

            MenuSeedConfig config = parseMenuConfig(configFile);
            int createdCount = seedMenus(config.getMenus(), null);

            LogEvent.logInfo("MenuSeedServiceImpl", "seedMenusFromConfig",
                    "Menu seeding completed successfully: " + createdCount + " menus created");

            // Force rebuild menu cache to include newly seeded menus
            MenuUtil.forceRebuild();

        } catch (IOException e) {
            LogEvent.logError("MenuSeedServiceImpl", "seedMenusFromConfig",
                    "Error reading menu seed config file: " + e.getMessage());
            LogEvent.logError(e);
        } catch (Exception e) {
            LogEvent.logError("MenuSeedServiceImpl", "seedMenusFromConfig",
                    "Error seeding menus from config: " + e.getMessage());
            LogEvent.logError(e);
        }
    }

    /**
     * Parses the menu configuration file into a MenuSeedConfig object.
     *
     * @param configFile The JSON configuration file
     * @return Parsed menu configuration
     * @throws IOException If file cannot be read or parsed
     */
    @Override
    public MenuSeedConfig parseMenuConfig(File configFile) throws IOException {
        return objectMapper.readValue(configFile, MenuSeedConfig.class);
    }

    /**
     * Recursively seeds menus from the configuration. Creates menu entries that
     * don't exist, preserving existing ones.
     *
     * @param menuItemDTOs List of menu item definitions to seed
     * @param parentMenu   Parent menu for hierarchical structure (null for root
     *                     menus)
     * @return Count of menus created
     */
    @Override
    @Transactional
    public int seedMenus(List<MenuItemDTO> menuItemDTOs, Menu parentMenu) {
        int createdCount = 0;

        for (MenuItemDTO dto : menuItemDTOs) {
            if (GenericValidator.isBlankOrNull(dto.getElementId())) {
                LogEvent.logWarn("MenuSeedServiceImpl", "seedMenus", "Skipping menu with missing elementId");
                continue;
            }

            try {
                // Check if menu already exists
                Menu existingMenu = menuService.getMenuByElementId(dto.getElementId());

                if (existingMenu != null) {
                    LogEvent.logDebug("MenuSeedServiceImpl", "seedMenus",
                            "Menu already exists, skipping: " + dto.getElementId());

                    // Still process children with existing menu as parent
                    if (dto.getChildMenus() != null && !dto.getChildMenus().isEmpty()) {
                        createdCount += seedMenus(dto.getChildMenus(), existingMenu);
                    }
                    continue;
                }

                // Create new menu
                Menu newMenu = createMenuFromDTO(dto, parentMenu);
                menuService.insert(newMenu);
                createdCount++;

                LogEvent.logInfo("MenuSeedServiceImpl", "seedMenus", "Created menu: " + dto.getElementId()
                        + " (parent: " + (parentMenu != null ? parentMenu.getElementId() : "null") + ")");

                // Recursively seed child menus
                if (dto.getChildMenus() != null && !dto.getChildMenus().isEmpty()) {
                    createdCount += seedMenus(dto.getChildMenus(), newMenu);
                }

            } catch (Exception e) {
                LogEvent.logError("MenuSeedServiceImpl", "seedMenus",
                        "Error creating menu " + dto.getElementId() + ": " + e.getMessage());
                LogEvent.logError(e);
            }
        }

        return createdCount;
    }

    /**
     * Creates a Menu entity from a MenuItemDTO.
     *
     * @param dto        The menu item DTO from configuration
     * @param parentMenu The parent menu (null for root menus)
     * @return Populated Menu entity ready for persistence
     */
    @Override
    public Menu createMenuFromDTO(MenuItemDTO dto, Menu parentMenu) {
        Menu menu = new Menu();

        menu.setElementId(dto.getElementId());
        menu.setDisplayKey(dto.getDisplayKey());
        menu.setToolTipKey(dto.getToolTipKey());
        menu.setActionURL(dto.getActionURL());
        menu.setClickAction(dto.getClickAction());

        menu.setPresentationOrder(dto.getPresentationOrder() != null ? dto.getPresentationOrder() : 0);
        menu.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        menu.setOpenInNewWindow(dto.getOpenInNewWindow() != null ? dto.getOpenInNewWindow() : false);
        menu.setHideInOldUI(dto.getHideInOldUI() != null ? dto.getHideInOldUI() : false);

        if (parentMenu != null) {
            menu.setParent(parentMenu);
        }

        return menu;
    }
}
