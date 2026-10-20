(function () {
  "use strict";
  if (window.__grandmaAssistInstalled) return;
  window.__grandmaAssistInstalled = true;

  // Only click an enabled, visible, explicit Skip Ad control. No ad stream manipulation.
  const skipTerms = /^(skip\s*(ad|ads|advertisement|commercial)?|skip\s*ads?|광고\s*(건너뛰기|넘기기|스킵)|건너뛰기|광고\s*닫기)$/i;
  let focusEnabled = true;
  let focusedVideo = null;
  let previousVideoStyle = null;

  const visible = (el) => {
    if (!el || !el.isConnected || el.disabled || el.getAttribute("aria-disabled") === "true") return false;
    const rect = el.getBoundingClientRect();
    const css = getComputedStyle(el);
    return rect.width > 0 && rect.height > 0 && css.display !== "none" &&
      css.visibility !== "hidden" && Number(css.opacity) > 0;
  };

  const isSkip = (el) => {
    const txt = [el.innerText, el.getAttribute("aria-label"), el.getAttribute("title")]
      .filter(Boolean).join(" ").trim().replace(/\s+/g, " ");
    return !!txt && txt.length < 90 && skipTerms.test(txt);
  };

  function skipAds(doc) {
    try {
      const elements = doc.querySelectorAll('button, [role="button"], input[type="button"], a');
      for (const el of elements) {
        if (visible(el) && isSkip(el)) {
          el.click();
          return true;
        }
      }
      for (const frame of doc.querySelectorAll("iframe")) {
        try { if (frame.contentDocument && skipAds(frame.contentDocument)) return true; }
        catch (_) { /* cross-origin frame: cannot access it */ }
      }
    } catch (_) { /* site might replace the document */ }
    return false;
  }

  function restoreFocus() {
    if (focusedVideo && previousVideoStyle !== null) {
      if (previousVideoStyle) focusedVideo.setAttribute("style", previousVideoStyle);
      else focusedVideo.removeAttribute("style");
    }
    focusedVideo = null;
    previousVideoStyle = null;
  }

  function focusVideo() {
    if (!focusEnabled) { restoreFocus(); return; }
    // Prefer the largest actively playing video; never synthesize a new stream.
    const videos = [...document.querySelectorAll("video")].filter(v => v.isConnected && v.getBoundingClientRect().width > 60);
    const playing = videos.filter(v => !v.paused && v.readyState >= 2);
    const candidates = playing.length ? playing : videos;
    candidates.sort((a,b) => {
      const ra=a.getBoundingClientRect(), rb=b.getBoundingClientRect();
      return rb.width*rb.height-ra.width*ra.height;
    });
    const video = candidates[0];
    if (!video) return;
    if (focusedVideo === video) return;
    restoreFocus();
    focusedVideo = video;
    previousVideoStyle = video.getAttribute("style");
    video.style.setProperty("position", "fixed", "important");
    video.style.setProperty("inset", "0", "important");
    video.style.setProperty("width", "100vw", "important");
    video.style.setProperty("height", "100vh", "important");
    video.style.setProperty("max-width", "none", "important");
    video.style.setProperty("max-height", "none", "important");
    video.style.setProperty("object-fit", "contain", "important");
    video.style.setProperty("z-index", "2147483640", "important");
    video.style.setProperty("background", "black", "important");
  }

  window.__grandmaSetFocus = function (enabled) {
    focusEnabled = !!enabled;
    if (!focusEnabled) restoreFocus();
    else focusVideo();
  };

  // Some providers put their player inside cross-origin iframes.
  // Those players remain untouched, and the original page remains available.
  setInterval(() => { skipAds(document); focusVideo(); }, 1500);
  focusVideo();
})();