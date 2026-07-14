import { test } from "node:test";
import assert from "node:assert/strict";
import { createAuth } from "../src/auth.js";

function makeRepo(user) {
  return { async findByCredentials() { return user; } };
}

test("authenticate returns auth:1 for valid active user", async () => {
  const future = Math.floor(Date.now() / 1000) + 86400;
  const auth = createAuth(makeRepo({ username: "u", password: "p", status: true, exp_date: future, max_connections: 1 }));
  const r = await auth.authenticate("u", "p");
  assert.equal(r.auth, 1);
  assert.equal(r.user.username, "u");
});

test("authenticate returns auth:0 when credentials wrong", async () => {
  const auth = createAuth(makeRepo(null));
  const r = await auth.authenticate("u", "bad");
  assert.equal(r.auth, 0);
  assert.match(r.message, /Invalid/);
});

test("authenticate returns auth:0 when expired", async () => {
  const past = Math.floor(Date.now() / 1000) - 100;
  const auth = createAuth(makeRepo({ status: true, exp_date: past }));
  const r = await auth.authenticate("u", "p");
  assert.equal(r.auth, 0);
  assert.match(r.message, /expired/i);
});

test("authenticate returns auth:0 when disabled", async () => {
  const future = Math.floor(Date.now() / 1000) + 86400;
  const auth = createAuth(makeRepo({ status: false, exp_date: future }));
  const r = await auth.authenticate("u", "p");
  assert.equal(r.auth, 0);
  assert.match(r.message, /disabled/i);
});

test("authenticate returns auth:0 when credentials missing", async () => {
  const auth = createAuth(makeRepo(null));
  const r = await auth.authenticate("", "");
  assert.equal(r.auth, 0);
  assert.match(r.message, /Missing credentials/);
});

test("authenticate returns auth:0 on database error", async () => {
  const throwingRepo = { async findByCredentials() { throw new Error("boom"); } };
  const auth = createAuth(throwingRepo);
  const r = await auth.authenticate("u", "p");
  assert.equal(r.auth, 0);
  assert.match(r.message, /Database error/);
});
