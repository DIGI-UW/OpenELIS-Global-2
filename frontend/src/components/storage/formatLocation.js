/**
 * Renders an assignment/movement row's location as "type #id (coord)", for the
 * storage audit trail and the inventory lot history that share that row shape.
 */
export function formatLocation(type, id, coord) {
  const parts = [];
  if (type) parts.push(type);
  if (id != null) parts.push(`#${id}`);
  if (coord) parts.push(`(${coord})`);
  return parts.length > 0 ? parts.join(" ") : "-";
}
