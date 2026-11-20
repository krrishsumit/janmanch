// Shared code for every JanManch page: login state, API calls, the page
// shell (top bar, side menu, phone tab bar), icons and video cards.

const TOKEN_KEY = "janmanch_token";
const NAME_KEY = "janmanch_name";

// ---------- Login state ----------

function getToken() {
    try { return localStorage.getItem(TOKEN_KEY); } catch (e) { return null; }
}

function getName() {
    try { return localStorage.getItem(NAME_KEY) || ""; } catch (e) { return ""; }
}

function saveLogin(auth) {
    try {
        localStorage.setItem(TOKEN_KEY, auth.token);
        localStorage.setItem(NAME_KEY, auth.name);
    } catch (e) { /* storage unavailable: the login lasts for this page only */ }
}

function clearLogin() {
    try {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(NAME_KEY);
    } catch (e) { /* nothing to clear */ }
}

function logout() {
    clearLogin();
    location.href = "/";
}

function loginUrl() {
    return "/login.html?next=" + encodeURIComponent(location.pathname + location.search);
}

/** Sends visitors who are not logged in to the login page. Returns false if it redirected. */
function requireLogin() {
    if (getToken()) return true;
    location.href = loginUrl();
    return false;
}

// ---------- API ----------

/** Calls the API and returns the parsed JSON (or null for an empty response). */
async function api(path, options = {}) {
    const headers = Object.assign({}, options.headers);
    const token = getToken();
    if (token) headers["Authorization"] = "Bearer " + token;
    const init = Object.assign({}, options, { headers });
    if (options.body && !(options.body instanceof FormData)) {
        headers["Content-Type"] = "application/json";
        init.body = JSON.stringify(options.body);
    }
    const response = await fetch(path, init);
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = null; }
    if (!response.ok) {
        if (response.status === 401 && token) clearLogin(); // expired or invalid token
        const error = new Error(response.status === 401
            ? "Please log in to continue"
            : (data && data.message) || "Something went wrong (" + response.status + ")");
        error.status = response.status;
        throw error;
    }
    return data;
}

// ---------- DOM helpers ----------

/**
 * Builds an element. Text children are added as text nodes, so user input can
 * never inject HTML. Attributes starting with "on" are event listeners.
 */
function el(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs || {})) {
        if (value === null || value === undefined || value === false) continue;
        if (key.startsWith("on") && typeof value === "function") {
            node.addEventListener(key.slice(2).toLowerCase(), value);
        } else if (key === "style" && typeof value === "object") {
            Object.assign(node.style, value);
        } else {
            node.setAttribute(key, value === true ? "" : value);
        }
    }
    for (const child of children.flat()) {
        if (child === null || child === undefined || child === false) continue;
        node.append(child instanceof Node ? child : document.createTextNode(String(child)));
    }
    return node;
}

function $(selector) {
    return document.querySelector(selector);
}

// ---------- Icons (fixed markup, never user input) ----------

const ICONS = {
    home: '<path d="M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>',
    trending: '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
    subscriptions: '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M7 3h10M10 11l5 2.5-5 2.5z"/>',
    history: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    studio: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M8 4v5"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/>',
    play: '<path d="M8 5v14l11-7z" fill="currentColor" stroke="none"/>',
    pause: '<rect x="6" y="5" width="4" height="14" rx="1" fill="currentColor" stroke="none"/><rect x="14" y="5" width="4" height="14" rx="1" fill="currentColor" stroke="none"/>',
    volume: '<path d="M11 5L6 9H3v6h3l5 4z"/><path d="M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13"/>',
    mute: '<path d="M11 5L6 9H3v6h3l5 4z"/><path d="M16 9l5 6M21 9l-5 6"/>',
    fullscreen: '<path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>',
    like: '<path d="M7 10v11H3V10zM7 10l4-8a3 3 0 0 1 3 3v4h6a2 2 0 0 1 2 2.3l-1.4 8A2 2 0 0 1 18.6 21H7"/>',
    dislike: '<path d="M17 14V3h4v11zM17 14l-4 8a3 3 0 0 1-3-3v-4H4a2 2 0 0 1-2-2.3l1.4-8A2 2 0 0 1 5.4 3H17"/>',
    share: '<path d="M14 5l7 7-7 7M21 12H9a6 6 0 0 0-6 6"/>',
    trash: '<path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"/>',
    logout: '<path d="M15 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4M10 17l5-5-5-5M15 12H3"/>',
    film: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M10 9l5 3-5 3z"/>',
    upload: '<path d="M12 16V4M7 9l5-5 5 5M4 20h16"/>',
    shorts: '<rect x="7" y="2" width="10" height="20" rx="3"/><path d="M11 9l3 2-3 2z"/>'
};

