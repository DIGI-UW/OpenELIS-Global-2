import { useContext, useEffect, useState } from "react";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import {
  Button,
  Column,
  Grid,
  Heading,
  Section,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import AutoComplete from "../common/AutoComplete";

export default function PushNotificationPage() {
  const [data, setData] = useState({
    message: "",
  });

  const intl = useIntl();

  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [userId, setUserId] = useState(undefined);
  const [error, setError] = useState("");

  const [users, setUsers] = useState([]);

  const submit = () => {
    if (data.message.trim() !== "" && userId !== undefined) {
      postToOpenElisServer(
        `/rest/notification/${userId}`,
        JSON.stringify(data),
        (_) => {
          addNotification({
            title: intl.formatMessage({
              id: "notification.title",
            }),
            message: intl.formatMessage({
              id: `notify.user.success.notification`,
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setData({ message: "" });
          setError("");
        },
      );
    } else {
      setError(intl.formatMessage({ id: "notify.error" }));
    }
  };

  const getUsers = async () => {
    try {
      getFromOpenElisServer("/rest/systemusers", (data) => {
        setUsers(data);
      });
    } catch (error) {
      console.error("Error getting users:", error);
    }
  };

  useEffect(() => {
    getUsers();
  }, []);

  const handleUserAutoCompleteChange = (e) => {
    // setData({ ...data, userId: parseInt(e.target.value) });
    setUserId(e.target.value);
  };

  const handleUserAutoCompleteSelect = (id) => {
    // setData({ ...data, userId: parseInt(id) });
    setUserId(id);
  };

  return (
    <div className="adminPageContent">
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
          {
            label: "notify.main.title",
            link: "/MasterListsPage#NotifyUser",
          },
        ]}
      />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="notify.main.title" />
            </Heading>
          </Section>
        </Column>
      </Grid>

      <div className="orderLegendBody">
        <Column lg={8} md={4} sm={4}>
          <TextArea
            type="text"
            labelText={intl.formatMessage({ id: "notify.message" })}
            name="message"
            id="message"
            required
            value={data.message}
            onChange={(e) => setData({ ...data, message: e.target.value })}
            style={{
              padding: "10px",
              boxSizing: "border-box",
              border: "1px solid #ccc",
              borderRadius: "4px",
              marginBottom: "20px",
            }}
          />
        </Column>
        <div style={{ marginBottom: "20px" }}>
          <label
            htmlFor="user"
            style={{ display: "block", marginBottom: "5px" }}
          >
            <FormattedMessage id="notify.user.by" />
            <span style={{ color: "red", marginLeft: "5px" }}>*</span>
          </label>
          <Column lg={8} md={4} sm={4}>
            <AutoComplete
              id="user"
              name="user"
              style={{ marginBottom: "20px" }}
              suggestions={users.map((user) => ({
                id: user.id,
                value: user.displayName,
              }))}
              onChange={handleUserAutoCompleteChange}
              onSelect={handleUserAutoCompleteSelect}
              required
            />
          </Column>
        </div>
        <br />
        {error !== "" && (
          <div style={{ color: "#c62828", margin: 4 }}>{error}</div>
        )}
        <Button onClick={submit} style={{ marginBottom: "20px" }}>
          Submit
        </Button>
      </div>
    </div>
  );
}
