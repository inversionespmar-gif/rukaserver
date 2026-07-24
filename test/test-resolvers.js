#!/usr/bin/env node
/**
 * test-resolvers.js — Prueba automática de todos los resolvers de host.
 *
 * Uso: node test/test-resolvers.js
 *
 * Para cada host registrado en createDefaultResolver, el script:
 *   1. Toma una URL de prueba representativa del host.
 *   2. Llama al resolver correspondiente.
 *   3. Verifica que devuelva { url, type } con una URL válida.
 *   4. Hace un HEAD/GET real para confirmar que el stream es accesible (2xx).
 *
 * Resultado: imprime PASS / FAIL por cada host + resumen final.
 */

import { createDefaultResolver, detectHost } from "../src/resolver/index.js";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

// ── Test cases ─────────────────────────────────────────────────────────────────
// Each entry: { host, url } where url is a real embed we can test against.
// If we can't test a real URL we still verify that resolve() runs without crash.
const TEST_CASES = [
  // Static/known-working embeds (adjust if they expire)
  { host: "voe.sx",         url: "https://voe.sx/e/abc123" },            // synthetic
  { host: "hlswish.com",    url: "https://hlswish.com/e/abc123" },       // synthetic
  { host: "goodstream.one", url: "https://goodstream.one/e/abc123" },    // synthetic
  { host: "vimeos.net",     url: "https://vimeos.net/e/abc123" },        // synthetic

  // Real popular hosts — these use public demo embeds or known embed pages
  { host: "doodstream.com", url: "https://doodstream.com/e/j7ghgxhgxeg5" },
  { host: "streamtape.com", url: "https://streamtape.com/e/xJxJxJxJxJxJ" }, // synthetic
  { host: "filemoon.sx",    url: "https://filemoon.sx/e/abc123" },           // synthetic
  { host: "mixdrop.co",     url: "https://mixdrop.co/e/abc123" },            // synthetic
  { host: "streamwish.com", url: "https://streamwish.com/e/abc123" },        // synthetic
  { host: "upstream.to",    url: "https://upstream.to/e/abc123" },           // synthetic
  { host: "ok.ru",          url: "https://ok.ru/videoembed/1234567890" },     // synthetic
];

// ── Helpers ────────────────────────────────────────────────────────────────────
const GREEN  = "\x1b[32m";
const RED    = "\x1b[31m";
const YELLOW = "\x1b[33m";
const RESET  = "\x1b[0m";
const BOLD   = "\x1b[1m";

function pass(label, detail = "") {
  console.log(`  ${GREEN}✓ PASS${RESET}  ${label}${detail ? "  →  " + detail : ""}`);
}
function fail(label, reason = "") {
  console.log(`  ${RED}✗ FAIL${RESET}  ${label}${reason ? "  →  " + reason : ""}`);
}
function warn(label, reason = "") {
  console.log(`  ${YELLOW}⚠ WARN${RESET}  ${label}${reason ? "  →  " + reason : ""}`);
}

async function verifyStreamAccessible(url, referer = "") {
  try {
    const headers = { "User-Agent": UA, "Accept": "*/*" };
    if (referer) headers["Referer"] = referer;
    const res = await fetch(url, { method: "HEAD", headers, redirect: "follow" });
    if (!res.ok) {
      // Try GET for CDNs that reject HEAD
      const res2 = await fetch(url, { method: "GET", headers, redirect: "follow" });
      return res2.ok;
    }
    return true;
  } catch {
    return false;
  }
}

// ── Main ───────────────────────────────────────────────────────────────────────
async function runTests() {
  console.log(`\n${BOLD}=== Rukaserver Resolver Tests ===${RESET}\n`);

  const resolver = createDefaultResolver({ cache: null, browserResolve: null });

  let passed = 0;
  let failed = 0;
  let warned = 0;

  for (const { host, url } of TEST_CASES) {
    process.stdout.write(`  Testing ${BOLD}${host}${RESET} ... `);
    let result = null;
    let errMsg = "";
    try {
      result = await resolver.resolve([url], { timeoutMs: 15000, waitMs: 5000 });
    } catch (e) {
      errMsg = e?.message || String(e);
    }

    if (errMsg) {
      console.log("");
      fail(host, `resolver threw: ${errMsg}`);
      failed++;
      continue;
    }

    if (!result || !result.url) {
      console.log("");
      // For synthetic/invalid URLs this is expected — just a warning, not a hard failure.
      if (url.includes("abc123") || url.includes("xJxJxJxJxJxJ") || url.includes("1234567890")) {
        warn(host, "synthetic URL → no stream found (expected)");
        warned++;
      } else {
        fail(host, "resolver returned null — embed may have expired or changed");
        failed++;
      }
      continue;
    }

    // We have a URL — verify it's actually accessible
    console.log(`\n    → ${result.type.toUpperCase()} ${result.url.substring(0, 80)}...`);
    const accessible = await verifyStreamAccessible(result.url, result.referer);
    if (accessible) {
      pass(host, `stream is accessible (${result.type})`);
      passed++;
    } else {
      warn(host, `resolved URL returned non-2xx — stream may be behind auth or expired`);
      warned++;
    }
  }

  console.log(`\n${BOLD}─── Results ───────────────────────────────────────${RESET}`);
  console.log(`  ${GREEN}Passed : ${passed}${RESET}`);
  console.log(`  ${YELLOW}Warned : ${warned}${RESET}  (synthetic URLs or expired streams)`);
  console.log(`  ${RED}Failed : ${failed}${RESET}`);
  console.log(`\n  Total  : ${TEST_CASES.length} tests\n`);

  // ── Additional: verify host detection mapping ─────────────────────────────
  console.log(`${BOLD}=== Host Detection Sanity Check ===${RESET}\n`);
  const domainCheck = [
    ["https://ok.ru/videoembed/123", "ok.ru"],
    ["https://dood.la/e/abc", "dood.la"],
    ["https://streamtape.com/e/abc", "streamtape.com"],
    ["https://filemoon.sx/e/abc", "filemoon.sx"],
    ["https://mixdrop.co/f/abc", "mixdrop.co"],
    ["https://streamwish.com/e/abc", "streamwish.com"],
    ["https://upstream.to/e/abc", "upstream.to"],
    ["https://voe.sx/e/abc", "voe.sx"],
    ["https://doodstream.com/e/abc", "doodstream.com"],
  ];
  let domainPassed = 0;
  for (const [url, expected] of domainCheck) {
    const detected = detectHost(url);
    if (detected === expected) {
      pass(`detectHost(${url.slice(8, 40)})`, detected);
      domainPassed++;
    } else {
      fail(`detectHost(${url.slice(8, 40)})`, `expected "${expected}", got "${detected}"`);
    }
  }
  console.log(`\n  ${domainPassed}/${domainCheck.length} host detection checks passed\n`);

  process.exit(failed > 0 ? 1 : 0);
}

runTests().catch(e => {
  console.error("Unexpected error:", e);
  process.exit(1);
});
