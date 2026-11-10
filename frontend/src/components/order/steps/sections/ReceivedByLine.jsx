import React, { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { ComboBox, Link } from "@carbon/react";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../../../utils/Utils";

const fullName = (user) =>
  [user?.firstName, user?.lastName].filter(Boolean).join(" ").trim();

/**
 * Received by (FRS clinical order entry v4, FR-B23): recorded and required,
 * but in the usual case it is the person entering the samples, so it reads as
 * "Received by {name} (you)" with Change, which opens a user search. The chosen
 * receiver is copied to every sample.
 *
 * Props: samples, onChange({ id, name }), isReadOnly.
 */
const ReceivedByLine = ({ samples = [], onChange, isReadOnly }) => {
  const intl = useIntl();
  const { userSessionDetails = {} } =
    useContext(UserSessionDetailsContext) || {};
  const [changing, setChanging] = useState(false);
  const [search, setSearch] = useState("");
  const [users, setUsers] = useState([]);

  const recorded = samples.find((sample) => sample.receivedById);
  const currentUserId = String(userSessionDetails.userId || "");
  const receiverId = recorded ? String(recorded.receivedById) : currentUserId;
  const isYou = !recorded || receiverId === currentUserId;
  const receiverName = isYou
    ? fullName(userSessionDetails) || userSessionDetails.loginName || ""
    : recorded.receivedByName || receiverId;

  useEffect(() => {
    if (!changing) {
      return undefined;
    }
    let active = true;
    const timer = setTimeout(() => {
      getFromOpenElisServer(
        `/rest/nce/users${search.trim() ? `?search=${encodeURIComponent(search.trim())}` : ""}`,
        (response) => {
          if (active) setUsers(Array.isArray(response) ? response : []);
        },
      );
    }, 250);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [changing, search]);

  return (
    <div className="received-by-line" data-testid="received-by-line">
      <span data-testid="received-by-text">
        <FormattedMessage
          id={
            isYou
              ? "order.entry.receivedBy.you"
              : "order.entry.receivedBy.other"
          }
          values={{ name: receiverName }}
        />
      </span>{" "}
      {!isReadOnly && !changing && (
        <Link
          href="#"
          onClick={(event) => {
            event.preventDefault();
            setChanging(true);
          }}
          data-testid="received-by-change"
        >
          <FormattedMessage id="common.change" />
        </Link>
      )}
      {changing && (
        <ComboBox
          id="received-by-user"
          titleText={intl.formatMessage({
            id: "order.entry.receivedBy.search",
          })}
          items={users}
          itemToString={(user) =>
            user ? user.displayName || fullName(user) || user.loginName : ""
          }
          onInputChange={(text) => setSearch(text || "")}
          onChange={({ selectedItem }) => {
            if (!selectedItem) {
              return;
            }
            onChange({
              id: String(selectedItem.id),
              name: selectedItem.displayName || fullName(selectedItem),
            });
            setChanging(false);
          }}
          placeholder={intl.formatMessage({
            id: "order.entry.receivedBy.search",
          })}
        />
      )}
    </div>
  );
};

export default ReceivedByLine;
