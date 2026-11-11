export interface OrderPriorityOption {
  value: string;
  labelId: string;
}

// Values are the backend OrderPriority enum codes; the server rejects anything else.
export const priorities: OrderPriorityOption[] = [
  { value: "ROUTINE", labelId: "sample.priority.ROUTINE" },
  { value: "ASAP", labelId: "sample.priority.ASAP" },
  { value: "STAT", labelId: "sample.priority.STAT" },
  { value: "TIMED", labelId: "sample.priority.TIMED" },
  { value: "FUTURE_STAT", labelId: "sample.priority.FUTURE_STAT" },
];
