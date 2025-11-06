import {
  Renew,
  NotificationFilled,
  Email,
  Filter,
  NotificationOff,
} from "@carbon/icons-react";
import {
  formatTimestamp,
  getFromOpenElisServerV2,
  postToOpenElisServer,
  putToOpenElisServer,
  urlBase64ToUint8Array,
  deleteToOpenElisServer,
} from "../utils/Utils";
import Spinner from "../common/Sprinner";
import { useIntl } from "react-intl";
import { useContext, useEffect, useState } from "react";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import NoNotificationSVG from "./NoNotificationSVG";

export default function SlideOverNotifications(props) {
  const intl = useIntl();
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const [iconLoading, setIconLoading] = useState({
    icon: null,
    loading: false,
  });

  const [subscriptionState, setSubscriptionState] = useState(null);

  useEffect(() => {
    // Whenever subscriptionState changes, re-check the subscription status

    intialSubscriptionState(); // Fetch the current subscription state again
  }, [subscriptionState]);

  const intialSubscriptionState = async () => {
    try {
      const res = await getFromOpenElisServerV2("/rest/notification/pnconfig");
      const reg = await navigator.serviceWorker.ready;
      const subscription = await reg.pushManager.getSubscription();
      if (!subscription && !res?.pf_endpoint) {
        setSubscriptionState("NotSubscribed");
        console.log("NotSubscribed");
      } else if (subscription?.endpoint === res?.pfEndpoint) {
        setSubscriptionState("SubscribedOnThisDevice");
        console.log("SubscribedOnThisDevice");
      } else {
        console.log("subscription?.endpoint", subscription?.endpoint);

        setSubscriptionState("SubscribedOnAnotherDevice");
        console.log("SubscribedOnAnotherDevice");
      }
    } catch (error) {
      console.error("Error checking subscription status:", error);
      setSubscriptionState("NotSubscribed");
    }
  };

  async function unsubscribe() {
    try {
      putToOpenElisServer("/rest/notification/unsubscribe", null, (res) => {
        addNotification({
          kind: "success",
          message: intl.formatMessage({
            id: "notification.slideover.button.unsubscribe.success",
          }),
          title: intl.formatMessage({ id: "notification.title" }),
        });
        setNotificationVisible(true);
        setSubscriptionState("NotSubscribed");
      });
    } catch (e) {
      addNotification({
        kind: "warning",
        message: intl.formatMessage({
          id: "notification.slideover.button.unsubscribe.fail",
        }),
        title: intl.formatMessage({ id: "notification.title" }),
      });
      setNotificationVisible(true);
    }
  }

  async function subscribe() {
    try {
      // Set the loading state
      setIconLoading({ icon: "NOTIFICATION", loading: true });

      // Check if service workers are supported
      if (!("serviceWorker" in navigator)) {
        throw new Error("Service workers are not supported in this browser.");
      }

      // Check if push messaging is supported
      if (!("PushManager" in window)) {
        throw new Error("Push messaging is not supported in this browser.");
      }

      // Register the service worker if not already registered
      const registration = await navigator.serviceWorker
        .register("/service-worker.js")
        .catch((error) => {
          throw new Error(
            "Service worker registration failed: " + error.message,
          );
        });

      // Ensure the service worker is ready
      const sw = await navigator.serviceWorker.ready;

      // Attempt to retrieve the public key from the server
      let pbKeyData = await getFromOpenElisServerV2(
        "/rest/notification/public_key",
      ).catch((error) => {
        throw new Error(
          "Failed to retrieve public key from server: " + error.message,
        );
      });

      // Convert the public key to a Uint8Array
      const applicationServerKey = urlBase64ToUint8Array(pbKeyData.publicKey);

      // Attempt to subscribe to push notifications
      const push = await sw.pushManager
        .subscribe({
          userVisibleOnly: true,
          applicationServerKey,
        })
        .catch((error) => {
          throw new Error("Push subscription failed: " + error.message);
        });

      // Encode the subscription keys
      const p256dh = btoa(
        String.fromCharCode.apply(null, new Uint8Array(push.getKey("p256dh"))),
      );
      const auth = btoa(
        String.fromCharCode.apply(null, new Uint8Array(push.getKey("auth"))),
      );

      // Construct the data object
      const data = {
        pfEndpoint: push.endpoint,
        pfP256dh: p256dh,
        pfAuth: auth,
      };

      // Send the subscription data to the server
      postToOpenElisServer(
        "/rest/notification/subscribe",
        JSON.stringify(data),
        (res) => {
          console.log("res", res);
        },
      );

      // Set the loading state to false
      setIconLoading({ icon: null, loading: false });
      addNotification({
        kind: "success",
        message: intl.formatMessage({
          id: "notification.slideover.button.subscribe.success",
        }),
        title: intl.formatMessage({ id: "notification.title" }),
      });
      setNotificationVisible(true);
      setSubscriptionState("SubscribedOnThisDevice");
    } catch (error) {
      // Handle any errors that occurred during the process
      console.error(
        "An error occurred during the subscription process:",
        error,
      );

      // let a = NotificationKinds.

      addNotification({
        kind: "warning",
        message: intl.formatMessage({
          id: "notification.slideover.button.subscribe.fail",
        }),
        title: intl.formatMessage({ id: "notification.title" }),
      });
      setNotificationVisible(true);
      setSubscriptionState("NotSubscribed");

      setIconLoading({ icon: null, loading: false });

      // Optionally set an error state here or provide user feedback
    }
  }

  const {
    loading,
    notifications,
    showRead,
    markNotificationAsRead,
    setShowRead,
    getNotifications,
    markAllNotificationsAsRead,
  } = props;

  const NotificationButton = ({ icon, label, onClick, disabled }) => (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        backgroundColor: disabled ? "#f0f0f0" : "white",
        padding: "0.5rem 0.8rem",
        fontWeight: "600",
        border: "none",
        borderRadius: "0.3rem",
        transition: "background-color 0.2s ease-in-out",
        color: disabled ? "#a1a1a1" : "#837994",
        whiteSpace: "nowrap",
        cursor: disabled ? "not-allowed" : "pointer",
      }}
      onMouseEnter={(e) => {
        if (!disabled) e.currentTarget.style.backgroundColor = "#DFDAE8";
      }}
      onMouseLeave={(e) => {
        if (!disabled) e.currentTarget.style.backgroundColor = "white";
      }}
    >
      {icon}
      <span style={{ fontSize: "0.75rem", marginLeft: "0.5rem" }}>{label}</span>
    </button>
  );

  return (
    <div
      style={{
        backgroundColor: "white",
        borderRadius: "0.3rem",
        transition: "background-color 0.2s ease-in-out",
        padding: "1rem",
        maxWidth: "600px",
        margin: "0 auto",
      }}
    >
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
        <br />

        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            flexWrap: "wrap",
          }}
        >
          {[
            {
              icon:
                iconLoading.loading == true && iconLoading.icon == "RELOAD" ? (
                  <Spinner />
                ) : (
                  <Renew />
                ),
              label: intl.formatMessage({
                id: "notification.slideover.button.reload",
              }),
              onClick: async () => {
                setIconLoading({ icon: "RELOAD", loading: true });
                await getNotifications();
                setIconLoading({ icon: null, loading: false });
              },
            },
            {
              icon:
                iconLoading.loading == true &&
                iconLoading.icon == "NOTIFICATION" ? (
                  <Spinner />
                ) : subscriptionState == "SubscribedOnThisDevice" ? (
                  <NotificationOff />
                ) : (
                  <NotificationFilled />
                ),
              label:
                subscriptionState &&
                subscriptionState == "SubscribedOnThisDevice"
                  ? intl.formatMessage({
                      id: "notification.slideover.button.unsubscribe",
                    })
                  : intl.formatMessage({
                      id: "notification.slideover.button.subscribe",
                    }),
              onClick: async () => {
                if (subscriptionState == "SubscribedOnThisDevice") {
                  unsubscribe();
                } else {
                  subscribe();
                }
              },
            },
            {
              icon:
                iconLoading.loading == true && iconLoading.icon == "EMAIL" ? (
                  <Spinner />
                ) : (
                  <Email />
                ),
              label: intl.formatMessage({
                id: "notification.slideover.button.markallasread",
              }),
              onClick: async () => {
                setIconLoading({ icon: "EMAIL", loading: true });
                await markAllNotificationsAsRead();
                setIconLoading({ icon: null, loading: false });
              },
            },
            {
              icon: <Filter />,
              label: showRead
                ? intl.formatMessage({
                    id: "notification.slideover.button.hideread",
                  })
                : intl.formatMessage({
                    id: "notification.slideover.button.showread",
                  }),
              onClick: () => setShowRead(!showRead),
            },
          ].map(({ icon, label, onClick }, index) => (
            <NotificationButton
              key={index}
              icon={icon}
              label={label}
              onClick={onClick}
            />
          ))}
        </div>
      </div>
      <div>
        {loading ? (
          <div style={{ textAlign: "center", marginTop: "1rem" }}>
            <Spinner />
          </div>
        ) : notifications && notifications.length > 0 ? (
          notifications.map((notification, index) => (
            <div
              key={index}
              style={{
                position: "relative",
                marginTop: "0.5rem",
                cursor: "pointer",
                borderRadius: "0.5rem",
                padding: "1.5rem 1rem",
                transition: "all 0.2s ease-in-out",
                backgroundColor: notification.readAt ? "#f3f3f3" : "white",
              }}
              onMouseOver={(e) => {
                if (!notification.readAt)
                  e.currentTarget.style.backgroundColor = "#f3f3f3";
              }}
              onMouseOut={(e) => {
                if (!notification.readAt)
                  e.currentTarget.style.backgroundColor = "white";
              }}
            >
              <div style={{ fontWeight: "500" }}>{notification.message}</div>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginTop: "0.5rem",
                  color: "#4b5563",
                  fontSize: "0.75rem",
                }}
              >
                <div>{formatTimestamp(notification.createdDate)}</div>
                <NotificationButton
                  icon={<Email />}
                  label={intl.formatMessage({
                    id: "notification.slideover.button.markasread",
                  })}
                  onClick={() => markNotificationAsRead(notification.id)}
                  disabled={!!notification.readAt}
                />
              </div>
            </div>
          ))
        ) : (
          <NoNotificationSVG />
        )}
      </div>
    </div>
  );
}
