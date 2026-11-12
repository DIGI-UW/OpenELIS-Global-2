import { fireEvent, screen } from "@testing-library/react";
import { getTextMacros } from "../../common/textMacro/TextMacroService";

export const expandedPhrase = (context) =>
  new RegExp(`^Seen Phrase for ${context} ?$`);

export const offerOnePhrasePerContext = () =>
  getTextMacros.mockImplementation((context) =>
    Promise.resolve([
      {
        id: context,
        code: ".pf",
        expansionText: `Phrase for ${context}`,
        category: "CULTURE",
        contexts: [context],
      },
    ]),
  );

export const typePhraseCode = async (field) => {
  fireEvent.focusIn(field);
  fireEvent.change(field, { target: { value: "Seen .pf" } });
  await screen.findByRole("option", { name: /\.pf/ });
  fireEvent.keyDown(field, { key: " " });
};
