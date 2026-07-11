(function () {
  globalThis.escalaIciDownloadBytesBase64 = function (url, timeoutMs, callback) {
    var settled = false;
    var done = function (status, payload) {
      if (settled) return;
      settled = true;
      callback(status, payload);
    };

    var controller = new AbortController();
    var timer = setTimeout(function () {
      controller.abort();
    }, timeoutMs);

    fetch(url, { signal: controller.signal, mode: "cors" })
      .then(function (response) {
        clearTimeout(timer);
        if (!response.ok) {
          done("error", "HTTP " + response.status);
          return null;
        }
        return response.arrayBuffer();
      })
      .then(function (buffer) {
        if (buffer == null) return;
        var bytes = new Uint8Array(buffer);
        var binary = "";
        for (var i = 0; i < bytes.length; i++) {
          binary += String.fromCharCode(bytes[i]);
        }
        done("success", btoa(binary));
      })
      .catch(function (error) {
        clearTimeout(timer);
        done("error", error && error.message ? error.message : "Falha ao baixar o arquivo.");
      });
  };
})();
