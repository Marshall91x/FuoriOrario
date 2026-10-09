// Wasm UI tests run longer than Mocha's 2s default on CI runners.
config.client = config.client || {};
config.client.mocha = Object.assign(config.client.mocha || {}, { timeout: 30000 });
