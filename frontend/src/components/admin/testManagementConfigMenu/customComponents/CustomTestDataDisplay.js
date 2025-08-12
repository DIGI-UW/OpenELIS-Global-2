import {
  Grid,
  Section,
  Column,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Heading,
  Row,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";

export const CustomTestDataDisplay = ({ testToDisplay }) => {
  if (!testToDisplay) return null;

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Section>
          <Heading>
            <FormattedMessage id={`banner.menu.patientEdit`} />
          </Heading>
        </Section>
        <hr />
        <Section>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id={`test.modify.header.test`} />
                {" : "}
                {`${testToDisplay.localization.english} (${testToDisplay.sampleType})`}
              </Heading>
            </Section>
          </Section>
        </Section>
        <hr />
      </Column>
      <Column lg={8} md={8} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id={`field.name`} />
            </Section>
          </Section>
        </Section>
        <Row>
          <Column lg={4}>
            <Section>
              <Section>
                <Section>
                  <FormattedMessage id={`test.modify.en`} />
                  {" : "}
                  {`${testToDisplay.localization.english}`}
                </Section>
              </Section>
            </Section>
          </Column>
          <Column lg={4}>
            <Section>
              <Section>
                <Section>
                  <FormattedMessage id={`test.modify.fr`} />
                  {" : "}
                  {`${testToDisplay.localization.french}`}
                </Section>
              </Section>
            </Section>
          </Column>
        </Row>
      </Column>
      <Column lg={8} md={8} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id={`field.reportName`} />
            </Section>
          </Section>
        </Section>
        <Row>
          <Column lg={4}>
            <Section>
              <Section>
                <Section>
                  <FormattedMessage id={`test.modify.en`} />
                  {" : "}
                  {`${testToDisplay.reportLocalization.english}`}
                </Section>
              </Section>
            </Section>
          </Column>
          <Column lg={4}>
            <Section>
              <Section>
                <Section>
                  <FormattedMessage id={`test.modify.fr`} />
                  {" : "}
                  {`${testToDisplay.reportLocalization.french}`}
                </Section>
              </Section>
            </Section>
          </Column>
        </Row>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="test.field.active" /> :{" "}
              {String(testToDisplay.active)}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="label.orderable" /> :{" "}
              {String(testToDisplay.orderable)}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="test.notifyResults" /> :{" "}
              {String(testToDisplay.notifyResults)}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="test.inLabOnly" /> :{" "}
              {String(testToDisplay.inLabOnly)}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="field.testUnit" /> :{" "}
              {testToDisplay.testUnit}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="field.sampleType" /> :{" "}
              {testToDisplay.sampleType}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="field.panel" /> : {testToDisplay.panel}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="field.resultType" /> :{" "}
              {testToDisplay.resultType}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="field.uom" /> : {testToDisplay.uom}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="field.significantDigits" /> :{" "}
              {testToDisplay.significantDigits}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="field.loinc" />:{" "}
              {testToDisplay.loinc ?? null}
            </Section>
          </Section>
        </Section>
      </Column>

      {testToDisplay &&
        testToDisplay?.hasDictionaryValues &&
        testToDisplay?.dictionaryValues && (
          <>
            <br />
            <hr />
            <Column lg={8} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <FormattedMessage id="field.selectValues" /> :
                    <ul>
                      {testToDisplay.dictionaryValues.map((value, index) => (
                        <li key={index}>{value}</li>
                      ))}
                    </ul>
                  </Section>
                </Section>
              </Section>
            </Column>
            <Column lg={8} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <FormattedMessage id="field.referenceValue" /> :{" "}
                    {testToDisplay.referenceValue}
                  </Section>
                </Section>
              </Section>
            </Column>
          </>
        )}

      {testToDisplay &&
        testToDisplay?.hasLimitValues &&
        testToDisplay?.resultLimits?.length > 0 && (
          <Column lg={16} md={8} sm={4}>
            <>
              <br />
              <hr />
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="field.resultLimits" />
                    </Heading>
                  </Section>
                </Section>
                <TableContainer>
                  <Table size="sm">
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          <FormattedMessage id="field.sex" />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage id="field.ageRange" />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage id="field.normalRange" />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage id="field.validRange" />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage id="label.critical.range" />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage id="label.reporting.range" />
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {testToDisplay?.resultLimits.map((limit, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{limit.gender}</TableCell>
                          <TableCell>{limit.ageRange}</TableCell>
                          <TableCell>{limit.normalRange}</TableCell>
                          <TableCell>{limit.validRange}</TableCell>
                          <TableCell>{limit.criticalRange}</TableCell>
                          <TableCell>{limit.reportingRange}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Section>
            </>
          </Column>
        )}
    </Grid>
  );
};
