import {
  Column,
  Grid,
  StructuredListBody,
  StructuredListCell,
  StructuredListHead,
  StructuredListRow,
  StructuredListWrapper,
} from "@carbon/react";

export const CustomShowGuide = ({ rows }) => {
  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <hr />
        <StructuredListWrapper ariaLabel="Structured list">
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>Field</StructuredListCell>
              <StructuredListCell head>Description</StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {rows.map((row) => (
              <StructuredListRow key={row.id}>
                <StructuredListCell>{row.field}</StructuredListCell>
                <StructuredListCell>{row.description}</StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
        <hr />
      </Column>
    </Grid>
  );
};
