import React, { useState } from "react";
import {
  Accordion,
  AccordionItem,
  Button,
  Column,
  Form,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  Section,
  Select,
  SelectItem,
  Stack,
  Toggle,
} from "@carbon/react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useIntl } from "react-intl";
import { postToOpenElisServerFullResponse } from "../../utils/Utils";
import { serverQuery } from "../../utils/queryClient";
import { navigationIcons } from "../../layout/navigationIcons";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import "./GlobalMenuManagement.scss";

const queryKey = ["navigation", "administration"];
const endpoint = "/rest/admin/menu";
const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.menu.global",
    link: "/MasterListsPage/globalMenuManagement",
  },
];
const iconLabels = {
  home: "home.label",
  order: "sidenav.workflow.orderTest",
  results: "banner.menu.results.unified",
  validation: "sidenav.workflow.resultsValidation",
  patient: "banner.menu.patient",
  reports: "sidenav.label.reports",
  settings: "sidenav.label.admin",
  workplan: "sidenav.label.workplan",
  more: "sidenav.moreTools",
};

const saveMenus = (items) =>
  new Promise((resolve, reject) => {
    postToOpenElisServerFullResponse(
      endpoint,
      JSON.stringify(items),
      async (response) => {
        if (!response?.ok) {
          reject(new Error("common.saveFailed"));
          return;
        }
        try {
          resolve(await response.json());
        } catch (error) {
          reject(error);
        }
      },
    );
  });

function MenuSettings({ item, drafts, onChange, saving, label }) {
  const { menu, childMenus = [] } = item;
  const current = drafts[menu.elementId] || menu;
  const controlled = (field) =>
    menu.configurationOnly || menu.configurationFields?.includes(field);
  const note = (field) =>
    controlled(field) ? label("menu.configuration.managed") : undefined;
  const iconNames = Object.keys(navigationIcons);
  const Icon = navigationIcons[current.icon];
  return (
    <AccordionItem
      title={label(menu.displayKey || menu.elementId)}
      data-testid={`menu-settings-${menu.elementId}`}
    >
      <Stack gap={5}>
        {menu.configurationOnly && <p>{label("menu.configuration.managed")}</p>}
        {!menu.configurationOnly && (
          <div
            className="menu-settings-fields"
            data-testid={`menu-fields-${menu.elementId}`}
          >
            <Toggle
              id={`${menu.elementId}-active`}
              labelText={label("common.active")}
              toggled={current.isActive}
              disabled={saving || controlled("isActive")}
              onToggle={(value) => onChange(menu, "isActive", value)}
            />
            {note("isActive") && <p>{note("isActive")}</p>}
            <Select
              id={`${menu.elementId}-style`}
              labelText={label("menu.presentation.style")}
              value={current.presentationStyle || ""}
              disabled={saving || controlled("presentationStyle")}
              helperText={
                note("presentationStyle") ||
                (current.presentationStyle === "section"
                  ? label("menu.presentation.sectionHint")
                  : undefined)
              }
              onChange={(event) =>
                onChange(menu, "presentationStyle", event.target.value)
              }
            >
              <SelectItem value="" text={label("menu.presentation.item")} />
              <SelectItem
                value="section"
                text={label("menu.presentation.section")}
              />
            </Select>
            <Select
              id={`${menu.elementId}-icon`}
              labelText={label("common.icon")}
              value={current.icon || ""}
              disabled={
                saving ||
                controlled("icon") ||
                current.presentationStyle === "section"
              }
              helperText={
                note("icon") ||
                (current.presentationStyle === "section"
                  ? label("menu.presentation.sectionIconHint")
                  : undefined)
              }
              onChange={(event) => onChange(menu, "icon", event.target.value)}
            >
              <SelectItem value="" text={label("common.none")} />
              {current.icon && !iconNames.includes(current.icon) && (
                <SelectItem value={current.icon} text={current.icon} />
              )}
              {iconNames.map((name) => (
                <SelectItem
                  key={name}
                  value={name}
                  text={label(iconLabels[name])}
                />
              ))}
            </Select>
            {Icon && current.presentationStyle !== "section" && (
              <Icon size={24} aria-hidden="true" />
            )}
          </div>
        )}
        {childMenus.length > 0 && (
          <Accordion>
            {childMenus.map((child) => (
              <MenuSettings
                key={child.menu.elementId}
                item={child}
                drafts={drafts}
                onChange={onChange}
                saving={saving}
                label={label}
              />
            ))}
          </Accordion>
        )}
      </Stack>
    </AccordionItem>
  );
}

export default function GlobalMenuManagement() {
  const intl = useIntl();
  const label = (id) => intl.formatMessage({ id, defaultMessage: id });
  const queryClient = useQueryClient();
  const query = useQuery(serverQuery(queryKey, endpoint));
  const [drafts, setDrafts] = useState({});
  const mutation = useMutation({
    mutationFn: saveMenus,
    onSuccess: (items) => {
      queryClient.setQueryData(queryKey, items);
      setDrafts({});
    },
  });
  const change = (menu, field, value) => {
    mutation.reset();
    setDrafts((previous) => ({
      ...previous,
      [menu.elementId]: {
        ...(previous[menu.elementId] || menu),
        [field]: value,
      },
    }));
  };
  const submit = (event) => {
    event.preventDefault();
    mutation.mutate(
      Object.values(drafts).map((menu) => ({ menu, childMenus: [] })),
    );
  };
  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column sm={4} md={8} lg={16}>
          <Section>
            <Heading>{label("menu.global.title")}</Heading>
          </Section>
          <Form
            onSubmit={submit}
            className="menu-settings-form"
            aria-label={label("menu.global.title")}
          >
            <Stack gap={6}>
              <p>{label("menu.configuration.description")}</p>
              {query.isLoading && (
                <Loading
                  withOverlay={false}
                  description={label("common.loading")}
                />
              )}
              {query.isError && (
                <>
                  <InlineNotification
                    kind="error"
                    title={label("menu.configuration.loadError")}
                    hideCloseButton
                  />
                  <Button kind="tertiary" onClick={() => query.refetch()}>
                    {label("common.retry")}
                  </Button>
                </>
              )}
              {mutation.isError && (
                <InlineNotification
                  kind="error"
                  title={label("common.saveFailed")}
                  hideCloseButton
                />
              )}
              {mutation.isSuccess && (
                <InlineNotification
                  kind="success"
                  title={label("menu.configuration.saved")}
                  hideCloseButton
                />
              )}
              {query.data && (
                <Accordion>
                  {query.data.map((item) => (
                    <MenuSettings
                      key={item.menu.elementId}
                      item={item}
                      drafts={drafts}
                      onChange={change}
                      saving={mutation.isLoading}
                      label={label}
                    />
                  ))}
                </Accordion>
              )}
              <Button
                type="submit"
                disabled={!Object.keys(drafts).length || mutation.isLoading}
              >
                {label("common.save")}
              </Button>
            </Stack>
          </Form>
        </Column>
      </Grid>
    </div>
  );
}
