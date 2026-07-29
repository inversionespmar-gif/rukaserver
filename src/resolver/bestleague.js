const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

const CACHE_TTL_MS = 30 * 60 * 1000;
const tokenCache = new Map();

const CHANNEL_NUMBERS = {
  "V2FybmVySEQ=": 7, "R0VOX1RW": 7, "VG9kb05vdGljaWFz": 7,
  "VHlDU3BvcnQ": 7, "QW1lcmljYTI0": 7, "QzVO": 7,
  "TGFfTmFjaW9u": 7, "Q3JvbmljYVRW": 7, "Q2FuYWxfOF9UdWN1bWFu": 7,
  "UGFyYWd1YXlfVFY=": 7, "UGFyYW1vdW50": 7, "Q29tZWR5Q2VudHJhbA": 7,
  "Qm9vbWVyYW5n": 7, "RHJlYW13b3Jrcw==": 7, "QW5pbWFsUGxhbmV0": 7,
  "SGlzdG9yeUhE": 7, "SUQ=": 7, "QnJhdm9UVg==": 7,
  "TkJBX1RW": 7, "SFRW": 7, "Rmxvd19NdXNpY18z": 7,
  "U29ueUhE": 6, "VHJ1VFY=": 6, "SEJPX1BPUA==": 6,
  "Rm94U3BvcnRzMl9VWQ==": 7, "Rm94U3BvcnRzM19VWQ==": 7,
  "RVNQTjQ=": 7, "RVNQTl9VWQ==": 7, "RVNQTjJfVVk=": 7,
  "Q2FuYWxfNV9Sb3Nhcmlv": 4, "Q2FuYWxfOF9DQkE": 6,
  "QTNfU2VyaWVz": 7, "VVNBX05ldHdvcms=": 7,
  "RHNwb3J0c19VWQ==": 7, "RHNwb3J0czJfVVk=": 7,
  "RHNwb3J0c19QbHVzX1VZ": 7,
};

function fetchText(url, timeoutMs = 15000) {
  return fetch(url, {
    headers: { "User-Agent": UA, "Accept": "*/*" },
    redirect: "follow",
    signal: AbortSignal.timeout(timeoutMs),
  }).then((r) => (r.ok ? r.text() : null));
}

function extractIframeSrc(html) {
  const m = html.match(/<iframe[^>]+src="([^"]+)"/i);
  if (!m) return null;
  let url = m[1].trim();
  if (url.startsWith("https://")) return url;
  if (url.startsWith("//")) return "https:" + url;
  return url;
}

function extractGetParam(url) {
  const m = url.match(/[?&]get=([^&"\s]+)/);
  return m ? m[1] : null;
}

function parseCdnTokens(html) {
  const tokens = [];
  const arrayMatch = html.match(/var mt = \[([\s\S]*?)\];/);
  if (!arrayMatch) return tokens;
  const block = arrayMatch[1];
  const objRe = /["']?cdn["']?\s*:\s*"([^"]+)"[\s\S]*?["']?token["']?\s*:\s*"([^"]+)"/g;
  let m;
  while ((m = objRe.exec(block))) {
    tokens.push({ cdn: m[1], token: m[2] });
  }
  return tokens;
}

function buildMpdUrl(cdn, token, number, channelName) {
  return `https://${cdn}.cvattv.com.ar/${token}/live/c${number}eds/${channelName}/SA_Live_dash_enc/${channelName}.mpd`;
}

export async function resolveBestLeague(embedUrl) {
  const cached = tokenCache.get(embedUrl);
  if (cached && Date.now() - cached.ts < CACHE_TTL_MS) {
    return cached.mpdUrl;
  }

  const html = await fetchText(embedUrl);
  if (!html) return null;

  const iframeSrc = extractIframeSrc(html);
  if (!iframeSrc) return null;

  const getParam = extractGetParam(iframeSrc);
  if (!getParam) return null;

  const channelName = Buffer.from(getParam, "base64").toString("utf-8");
  const number = CHANNEL_NUMBERS[getParam] ?? 3;

  const tokHtml = await fetchText(iframeSrc);
  if (!tokHtml) return null;

  const cdnTokens = parseCdnTokens(tokHtml);
  if (!cdnTokens.length) return null;

  const selected = cdnTokens[Math.floor(Math.random() * cdnTokens.length)];
  const mpdUrl = buildMpdUrl(selected.cdn, selected.token, number, channelName);

  tokenCache.set(embedUrl, { mpdUrl, ts: Date.now() });
  return mpdUrl;
}

export function isBestLeagueUrl(url) {
  return /embed\.php|telelibrefull|bestleague\.life|bestleague\.top/i.test(url);
}
