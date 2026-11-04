// The MCP add-on's settings page (slot addon-settings-page-slot, administrators
// only). It runs in a sandboxed iframe with an opaque origin: everything it
// knows comes from the host's "context" message (a short-lived OpenELIS UI
// token and apiBase). It talks only to its own service, which checks that token
// with OpenELIS before doing anything. Data is put in the page with textContent
// only.
(function () {
  "use strict";

  var host = null; // the last context message
  var refreshTimer = null;
  var agents = [];
  var loaded = false; // status, tokens and calls shown at least once
  var $ = function (id) {
    return document.getElementById(id);
  };

  // ------------------------------------------------------------ plumbing

  function resize() {
    window.parent.postMessage({ type: "resize", height: document.body.scrollHeight + 16 }, "*");
  }

  function showError(text) {
    var el = $("error");
    el.hidden = !text;
    el.textContent = text || "";
    resize();
  }

  function api(method, path, body) {
    var init = { method: method, headers: { Authorization: "Bearer " + host.token } };
    if (body !== undefined) {
      init.headers["Content-Type"] = "application/json";
      init.body = JSON.stringify(body);
    }
    return fetch(host.apiBase + path, init).then(function (r) {
      if (r.status === 401) {
        window.parent.postMessage({ type: "token-refresh" }, "*");
        throw new Error("The session of this page expired; reloading it…");
      }
      if (r.status === 403) throw new Error("Only an administrator can manage MCP access.");
      if (r.status === 503) throw new Error("OpenELIS could not be reached; try again.");
      return r.json().then(function (data) {
        if (!r.ok) {
          var why = data && data.issues ? data.issues.join("; ") : (data && data.error) || r.status;
          throw new Error("Refused: " + why);
        }
        return data;
      });
    });
  }

  function cell(tr, text, className) {
    var td = document.createElement("td");
    td.textContent = text === undefined || text === null ? "" : String(text);
    if (className) td.className = className;
    tr.appendChild(td);
    return td;
  }

  function when(iso) {
    if (!iso) return "";
    var d = new Date(iso);
    return isNaN(d.getTime())
      ? iso
      : d.toLocaleString(host && host.locale ? host.locale.replace("_", "-") : undefined);
  }

  function copy(input) {
    input.select();
    var text = input.value;
    var done = function () {
      input.select();
    };
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done, function () {
          document.execCommand("copy");
        });
        return;
      }
    } catch (e) {
      /* the sandbox may refuse the clipboard: the text stays selected for Ctrl+C */
    }
    try {
      document.execCommand("copy");
    } catch (e) {
      /* selected; the user copies it */
    }
  }

  // ------------------------------------------------------------ status

  function loadStatus() {
    return api("GET", "status").then(function (s) {
      $("endpoint").textContent = s.endpoint;
      $("example").value =
        "claude mcp add --transport http openelis " +
        s.endpoint +
        ' --header "Authorization: Bearer <token>"';
      $("scopes").textContent = s.grantedScopes.length ? s.grantedScopes.join(", ") : "nothing yet";
      $("limits").textContent =
        "Limits per token: " +
        s.settings.ratePerMinute +
        " calls a minute, " +
        s.settings.ratePerDay +
        " a day (" +
        s.settings.timeZone +
        "). Calls are kept " +
        s.settings.auditRetentionDays +
        " days." +
        (s.openelis === "ok" ? "" : " OpenELIS is not reachable right now.");
    });
  }

  // ------------------------------------------------------------ agents

  function loadAgents() {
    return api("GET", "agents").then(function (r) {
      agents = r.agents || [];
      renderAgents();
      renderAgentFilter();
    });
  }

  function renderAgents() {
    var body = $("agents");
    body.textContent = "";
    $("no-agents").hidden = agents.length > 0;
    agents
      .slice()
      .reverse()
      .forEach(function (a) {
        var tr = document.createElement("tr");
        if (a.status !== "active") tr.className = "is-muted";
        var name = cell(tr, a.name);
        if (a.tools) name.title = "Tools: " + a.tools.join(", ");
        cell(tr, a.identifiers ? "yes" : "no");
        cell(tr, when(a.createdAt));
        cell(tr, a.expiresAt ? when(a.expiresAt) : "never");
        cell(tr, when(a.lastUsedAt));
        cell(tr, a.status);
        var actions = cell(tr, "");
        if (a.status === "active") {
          var b = document.createElement("button");
          b.type = "button";
          b.className = "mcp__button mcp__button--danger";
          b.textContent = "Revoke";
          b.setAttribute("data-testid", "mcp-revoke-" + a.id);
          b.addEventListener("click", function () {
            // two clicks: the sandbox has no confirm()
            if (!b.classList.contains("is-armed")) {
              b.classList.add("is-armed");
              b.textContent = "Confirm revoke";
              return;
            }
            b.disabled = true;
            api("POST", "agents/" + encodeURIComponent(a.id) + "/revoke")
              .then(loadAgents)
              .then(function () {
                showError(null);
              })
              .catch(function (e) {
                b.disabled = false;
                showError(e.message);
              });
          });
          actions.appendChild(b);
        }
        body.appendChild(tr);
      });
    resize();
  }

  function renderAgentFilter() {
    var sel = $("audit-agent");
    var current = sel.value;
    while (sel.options.length > 1) sel.remove(1);
    agents.forEach(function (a) {
      var o = document.createElement("option");
      o.value = a.id;
      o.textContent = a.name + (a.status === "active" ? "" : " (" + a.status + ")");
      sel.appendChild(o);
    });
    sel.value = current;
  }

  $("create").addEventListener("submit", function (e) {
    e.preventDefault();
    if (!host) return;
    var body = { name: $("name").value.trim(), identifiers: $("identifiers").checked };
    var days = $("expiry").value;
    if (days) body.expiresInDays = Number(days);
    $("create-button").disabled = true;
    api("POST", "agents", body)
      .then(function (r) {
        $("token").value = r.token;
        $("created").hidden = false;
        $("name").value = "";
        $("identifiers").checked = false;
        showError(null);
        return loadAgents();
      })
      .catch(function (err) {
        showError(err.message);
      })
      .then(function () {
        $("create-button").disabled = false;
        resize();
      });
  });

  $("copy-token").addEventListener("click", function () {
    copy($("token"));
  });
  $("copy-example").addEventListener("click", function () {
    copy($("example"));
  });

  // ------------------------------------------------------------ audit

  function loadAudit() {
    var agent = $("audit-agent").value;
    return api(
      "GET",
      "audit?limit=200" + (agent ? "&agent=" + encodeURIComponent(agent) : ""),
    ).then(function (r) {
      var body = $("audit");
      body.textContent = "";
      var lines = r.lines || [];
      $("no-audit").hidden = lines.length > 0;
      lines.forEach(function (l) {
        var tr = document.createElement("tr");
        cell(tr, when(l.at));
        cell(tr, l.agentName || (l.agent ? l.agent : "—"));
        cell(tr, l.client || "");
        cell(tr, l.tool || "");
        cell(tr, l.args === null || l.args === undefined ? "" : JSON.stringify(l.args));
        cell(tr, l.outcome, l.outcome === "ok" ? "mcp__outcome--ok" : "mcp__outcome--bad");
        cell(tr, l.rows);
        cell(tr, l.ms);
        body.appendChild(tr);
      });
      resize();
    });
  }

  $("audit-agent").addEventListener("change", function () {
    loadAudit().catch(function (e) {
      showError(e.message);
    });
  });
  $("audit-reload").addEventListener("click", function () {
    loadAudit().catch(function (e) {
      showError(e.message);
    });
  });

  // ------------------------------------------------------------ host

  function onContext(m) {
    host = m;
    // ask for a new token a minute before this one expires
    clearTimeout(refreshTimer);
    refreshTimer = setTimeout(
      function () {
        window.parent.postMessage({ type: "token-refresh" }, "*");
      },
      Math.max(30, (m.expiresIn || 600) - 60) * 1000,
    );
    if (loaded) return; // a refreshed token: nothing to reload
    Promise.all([loadStatus(), loadAgents()])
      .then(loadAudit)
      .then(function () {
        loaded = true;
        $("app").setAttribute("data-state", "ready");
        showError(null);
      })
      .catch(function (e) {
        $("app").setAttribute("data-state", "ready");
        showError(e.message);
      });
  }

  window.addEventListener("message", function (event) {
    if (event.source !== window.parent) return;
    var m = event.data || {};
    if (m.type === "context" && typeof m.token === "string" && typeof m.apiBase === "string")
      onContext(m);
  });

  window.addEventListener("resize", resize);
  window.parent.postMessage({ type: "ready" }, "*");
})();
