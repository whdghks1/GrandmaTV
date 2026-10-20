(function () {
  "use strict";
  if (window.__grandmaAssistInstalled) return;
  window.__grandmaAssistInstalled = true;

  // Safe mode: never modify video styles, positioning, player containers or playback.
  // Only click a visible and enabled explicit Skip Ad button when one appears.
  const skipTerms = /^(skip\s*(ad|ads|advertisement|commercial)?|광고\s*(건너뛰기|넘기기|스킵)|건너뛰기)$/i;
  const isVisible = (el) => {
    if (!el || !el.isConnected || el.disabled || el.getAttribute("aria-disabled") === "true") return false;
    const rect = el.getBoundingClientRect();
    const style = getComputedStyle(el);
    return rect.width > 0 && rect.height > 0 && style.visibility !== "hidden" &&
      style.display !== "none" && Number(style.opacity) > 0;
  };
  function trySkip(root) {
    try {
      for (const element of root.querySelectorAll('button, [role="button"], input[type="button"], a')) {
        const label = [element.innerText, element.value, element.getAttribute("aria-label"), element.getAttribute("title")]
          .filter(Boolean).join(" ").trim().replace(/\s+/g, " ");
        if (label.length < 90 && skipTerms.test(label) && isVisible(element)) {
          element.click();
          return true;
        }
      }
      for (const frame of root.querySelectorAll("iframe")) {
        try {
          if (frame.contentDocument && trySkip(frame.contentDocument)) return true;
        } catch (_) { /* Cross-origin frame cannot be accessed. */ }
      }
    } catch (_) { /* Page may be replacing its nodes. */ }
    return false;
  }
  setInterval(() => trySkip(document), 1500);
})();
