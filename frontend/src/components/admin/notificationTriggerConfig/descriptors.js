// Shared per-event metadata for the Notification Trigger Configuration admin.
// Both the Triggers tab (row label + NEW tag) and the Templates tab (per-event
// tab strip + description line) consume this map. Trigger event codes that
// aren't listed here render with the raw event_code as their label.
export const TRIGGER_DESCRIPTORS = {
  REFERRAL_OUT: {
    nameKey: "notificationtrigger.referralout.name",
    descriptionKey: "notificationtrigger.referralout.description",
    templateDescriptionKey:
      "notificationtrigger.templates.referralout.description",
    statusKey: "notificationtrigger.status.new",
  },
  SUBCONTRACT_DISPATCHED: {
    nameKey: "notificationtrigger.subcontractdispatched.name",
    descriptionKey: "notificationtrigger.subcontractdispatched.description",
    templateDescriptionKey:
      "notificationtrigger.templates.subcontractdispatched.description",
    statusKey: "notificationtrigger.status.new",
  },
};
