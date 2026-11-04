// Minimal, SAFE Markdown: builds DOM nodes only (never innerHTML), so text from
// the model can never become HTML. Paragraphs, line breaks, headings (#),
// lists (- * and 1.), **bold**, *italic* / _italic_, `code`.
(function (global) {
  "use strict";

  function inline(parent, text) {
    var re = /(\*\*[^*]+\*\*|__[^_]+__|`[^`]+`|\*[^*\s][^*]*\*|_[^_\s][^_]*_)/g;
    var last = 0;
    var m;
    while ((m = re.exec(text))) {
      if (m.index > last) parent.appendChild(document.createTextNode(text.slice(last, m.index)));
      var tok = m[0];
      var el;
      if (tok.indexOf("**") === 0 || tok.indexOf("__") === 0) {
        el = document.createElement("strong");
        el.textContent = tok.slice(2, -2);
      } else if (tok.charAt(0) === "`") {
        el = document.createElement("code");
        el.textContent = tok.slice(1, -1);
      } else {
        el = document.createElement("em");
        el.textContent = tok.slice(1, -1);
      }
      parent.appendChild(el);
      last = m.index + tok.length;
    }
    if (last < text.length) parent.appendChild(document.createTextNode(text.slice(last)));
  }

  function blocks(src) {
    var out = [];
    var cur = null;
    function flush() {
      if (cur) out.push(cur);
      cur = null;
    }
    src.replace(/\r/g, "").split("\n").forEach(function (raw) {
      var line = raw.replace(/\s+$/, "");
      var ul = /^\s*[-*•]\s+(.*)$/.exec(line);
      var ol = /^\s*\d+[.)]\s+(.*)$/.exec(line);
      var h = /^\s*#{1,6}\s+(.*)$/.exec(line);
      if (!line.trim()) {
        flush();
      } else if (h) {
        flush();
        out.push({ kind: "h", text: h[1] });
      } else if (ul || ol) {
        var kind = ul ? "ul" : "ol";
        if (!cur || cur.kind !== kind) {
          flush();
          cur = { kind: kind, items: [] };
        }
        cur.items.push((ul || ol)[1]);
      } else if (cur && (cur.kind === "ul" || cur.kind === "ol") && /^\s{2,}/.test(raw)) {
        cur.items[cur.items.length - 1] += " " + line.trim();
      } else {
        if (!cur || cur.kind !== "p") {
          flush();
          cur = { kind: "p", lines: [] };
        }
        cur.lines.push(line.trim());
      }
    });
    flush();
    return out;
  }

  function render(text) {
    var root = document.createElement("div");
    root.className = "ai__md";
    blocks(text).forEach(function (b) {
      var el;
      if (b.kind === "h") {
        el = document.createElement("p");
        el.className = "ai__md-h";
        var s = document.createElement("strong");
        inline(s, b.text.replace(/\*\*/g, ""));
        el.appendChild(s);
      } else if (b.kind === "ul" || b.kind === "ol") {
        el = document.createElement(b.kind);
        b.items.forEach(function (it) {
          var li = document.createElement("li");
          inline(li, it);
          el.appendChild(li);
        });
      } else {
        el = document.createElement("p");
        b.lines.forEach(function (l, j) {
          if (j > 0) el.appendChild(document.createElement("br"));
          inline(el, l);
        });
      }
      root.appendChild(el);
    });
    return root;
  }

  global.AssistantMarkdown = { render: render, blocks: blocks };
})(window);
