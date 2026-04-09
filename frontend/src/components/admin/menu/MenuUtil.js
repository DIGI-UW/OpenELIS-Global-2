import React from "react";
import { Checkbox } from "@carbon/react";
import { useIntl } from "react-intl";

export const updateMenuWithElementId = (newMenus, field, value) => {};

export const MenuCheckBox = (props) => {
  const intl = useIntl();

  const setMenuIsActiveToValueIncludeChildren = (value, currentMenuItem) => {
    let newCurrentMenuItem = { ...currentMenuItem };
    if (newCurrentMenuItem.childMenus) {
      newCurrentMenuItem.childMenus = newCurrentMenuItem.childMenus.map(
        (childMenuItem) => {
          return setMenuIsActiveToValueIncludeChildren(value, childMenuItem);
        },
      );
    }
    newCurrentMenuItem.menu.isActive = value;
    return newCurrentMenuItem;
  };

  const createCheckBoxForMenuItem = (
    menuItem,
    curMenuItem,
    setMenuItem,
    path,
    labelKey,
    recurse,
  ) => {
    if (!curMenuItem?.menu.elementId) {
      return <></>;
    }
    let depth = path.match(/\./g) ? path.match(/\./g).length : 0;
    return (
      <>
        <div
          className="formInlineDiv"
          style={{ marginLeft: 2 * depth + "rem" }}
        >
          <Checkbox
            id={curMenuItem?.menu.elementId + "_checkbox"}
            labelText={intl.formatMessage({
              id: labelKey
                ? labelKey
                : curMenuItem.menu.displayKey
                  ? curMenuItem.menu.displayKey
                  : "missing display key",
            })}
            disabled={curMenuItem.menu.elementId === "menu_sidenav"}
            checked={curMenuItem?.menu.isActive}
            onChange={(_, { checked }) => {
              if (path === "$" || !path) {
                if (curMenuItem.menu.elementId !== "menu_sidenav") {
                  setMenuItem({
                    ...setMenuIsActiveToValueIncludeChildren(
                      checked,
                      curMenuItem,
                    ),
                  });
                }
              } else {
                let newMenuItem = { ...menuItem };
                var jp = require("jsonpath");
                jp.value(
                  newMenuItem,
                  path,
                  setMenuIsActiveToValueIncludeChildren(checked, curMenuItem),
                );
                setMenuItem(newMenuItem);
              }
            }}
          />
        </div>
        {recurse &&
          curMenuItem?.childMenus?.map((childMenuItem, index) => {
            return (
              <React.Fragment key={childMenuItem.menu.elementId}>
                {createCheckBoxForMenuItem(
                  menuItem,
                  childMenuItem,
                  setMenuItem,
                  path + ".childMenus[" + index + "]",
                  childMenuItem.menu.displayKey,
                  true,
                )}
              </React.Fragment>
            );
          })}
      </>
    );
  };
  return (
    <>
      {createCheckBoxForMenuItem(
        props.menuItem,
        props.menuItem,
        props.setMenuItem,
        props.path,
        props.labelKey,
        props.recurse,
      )}
    </>
  );
};
