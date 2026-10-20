(function () {
  "use strict";
  if (window.__grandmaAssistInstalled) return;
  window.__grandmaAssistInstalled = true;
  // Keep the website/player untouched to preserve the proven playback path.
  const normalize = s => (s || "").replace(/\s+/g, " ").trim();
  const matches = s => /^(?:skip(?:\s+(?:this\s+)?(?:ad|ads|advertisement))?|광고\s*(?:건너뛰기|넘기기|스킵)|건너뛰기)(?:\s*[»›>])?$/i.test(normalize(s));
  const state = {checks:0, skipped:0, frames:0, inaccessibleFrames:0, candidates:0, last:"none"};
  const visible = el => {
    if (!el || !el.isConnected || el.disabled || el.getAttribute("aria-disabled")==="true") return false;
    const r = el.getBoundingClientRect(), cs = getComputedStyle(el);
    return r.width>1 && r.height>1 && cs.display!=="none" &&
      cs.visibility!=="hidden" && Number(cs.opacity)>0;
  };
  function scan(root) {
    const elements = root.querySelectorAll("button, a, [role=button], input[type=button], [class*=skip], [id*=skip]");
    for (const el of elements) {
      const labels = [el.innerText, el.textContent, el.value, el.getAttribute("aria-label"), el.title]
        .filter(Boolean).map(normalize);
      if (labels.some(matches) && visible(el)) {
        state.candidates++;
        state.last = labels.find(matches) || "skip";
        try { el.click(); state.skipped++; return true; } catch (_) {}
      }
      if (el.shadowRoot && scan(el.shadowRoot)) return true;
    }
    for (const frame of root.querySelectorAll("iframe")) {
      state.frames++;
      try {
        if (frame.contentDocument && scan(frame.contentDocument)) return true;
        if (!frame.contentDocument) state.inaccessibleFrames++;
      } catch (_) { state.inaccessibleFrames++; }
    }
    return false;
  }
  window.__grandmaDiagnostics = () => JSON.stringify(state);
  setInterval(() => {
    state.checks++;
    state.frames = 0;
    state.inaccessibleFrames = 0;
    try { scan(document); } catch (_) {}
  }, 1500);
})();