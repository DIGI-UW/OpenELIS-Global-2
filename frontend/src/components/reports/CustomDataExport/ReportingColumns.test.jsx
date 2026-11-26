import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import ReportingColumns from "./ReportingColumns";

// Reporting field labels arrive from the server in English. They are resolved
// against reporting.field.* so a non-English locale does not see English column
// names, while database-derived fields keep the label the server sent.
const messages = {
  "reporting.group.sample": "Échantillon",
  "reporting.field.accessionNumber": "Numéro d'accession",
  "reporting.design.available": "Disponible",
  "reporting.design.findField": "Trouver",
  "reporting.design.searchPlaceholder": "Rechercher",
  "reporting.design.fieldCount": "{count} champs",
  "reporting.design.matchCount": "{count} correspondances",
  "reporting.design.columnViews": "Vues",
  "reporting.design.yourColumns": "Vos colonnes ({count})",
  "reporting.design.expansion": "Expansion",
  "reporting.design.add": "Ajouter",
  "reporting.design.added": "Ajouté",
  "reporting.design.addField": "Ajouter {field}",
  "reporting.design.addedField": "{field} ajouté",
  "reporting.design.addShown": "Tout ajouter",
  "reporting.design.removeShown": "Tout retirer",
  "reporting.design.clearSearch": "Effacer",
};

const fields = [
  {
    id: "accessionNumber",
    label: "Accession Number",
    type: "text",
    group: "sample",
  },
  // Built from the database at request time, so it has no catalog key.
  { id: "test:12", label: "Malaria", type: "text", group: "sample" },
];

// Groups start collapsed, so searching is how fields become visible.
function searchFor(query) {
  render(
    <IntlProvider locale="fr" defaultLocale="en" messages={messages}>
      <ReportingColumns fields={fields} selected={[]} onChange={() => {}} />
    </IntlProvider>,
  );
  fireEvent.change(screen.getByPlaceholderText("Rechercher"), {
    target: { value: query },
  });
}

test("a catalog field is found and shown by its translated label", () => {
  searchFor("accession");
  expect(screen.getByText("Numéro d'accession")).toBeInTheDocument();
  expect(screen.queryByText("Accession Number")).not.toBeInTheDocument();
});

test("a database-derived field keeps the label the server sent", () => {
  searchFor("malaria");
  expect(screen.getByText("Malaria")).toBeInTheDocument();
});
