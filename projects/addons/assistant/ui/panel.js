// The assistant's panel. It runs in a sandboxed iframe with an opaque origin:
// no OpenELIS cookie, storage or page. All it knows comes from the host's
// "context" message (slot, page, a short-lived token, locale, apiBase). It talks
// only to its own service (apiBase), which asks OpenELIS for the de-identified
// context with that token.
(function () {
  "use strict";

  var STRINGS = {
    en: {
      title: "AI assistant",
      placeholder: "Ask a question",
      send: "Send",
      newConversation: "New conversation",
      continuePrevious: "Continue the previous conversation",
      note: "The assistant receives only sex, age and results — no name, patient id or lab number.",
      thinking: "Writing the answer…",
      quota: "Questions today: {used}/{limit}",
      quotaUnlimited: "Questions today: {used}",
      egfrCorrected: "kidney staging corrected by the system",
      "error.empty": "Type a question.",
      "error.quota": "You have used all of today's questions. Ask again tomorrow.",
      "error.provider": "The assistant is busy or failing; try again in a few minutes. This question was not counted.",
      "error.not-configured": "The assistant is not configured yet (Administration → Extensions).",
      "error.token": "The session of this panel expired; reloading it…",
      "error.openelis": "OpenELIS could not be reached; try again.",
      "error.busy": "Too many questions at once; try again in a moment.",
      "error.network": "The connection dropped while answering. Try again.",
      "suggest.abnormalSummary": "Summarise the abnormal results",
      "suggest.clusterThinking": "How to think about this group of results",
      "suggest.egfrStage": "Which stage is eGFR {value}?",
      "suggest.abnormalOne": "{test} {value} {unit} is {direction} — is it a concern?",
      "suggest.moreTests": "Suggest further tests",
      "suggest.patientAdvice": "Write plain advice for the patient",
      "suggest.validationPriority": "Which case needs a closer look first?",
      "suggest.validationDelta": "Which results changed a lot since last time?",
      "suggest.whatIsScreen": "What is this screen for?",
      high: "high",
      low: "low",
      disclaimer: "Suggestion for reference only — not a substitute for a physician's diagnosis."
    },
    vi: {
      title: "Phụ tá AI",
      placeholder: "Đặt câu hỏi",
      send: "Gửi",
      newConversation: "Hội thoại mới",
      continuePrevious: "Tiếp tục hội thoại trước",
      note: "Trợ lý chỉ nhận giới, tuổi và kết quả — không nhận họ tên, mã bệnh nhân, số phiếu.",
      thinking: "Đang soạn câu trả lời…",
      quota: "Câu hỏi hôm nay: {used}/{limit}",
      quotaUnlimited: "Câu hỏi hôm nay: {used}",
      egfrCorrected: "đã tự sửa phân độ eGFR",
      "error.empty": "Hãy nhập câu hỏi.",
      "error.quota": "Bạn đã dùng hết số câu hỏi hôm nay. Mai hỏi tiếp nhé.",
      "error.provider": "Trợ lý đang bận hoặc lỗi, thử lại sau ít phút. Câu này không bị tính vào hạn mức.",
      "error.not-configured": "Phụ tá AI chưa được cấu hình (Quản trị → Tiện ích mở rộng).",
      "error.token": "Phiên của khung này đã hết hạn, đang tải lại…",
      "error.openelis": "Không kết nối được OpenELIS, thử lại nhé.",
      "error.busy": "Đang có nhiều câu hỏi cùng lúc, thử lại sau giây lát.",
      "error.network": "Mất kết nối khi đang trả lời. Thử lại nhé.",
      "suggest.abnormalSummary": "Tóm tắt các chỉ số bất thường",
      "suggest.clusterThinking": "Hướng nghĩ cho cụm kết quả này",
      "suggest.egfrStage": "eGFR {value} thuộc giai đoạn nào?",
      "suggest.abnormalOne": "{test} {value} {unit} {direction} — có đáng lo không?",
      "suggest.moreTests": "Gợi ý xét nghiệm làm thêm",
      "suggest.patientAdvice": "Viết lời dặn dễ hiểu cho bệnh nhân",
      "suggest.validationPriority": "Ca nào cần xem kỹ trước?",
      "suggest.validationDelta": "Kết quả nào lệch nhiều so với lần trước?",
      "suggest.whatIsScreen": "Màn này dùng để làm gì?",
      high: "cao",
      low: "thấp",
      disclaimer: "Gợi ý tham khảo — không thay chẩn đoán của bác sĩ."
    }
  };

  var PAGE_OF_SLOT = {
    "result-validation-side-panel-slot": "validation",
    "result-lookup-side-panel-slot": "lookup",
    "top-nav-actions-slot": "other"
  };

  var host = null; // the last context message
  var strings = STRINGS.en;
  var conversationId = null;
  var previousId = null;
  var busy = false;
  var forceNew = false; // "New conversation": the next question starts a fresh one
  var refreshTimer = null;
  var $ = function (id) { return document.getElementById(id); };

  function t(key, values) {
    var s = strings[key] || STRINGS.en[key] || key;
    Object.keys(values || {}).forEach(function (k) {
      s = s.split("{" + k + "}").join(values[k]);
    });
    return s;
  }

  function page() {
    return PAGE_OF_SLOT[host.slot] || "other";
  }

  function pageName() {
    return (host.context && host.context.page) || "";
  }

  function api(path) {
    return host.apiBase + path;
  }

  function headers(json) {
    var h = { Authorization: "Bearer " + host.token };
    if (json) h["Content-Type"] = "application/json";
    return h;
  }

  function resize() {
    if (host && host.slot === "top-nav-actions-slot") return; // the header panel fills its space
    window.parent.postMessage({ type: "resize", height: document.body.scrollHeight + 8 }, "*");
  }

  function showError(code) {
    var el = $("error");
    if (!code) {
      el.hidden = true;
      el.textContent = "";
    } else {
      el.hidden = false;
      el.textContent = t("error." + code);
      if (code === "token") window.parent.postMessage({ type: "token-refresh" }, "*");
    }
    resize();
  }

  function setQuota(q) {
    if (!q) return;
    $("quota").textContent = q.limit > 0 ? t("quota", q) : t("quotaUnlimited", q);
  }

  function bubble(kind, text) {
    var el = document.createElement("div");
    el.className = "ai__msg ai__msg--" + kind;
    if (kind === "q") {
      el.textContent = text;
    } else {
      el.appendChild(window.AssistantMarkdown.render(text));
    }
    $("log").appendChild(el);
    el.scrollIntoView({ block: "end" });
    return el;
  }

  function renderSuggestions(list) {
    var box = $("suggestions");
    box.innerHTML = "";
    (list || []).forEach(function (s) {
      var v = Object.assign({}, s.values || {});
      if (v.direction) v.direction = t(v.direction);
      var b = document.createElement("button");
      b.type = "button";
      b.className = "ai__chip";
      b.setAttribute("data-suggestion", s.id);
      b.textContent = t("suggest." + s.id, v);
      b.addEventListener("click", function () { ask(b.textContent, s.id); });
      box.appendChild(b);
    });
  }

  function renderConversation(c) {
    $("log").innerHTML = "";
    conversationId = c ? c.id : null;
    ((c && c.turns) || []).forEach(function (turn) {
      bubble("q", turn.question);
      bubble("a", turn.answer || "");
    });
  }

  function loadStatus() {
    var q = "page=" + encodeURIComponent(page()) + "&name=" + encodeURIComponent(pageName());
    if (conversationId) q += "&conversationId=" + encodeURIComponent(conversationId);
    return fetch(api("status?" + q), { headers: headers(false) })
      .then(function (r) {
        if (r.status === 401) throw { code: "token" };
        if (!r.ok) throw { code: "openelis" };
        return r.json();
      })
      .then(function (s) {
        $("app").setAttribute("data-state", s.enabled ? "ready" : "off");
        if (!s.enabled) showError(s.reason || "not-configured");
        else showError(null);
        setQuota(s.quota);
        renderSuggestions(s.suggestions);
        renderConversation(s.conversation);
        previousId = s.previous ? s.previous.id : null;
        $("previous").hidden = !previousId;
        resize();
      })
      .catch(function (e) { showError((e && e.code) || "openelis"); });
  }

  // POST /ask, read the text/event-stream answer as it comes
  function ask(question, suggestionId) {
    question = (question || "").trim();
    if (!question) return showError("empty");
    if (busy || !host) return;
    busy = true;
    $("send").disabled = true;
    showError(null);
    bubble("q", question);
    var answer = bubble("a", "");
    answer.classList.add("ai__msg--pending");
    answer.textContent = t("thinking");
    var streamed = "";
    var finished = false;
    var startNew = forceNew;
    forceNew = false;

    function onEvent(name, data) {
      if (name === "meta") {
        conversationId = data.conversationId;
      } else if (name === "delta") {
        streamed += data.t;
        answer.classList.remove("ai__msg--pending");
        answer.innerHTML = "";
        answer.appendChild(window.AssistantMarkdown.render(streamed));
      } else if (name === "reset") {
        streamed = "";
        answer.textContent = t("thinking");
      } else if (name === "done") {
        finished = true;
        answer.classList.remove("ai__msg--pending");
        answer.innerHTML = "";
        answer.appendChild(window.AssistantMarkdown.render(data.answer));
        answer.setAttribute("data-testid", "ai-answer");
        answer.setAttribute("data-provider", data.provider || "");
        if (data.egfrCorrected) {
          var tag = document.createElement("small");
          tag.className = "ai__tag";
          tag.textContent = t("egfrCorrected");
          answer.appendChild(tag);
        }
        conversationId = data.conversationId;
        setQuota(data.quota);
      } else if (name === "error") {
        finished = true;
        answer.remove();
        showError(data.code);
      }
    }

    fetch(api("ask"), {
      method: "POST",
      headers: Object.assign(headers(true), { Accept: "text/event-stream" }),
      body: JSON.stringify({
        page: page(), name: pageName(), question: question, suggestionId: suggestionId || null,
        conversationId: conversationId, newConversation: startNew
      })
    })
      .then(function (r) {
        if (r.status === 401) throw { code: "token" };
        var type = r.headers.get("Content-Type") || "";
        if (type.indexOf("text/event-stream") < 0) {
          return r.json().then(function (d) { onEvent(d.event || "error", d); });
        }
        var reader = r.body.getReader();
        var decoder = new TextDecoder();
        var buffer = "";
        function pump() {
          return reader.read().then(function (chunk) {
            if (chunk.done) return;
            buffer += decoder.decode(chunk.value, { stream: true });
            var parts = buffer.split("\n\n");
            buffer = parts.pop();
            parts.forEach(function (block) {
              var name = "message";
              var data = "";
              block.split("\n").forEach(function (line) {
                if (line.indexOf("event: ") === 0) name = line.slice(7);
                else if (line.indexOf("data: ") === 0) data += line.slice(6);
              });
              if (data) onEvent(name, JSON.parse(data));
            });
            return pump();
          });
        }
        return pump();
      })
      .catch(function (e) {
        if (!finished) {
          answer.remove();
          showError((e && e.code) || "network");
        }
      })
      .then(function () {
        if (!finished && answer.isConnected && answer.classList.contains("ai__msg--pending")) {
          answer.remove();
          showError("network");
        }
        busy = false;
        $("send").disabled = false;
        resize();
      });
  }

  function continuePrevious() {
    if (!previousId) return;
    fetch(api("continue"), {
      method: "POST",
      headers: headers(true),
      body: JSON.stringify({ page: page(), name: pageName(), previousId: previousId })
    })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (c) {
        if (!c) return;
        $("previous").hidden = true;
        previousId = null;
        renderConversation(c);
        if (c.seedSummary) {
          var s = document.createElement("div");
          s.className = "ai__msg ai__msg--seed";
          s.appendChild(window.AssistantMarkdown.render(c.seedSummary));
          $("log").appendChild(s);
        }
        resize();
      });
  }

  function onContext(m) {
    var first = host === null;
    var samePage = host && host.slot === m.slot && JSON.stringify(host.context) === JSON.stringify(m.context);
    host = m;
    strings = String(m.locale || "en").toLowerCase().indexOf("vi") === 0 ? STRINGS.vi : STRINGS.en;
    if (first) {
      document.documentElement.lang = strings === STRINGS.vi ? "vi" : "en";
      document.title = t("title");
      $("question").placeholder = t("placeholder");
      $("send").textContent = t("send");
      $("new").textContent = t("newConversation");
      $("continue").textContent = t("continuePrevious");
      $("note").textContent = t("note");
      $("disclaimer").textContent = t("disclaimer");
    }
    // ask for a new token a minute before this one expires
    clearTimeout(refreshTimer);
    refreshTimer = setTimeout(function () {
      window.parent.postMessage({ type: "token-refresh" }, "*");
    }, Math.max(30, (m.expiresIn || 600) - 60) * 1000);
    if (!samePage) {
      conversationId = null; // another patient or page: another conversation (the service decides)
      loadStatus();
    } else {
      showError(null);
    }
  }

  window.addEventListener("message", function (event) {
    if (event.source !== window.parent) return;
    var m = event.data || {};
    if (m.type === "context" && typeof m.token === "string" && typeof m.apiBase === "string") onContext(m);
  });

  $("form").addEventListener("submit", function (e) {
    e.preventDefault();
    var q = $("question").value;
    $("question").value = "";
    ask(q);
  });
  $("question").addEventListener("keydown", function (e) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      $("form").requestSubmit();
    }
  });
  $("new").addEventListener("click", function () {
    conversationId = null;
    forceNew = true;
    $("log").innerHTML = "";
    showError(null);
  });
  $("continue").addEventListener("click", continuePrevious);

  window.parent.postMessage({ type: "ready" }, "*");
})();
