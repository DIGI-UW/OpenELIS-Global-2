import React from "react";

interface FilterEmptyDataIllustrationProps {
  width?: string;
  height?: string;
}

const FilterEmptyDataIllustration: React.FC<
  FilterEmptyDataIllustrationProps
> = ({ width = "64", height = "64" }) => (
  <svg
    width={width}
    height={height}
    viewBox="0 0 58 58"
    xmlns="http://www.w3.org/2000/svg"
  >
    <g fill="none" fill-rule="evenodd">
      <g fill="#CEE6E5">
        <path d="M15 15h14v14H15zM43 43h14v14H43zM29 29h14v14H29zM29 1h14v14H29zM1 29h14v14H1zM43 15h14v14H43zM15 43h14v14H15zM1 1h14v14H1z" />
      </g>
      <path
        d="M57 57H1V1h56v56zM1 29h56M1 15h56M1 43h56M29 1v56M15 1v56M43 1v56"
        stroke="#9ACBCA"
        stroke-width="1.44"
        stroke-linejoin="round"
      />
    </g>
  </svg>
);

export default FilterEmptyDataIllustration;
