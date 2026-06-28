export interface OrderPriorityOption {
  value: string;
  label: string;
}

export const priorities: OrderPriorityOption[] = [
  {
    value: "Routine",
    label: "ROUTINE",
  },
  {
    value: "ASAP",
    label: "ASAP",
  },
  {
    value: "STAT",
    label: "STAT",
  },
  {
    value: "Timed",
    label: "Timed",
  },
  {
    value: "FUTURE_STAT",
    label: "Future STAT",
  },
];
