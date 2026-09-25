import React from "react";
import { Button, Link } from "@carbon/react";
import { ArrowLeft, ArrowRight } from "@carbon/react/icons";

interface ServerPageArrowsProps {
  currentPage: number | string | null;
  totalPages: number | string | null;
  onPrevious: () => void;
  onNext: () => void;
  previousDisabled: boolean;
  nextDisabled: boolean;
}

/**
 * The server page arrows shown above a paged table. They sit in the same
 * full-width column as the table and are pushed to its right edge, so Next
 * lines up with the table's own edge and with the Carbon Pagination below.
 */
const ServerPageArrows: React.FC<ServerPageArrowsProps> = ({
  currentPage,
  totalPages,
  onPrevious,
  onNext,
  previousDisabled,
  nextDisabled,
}) => (
  <div
    style={{
      display: "flex",
      justifyContent: "flex-end",
      alignItems: "center",
      gap: "0.5rem",
      width: "100%",
    }}
  >
    <Link>{`${currentPage} / ${totalPages}`}</Link>
    <Button
      hasIconOnly
      id="loadpreviousresults"
      onClick={onPrevious}
      disabled={previousDisabled}
      renderIcon={ArrowLeft}
      iconDescription="previous"
    />
    <Button
      hasIconOnly
      id="loadnextresults"
      onClick={onNext}
      disabled={nextDisabled}
      renderIcon={ArrowRight}
      iconDescription="next"
    />
  </div>
);

export default ServerPageArrows;
