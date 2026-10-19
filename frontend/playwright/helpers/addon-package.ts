/**
 * Builds an add-on package (a zip of addon.json + ui/ files) in memory for
 * end-to-end tests, without a zip dependency: entries are STORED (no
 * compression), which every zip reader accepts. Unsigned packages install as
 * "Unverified publisher" (docs/addons/design.md).
 */

const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c >>> 0;
  }
  return table;
})();

function crc32(data: Buffer): number {
  let crc = 0xffffffff;
  for (const byte of data) crc = CRC_TABLE[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
}

/** A zip of the given files (name -> text content), STORED entries. */
export function zipFiles(files: Record<string, string>): Buffer {
  const locals: Buffer[] = [];
  const centrals: Buffer[] = [];
  let offset = 0;
  for (const [name, text] of Object.entries(files)) {
    const nameBytes = Buffer.from(name, "utf8");
    const data = Buffer.from(text, "utf8");
    const crc = crc32(data);
    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4); // version needed
    local.writeUInt16LE(0x0800, 6); // UTF-8 names
    local.writeUInt16LE(0, 8); // stored
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(data.length, 18);
    local.writeUInt32LE(data.length, 22);
    local.writeUInt16LE(nameBytes.length, 26);
    locals.push(local, nameBytes, data);

    const central = Buffer.alloc(46);
    central.writeUInt32LE(0x02014b50, 0);
    central.writeUInt16LE(20, 4);
    central.writeUInt16LE(20, 6);
    central.writeUInt16LE(0x0800, 8);
    central.writeUInt16LE(0, 10);
    central.writeUInt32LE(crc, 16);
    central.writeUInt32LE(data.length, 20);
    central.writeUInt32LE(data.length, 24);
    central.writeUInt16LE(nameBytes.length, 28);
    central.writeUInt32LE(offset, 42);
    centrals.push(central, nameBytes);
    offset += local.length + nameBytes.length + data.length;
  }
  const centralSize = centrals.reduce((n, b) => n + b.length, 0);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(Object.keys(files).length, 8);
  end.writeUInt16LE(Object.keys(files).length, 10);
  end.writeUInt32LE(centralSize, 12);
  end.writeUInt32LE(offset, 16);
  return Buffer.concat([...locals, ...centrals, end]);
}

/**
 * A probe add-on: its page reports what it received from OpenELIS and what
 * the sandbox lets it see, so a test can read the result inside the frame.
 */
export function probeAddon(id: string) {
  const manifest = {
    schemaVersion: 1,
    id,
    name: `E2E probe ${id}`,
    version: "1.0.0",
    publisher: "e2e",
    description: "Reports its context and its sandbox from inside the frame.",
    image: {
      ref: `registry.example.org/${id}@sha256:${"0".repeat(64)}`,
    },
    scopes: ["orders.read"],
    events: [],
    extensions: [
      {
        name: "probe-page",
        slot: "app-menu-slot",
        page: "probe.html",
        order: 1,
        meta: { title: `Probe ${id}` },
      },
      {
        name: "probe-panel",
        slot: "result-validation-side-panel-slot",
        page: "probe.html",
        order: 1,
        meta: { title: `Probe panel ${id}`, height: 200 },
      },
    ],
  };
  const html = `<!doctype html>
<html><head><meta charset="utf-8"><link rel="stylesheet" href="probe.css"></head>
<body>
<p id="status">waiting</p>
<p id="script">script not run</p>
<p id="origin"></p><p id="cookie"></p><p id="page"></p><p id="token"></p>
<script src="probe.js"></script>
</body></html>`;
  const js = `document.getElementById("script").textContent = "script ran";
document.getElementById("origin").textContent = "origin " + self.origin;
try { void document.cookie; document.getElementById("cookie").textContent = "cookie readable"; }
catch (e) { document.getElementById("cookie").textContent = "cookie blocked"; }
window.addEventListener("message", function (event) {
  if (event.source !== window.parent || !event.data || event.data.type !== "context") return;
  document.getElementById("status").textContent = "context received";
  document.getElementById("page").textContent = "page " + event.data.context.page;
  document.getElementById("token").textContent = "token " + String(event.data.token).slice(0, 5);
});
window.parent.postMessage({ type: "ready" }, "*");`;
  return zipFiles({
    "addon.json": JSON.stringify(manifest),
    "ui/probe.html": html,
    "ui/probe.js": js,
    "ui/probe.css": "body { font-family: sans-serif; }",
  });
}