function icon(name, size = 20, strokeWidth = 2) {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("width", size);
    svg.setAttribute("height", size);
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("fill", "none");
    svg.setAttribute("stroke", "currentColor");
    svg.setAttribute("stroke-width", strokeWidth);
    svg.setAttribute("stroke-linecap", "round");
    svg.setAttribute("stroke-linejoin", "round");
    svg.setAttribute("aria-hidden", "true");
    svg.innerHTML = ICONS[name] || "";
    return svg;
}

// ---------- Formatting ----------

function compactNumber(n) {
    if (n >= 10000000) return (n / 10000000).toFixed(n >= 100000000 ? 0 : 1).replace(/\.0$/, "") + "Cr";
    if (n >= 1000000) return (n / 1000000).toFixed(1).replace(/\.0$/, "") + "M";
    if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, "") + "K";
    return String(n);
}

function formatViews(n) {
    return compactNumber(n) + (n === 1 ? " view" : " views");
}

function formatCount(n, singular, plural) {
    return n.toLocaleString() + " " + (n === 1 ? singular : plural);
}

function formatDate(iso) {
    return new Date(iso).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

function timeAgo(iso) {
    const seconds = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000);
    const units = [["year", 31536000], ["month", 2592000], ["week", 604800], ["day", 86400], ["hour", 3600], ["minute", 60]];
    for (const [unit, size] of units) {
        const value = Math.floor(seconds / size);
        if (value >= 1) return value + " " + unit + (value === 1 ? "" : "s") + " ago";
    }
    return "just now";
}

function formatDuration(seconds) {
    if (seconds === null || seconds === undefined || isNaN(seconds)) return "";
    seconds = Math.floor(seconds);
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = String(seconds % 60).padStart(2, "0");
    return h ? h + ":" + String(m).padStart(2, "0") + ":" + s : m + ":" + s;
}

// ---------- Colours for avatars and thumbnail placeholders ----------

const AVATAR_COLORS = ["#2E7D6B", "#3A6FA8", "#B4573A", "#2E7D57", "#8A3F78", "#A07C22", "#5157A8", "#4C6A85"];
const THUMB_COLORS = ["#2C3B5A", "#23415A", "#5A3B2C", "#5E4A1F", "#1F4D3A", "#2F3F4F", "#4E2A44", "#2B2E5A", "#3E3A2B"];

function hashOf(text) {
    let hash = 0;
    for (const ch of String(text || "")) hash = (hash * 31 + ch.codePointAt(0)) >>> 0;
    return hash;
}

function avatar(name, size = 36) {
    const letter = (String(name || "?").trim()[0] || "?");
    return el("span", {
        class: "avatar", "aria-hidden": "true",
        style: { width: size + "px", height: size + "px", fontSize: Math.round(size * 0.42) + "px",
                 background: AVATAR_COLORS[hashOf(name) % AVATAR_COLORS.length] }
    }, letter);
}

/** A 16:9 thumbnail: the real image when there is one, otherwise a coloured block. */
function thumbnail(video, options = {}) {
    const box = el(options.href ? "a" : "span", {
        class: "thumb", href: options.href, "aria-label": options.href ? video.title : null,
        style: { background: THUMB_COLORS[hashOf(video.title) % THUMB_COLORS.length] }
    });
    if (video.thumbnailUrl) {
        box.append(el("img", { src: video.thumbnailUrl, alt: "", loading: "lazy" }));
    } else {
        box.append(el("span", { class: "placeholder" }, icon("film", options.iconSize || 36, 1.5)));
    }
    if (options.flag) box.append(el("span", { class: "flag" }, options.flag));
    const duration = formatDuration(video.durationSeconds);
    if (duration) box.append(el("span", { class: "duration" }, duration));
    return box;
}

