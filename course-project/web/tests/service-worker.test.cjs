const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

test('页面壳网络优先、离线回退、缓存升级及API隔离', async () => {
  const handlers = new Map();
  const stores = new Map([['feynman-shell-v1', new Map()], ['other-application', new Map()]]);
  const keyFor = (request) => new URL(typeof request === 'string' ? request : request.url, 'https://app.test').href;
  const caches = {
    keys: async () => [...stores.keys()],
    delete: async (name) => stores.delete(name),
    open: async (name) => {
      if (!stores.has(name)) stores.set(name, new Map());
      const store = stores.get(name);
      return {
        addAll: async (urls) => urls.forEach((url) => store.set(keyFor(url), new Response('offline shell'))),
        match: async (request) => store.get(keyFor(request))?.clone(),
        put: async (request, response) => store.set(keyFor(request), response)
      };
    }
  };
  let offline = false;
  let networkCalls = 0;
  const self = { location: { origin: 'https://app.test' }, clients: { claim: async () => {} }, skipWaiting() {}, addEventListener: (type, handler) => handlers.set(type, handler) };
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../public/sw.js'), 'utf8'), {
    self, caches, Response, URL,
    fetch: async () => { networkCalls++; if (offline) throw new Error('offline'); return new Response('latest shell'); }
  });
  for (const type of ['install', 'activate']) {
    let completion;
    handlers.get(type)({ waitUntil: (promise) => { completion = promise; } });
    await completion;
  }
  assert.equal(stores.has('feynman-shell-v1'), false);
  assert.equal(stores.has('other-application'), true);
  const dispatch = (url, mode = 'navigate', method = 'GET') => {
    let completion;
    handlers.get('fetch')({ request: { url, method, mode }, respondWith: (promise) => { completion = promise; } });
    return completion;
  };
  assert.equal(await (await dispatch('https://app.test/knowledge')).text(), 'latest shell');
  offline = true;
  assert.equal(await (await dispatch('https://app.test/knowledge')).text(), 'latest shell');
  assert.equal(await (await dispatch('https://app.test/not-cached')).text(), 'offline shell');
  assert.equal((await dispatch('https://app.test/assets/not-cached.js', 'cors')).type, 'error');
  assert.equal(dispatch('https://app.test/api/knowledge'), undefined);
  assert.equal(dispatch('https://external.test/asset.js', 'cors'), undefined);
  assert.equal(dispatch('https://app.test/knowledge', 'cors', 'POST'), undefined);
  assert.equal(networkCalls, 4);
});
