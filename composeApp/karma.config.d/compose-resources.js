// Wasm UI tests: serve Compose resources (strings, fonts), fetched from /composeResources/.
config.files.push({
    pattern: config.basePath + "/kotlin/composeResources/**",
    included: false,
    served: true,
    watched: false
});
config.proxies["/composeResources/"] = "/base/kotlin/composeResources/";