function watchUrl(id) {
    return "/watch.html?id=" + encodeURIComponent(id);
}

function channelUrl(id) {
    return "/?channel=" + encodeURIComponent(id);
}

/** Grid card used on the home, search, channel and history pages. */
function videoCard(video, extraMeta) {
    return el("div", { class: "card" },
        thumbnail(video, { href: watchUrl(video.id) }),
        el("div", { class: "card-body" },
            el("a", { href: channelUrl(video.channelId), tabindex: "-1" }, avatar(video.channelName, 36)),
            el("div", { class: "card-text" },
                el("a", { href: watchUrl(video.id), class: "card-title" }, video.title),
                el("div", { class: "card-meta" }, el("a", { href: channelUrl(video.channelId) }, video.channelName)),
                el("div", { class: "card-meta" },
                    formatViews(video.viewCount) + " · " + (extraMeta || timeAgo(video.uploadedAt))))));
}

/** Small side-by-side card used in "Up next". */
function compactCard(video) {
    return el("div", { class: "compact" },
        thumbnail(video, { href: watchUrl(video.id), iconSize: 24 }),
        el("div", { class: "card-text" },
            el("a", { href: watchUrl(video.id), class: "card-title" }, video.title),
            el("div", { class: "card-meta" }, el("a", { href: channelUrl(video.channelId) }, video.channelName)),
            el("div", { class: "card-meta" }, formatViews(video.viewCount) + " · " + timeAgo(video.uploadedAt))));
}

function stateBox(title, message, action) {
    return el("div", { class: "state" },
        el("h2", {}, title),
        message ? el("div", {}, message) : null,
        action || null);
}

let toastTimer = null;
function toast(message) {
    let node = document.getElementById("toast");
    if (!node) {
        node = el("div", { id: "toast", class: "toast", role: "status" });
        document.body.append(node);
    }
    node.textContent = message;
    node.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { node.hidden = true; }, 2500);
}

/** A button that asks for a second click before doing something destructive. */
function confirmButton(label, confirmLabel, onConfirm, className) {
    const text = document.createTextNode(label);
    const button = el("button", { type: "button", class: className || "btn small danger" }, icon("trash", 16), text);
    let armed = false;
    let timer = null;
    button.addEventListener("click", async () => {
        if (!armed) {
            armed = true;
            text.textContent = confirmLabel;
            timer = setTimeout(() => { armed = false; text.textContent = label; }, 4000);
            return;
        }
        clearTimeout(timer);
        button.disabled = true;
        try {
            await onConfirm();
        } finally {
            button.disabled = false;
            armed = false;
            text.textContent = label;
        }
    });
    return button;
}

// ---------- Page shell ----------

const NAV_ITEMS = [
    { key: "home", href: "/", label: "Home", icon: "home" },
    { key: "trending", href: "/?tab=trending", label: "Trending", icon: "trending" },
    { key: "subscriptions", href: "/?tab=subscriptions", label: "Subscriptions", icon: "subscriptions" },
    { key: "history", href: "/?tab=history", label: "History", icon: "history" },
    { key: "studio", href: "/studio.html", label: "Your studio", icon: "studio" }
];

/**
 * Fills the top bar (#topbar), side menu (#sidebar) and phone tab bar (#bottom-nav).
 * active is the NAV_ITEMS key of the current page, or null.
 */
function renderShell(active, options = {}) {
    const topbar = document.getElementById("topbar");
    if (topbar) renderTopbar(topbar, options);
    const sidebar = document.getElementById("sidebar");
    if (sidebar) renderSidebar(sidebar, active);
    const bottom = document.getElementById("bottom-nav");
    if (bottom) renderBottomNav(bottom, active);
}

function brand(subtitle) {
    return el("a", { href: "/", class: "brand", "aria-label": "JanManch home" },
        el("span", { class: "brand-mark" }, icon("play", 16)),
        el("span", { class: "brand-name" }, "JanManch", subtitle ? el("span", { class: "sub" }, " " + subtitle) : null));
}

