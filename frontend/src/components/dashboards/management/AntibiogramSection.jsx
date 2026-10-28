import React from "react";
import {
  SkeletonText,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ServerDataState from "../../utils/ServerDataState";
import { useServerData } from "../../utils/useServerData";
import { antibiogramUrl } from "./managementApi";
import "../../qa/common/QAStyles.css";
import "./AntibiogramSection.css";

const band = (pctR) => {
  if (pctR < 20) return "green";
  if (pctR < 40) return "amber";
  if (pctR < 60) return "orange";
  return "red";
};

const AntibiogramSection = ({ scope, features }) => {
  const intl = useIntl();
  const query = useServerData(
    features?.microbiology ? antibiogramUrl(scope) : null,
  );

  if (!features?.microbiology) return null;

  const data = query.data;
  const organismLabel = (organism) =>
    intl.formatMessage(
      { id: "label.mgmt.antibiogram.organism" },
      { 0: organism.name, 1: intl.formatNumber(organism.isolates) },
    );

  const body = () => {
    if (query.isError) return <ServerDataState query={query} />;
    if (!data) return <SkeletonText paragraph lineCount={4} />;
    if (data.organisms.length === 0) {
      return (
        <p className="mgmt-antibiogram__note">
          <FormattedMessage id="label.mgmt.antibiogram.none" />
        </p>
      );
    }
    if (data.cells.length === 0) {
      return (
        <>
          <p className="mgmt-antibiogram__note">
            <FormattedMessage id="label.mgmt.antibiogram.fallback" />
          </p>
          <ul className="mgmt-antibiogram__counts">
            {data.organisms.map((organism) => (
              <li key={organism.id}>{organismLabel(organism)}</li>
            ))}
          </ul>
        </>
      );
    }
    const cells = new Map(
      data.cells.map((cell) => [
        `${cell.organismId}|${cell.antibioticId}`,
        cell,
      ]),
    );
    return (
      <div className="mgmt-antibiogram__scroll">
        <Table size="sm" useZebraStyles={false}>
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="label.mgmt.antibiogram.organismHeader" />
              </TableHeader>
              {data.antibiotics.map((antibiotic) => (
                <TableHeader key={antibiotic.id}>{antibiotic.name}</TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {data.organisms.map((organism) => (
              <TableRow key={organism.id}>
                <TableCell>{organismLabel(organism)}</TableCell>
                {data.antibiotics.map((antibiotic) => {
                  const cell = cells.get(`${organism.id}|${antibiotic.id}`);
                  if (!cell || cell.pctR == null) {
                    return (
                      <TableCell
                        key={antibiotic.id}
                        className="mgmt-antibiogram__cell"
                        title={
                          cell
                            ? intl.formatMessage(
                                { id: "label.mgmt.antibiogram.tooFew" },
                                { 0: cell.n },
                              )
                            : undefined
                        }
                      >
                        —
                      </TableCell>
                    );
                  }
                  return (
                    <TableCell
                      key={antibiotic.id}
                      className={`mgmt-antibiogram__cell mgmt-antibiogram__cell--${band(
                        Math.round(cell.pctR),
                      )}`}
                    >
                      <div>
                        {intl.formatNumber(cell.pctR / 100, {
                          style: "percent",
                          maximumFractionDigits: 0,
                        })}
                      </div>
                      <div className="mgmt-antibiogram__n">
                        <FormattedMessage
                          id="label.mgmt.antibiogram.tested"
                          values={{ 0: cell.n }}
                        />
                      </div>
                    </TableCell>
                  );
                })}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    );
  };

  return (
    <section className="mgmt-antibiogram" data-testid="mgmt-antibiogram">
      <Tile>
        <h4>
          <FormattedMessage id="label.mgmt.antibiogram.title" />
        </h4>
        <p className="mgmt-antibiogram__note">
          <FormattedMessage id="label.mgmt.antibiogram.meta" />
        </p>
        {body()}
      </Tile>
    </section>
  );
};

export default AntibiogramSection;
