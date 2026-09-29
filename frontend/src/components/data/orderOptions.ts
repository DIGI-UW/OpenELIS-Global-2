export interface OrderPriorityOption {
  value: string;
  labelId: string;
}

// Values are the backend OrderPriority enum codes; the server rejects anything else.
export const priorities: OrderPriorityOption[] = [
  { value: "ROUTINE", labelId: "order.priority.routine" },
  { value: "ASAP", labelId: "order.priority.asap" },
  { value: "STAT", labelId: "order.priority.stat" },
  { value: "TIMED", labelId: "order.priority.timed" },
  { value: "FUTURE_STAT", labelId: "order.priority.futureStat" },
];