function renderTopbar(topbar, options) {
    const query = new URLSearchParams(location.search).get("q") || "";
    const input = el("input", { id: "search-input", type: "search", name: "q", value: query,
        placeholder: "Search videos or #tags", autocomplete: "off" });
    const search = el("form", { class: "search", role: "search", action: "/", method: "get" },
        el("label", { for: "search-input", class: "sr-only" }, "Search JanManch"),
        input,
        el("button", { type: "submit", "aria-label": "Search" }, icon("search", 18)));

    const actions = el("div", { class: "top-actions" });
    if (getToken()) {
        actions.append(el("a", { href: "/upload.html", class: "btn filled" }, icon("plus", 18),
            el("span", { class: "upload-label" }, "Upload")));
        const menu = el("div", { class: "account-menu", hidden: true, role: "menu" },
            el("div", { class: "who" }, el("div", { style: { fontWeight: "700" } }, getName()),
                el("div", { class: "muted small" }, "Signed in")),
            el("a", { href: "/studio.html", role: "menuitem" }, icon("studio", 18), "Your studio"),
            el("a", { href: "/?tab=history", role: "menuitem" }, icon("history", 18), "History"),
            el("button", { type: "button", role: "menuitem", onclick: logout }, icon("logout", 18), "Log out"));
        const trigger = el("button", { type: "button", class: "icon-btn", "aria-label": "Account menu",
            "aria-haspopup": "true", "aria-expanded": "false" }, avatar(getName(), 34));
        trigger.addEventListener("click", event => {
            event.stopPropagation();
            menu.hidden = !menu.hidden;
            trigger.setAttribute("aria-expanded", String(!menu.hidden));
        });
        document.addEventListener("click", event => {
            if (!menu.hidden && !menu.contains(event.target)) {
                menu.hidden = true;
                trigger.setAttribute("aria-expanded", "false");
            }
        });
        menu.addEventListener("keydown", event => {
            if (event.key === "Escape") { menu.hidden = true; trigger.focus(); }
        });
        actions.append(el("div", { class: "account" }, trigger, menu));
    } else {
        actions.append(el("a", { href: loginUrl(), class: "btn primary" }, "Log in"));
    }

    topbar.replaceChildren(brand(options.subtitle), options.hideSearch ? el("span", { style: { flex: "1" } }) : search, actions);
}

function renderSidebar(sidebar, active) {
    sidebar.replaceChildren();
    sidebar.setAttribute("aria-label", "Main");
    for (const item of NAV_ITEMS) {
        sidebar.append(el("a", { href: item.href, class: "nav-link" + (item.key === active ? " active" : ""),
            "aria-current": item.key === active ? "page" : null },
            icon(item.icon, 20), el("span", { class: "name" }, item.label)));
    }
    if (!getToken()) return;
    const block = el("div", { class: "subs-block" }, el("hr"), el("div", { class: "sidebar-label" }, "Subscriptions"));
    sidebar.append(block);
    api("/users/me/subscriptions").then(channels => {
        if (!channels || !channels.length) {
            block.append(el("div", { class: "muted small", style: { padding: "0 14px" } },
                "Channels you subscribe to appear here."));
            return;
        }
        for (const channel of channels.slice(0, 12)) {
            block.append(el("a", { href: channelUrl(channel.id), class: "nav-link" },
                avatar(channel.name, 26), el("span", { class: "name" }, channel.name)));
        }
    }).catch(() => { /* the menu still works without the list */ });
}

function renderBottomNav(bottom, active) {
    const item = (key, href, label, iconName) =>
        el("a", { href, class: key === active ? "active" : null, "aria-current": key === active ? "page" : null },
            icon(iconName, 22), label);
    bottom.setAttribute("aria-label", "Main");
    bottom.replaceChildren(
        item("home", "/", "Home", "home"),
        item("trending", "/?tab=trending", "Trending", "trending"),
        el("a", { href: getToken() ? "/upload.html" : loginUrl(), "aria-label": "Upload" },
            el("span", { class: "upload-pill" }, icon("plus", 22, 2.4))),
        item("subscriptions", "/?tab=subscriptions", "Subscriptions", "subscriptions"),
        item("studio", "/studio.html", "Studio", "studio"));
}
