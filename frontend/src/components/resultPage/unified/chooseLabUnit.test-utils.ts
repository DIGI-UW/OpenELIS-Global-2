import { fireEvent, screen } from "@testing-library/react";

/**
 * Chooses a lab unit on the Results toolbar the way a user does: open the
 * searchable Lab Unit box and click the unit by name (OGC-1515).
 */
export const chooseLabUnit = (name: string) => {
  fireEvent.click(screen.getByRole("combobox", { name: /Lab Unit/i }));
  fireEvent.click(screen.getByRole("option", { name }));
};
