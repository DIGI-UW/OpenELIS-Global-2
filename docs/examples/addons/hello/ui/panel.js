// Runs in a sandboxed iframe (opaque origin): it cannot read OpenELIS cookies,
// storage or pages. Everything it knows comes from the host's "context" message.
(function () {
  var status = document.getElementById("status");
  var list = document.getElementById("orders");

  window.addEventListener("message", function (event) {
    if (event.source !== window.parent) return;
    var m = event.data || {};
    if (m.type !== "context") return;
    var orders = (m.context && m.context.accessionNumbers) || [];
    status.textContent = orders.length + " order(s) on screen";
    list.innerHTML = "";
    orders.forEach(function (acc) {
      var li = document.createElement("li");
      li.textContent = acc + ": …";
      list.appendChild(li);
      // Our own service, reached through the reverse proxy; it calls the
      // OpenELIS add-on API with the token, which only covers these orders.
      fetch(m.apiBase + "summary?acc=" + encodeURIComponent(acc), {
        headers: { Authorization: "Bearer " + m.token },
      })
        .then(function (r) { return r.json(); })
        .then(function (s) { li.textContent = acc + ": " + s.validated + "/" + s.total + " validated"; })
        .catch(function () { li.textContent = acc + ": unavailable"; });
    });
    window.parent.postMessage({ type: "resize", height: document.body.scrollHeight + 16 }, "*");
  });

  window.parent.postMessage({ type: "ready" }, "*");
})();
