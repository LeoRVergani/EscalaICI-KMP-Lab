(function () {
  var TOKEN_STORAGE_KEY = "escalaIciDropboxToken";

  function getStoredToken() {
    try {
      var raw = localStorage.getItem(TOKEN_STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function storeToken(token) {
    localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(token));
  }

  function clearToken() {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }

  function base64UrlEncode(bytes) {
    var binary = "";
    for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
    return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }

  function randomBase64Url(byteLength) {
    var bytes = new Uint8Array(byteLength);
    crypto.getRandomValues(bytes);
    return base64UrlEncode(bytes);
  }

  async function generatePkce() {
    var verifier = randomBase64Url(64);
    var digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
    var challenge = base64UrlEncode(new Uint8Array(digest));
    return { verifier: verifier, challenge: challenge };
  }

  function openAuthPopup(authUrl, expectedState) {
    return new Promise(function (resolve, reject) {
      var popup = window.open(authUrl, "escala-ici-dropbox-oauth", "width=480,height=680");
      if (!popup) {
        reject(new Error("O navegador bloqueou a janela de autorização do Dropbox. Permita pop-ups para este site e tente novamente."));
        return;
      }
      var settled = false;

      var timer = setTimeout(function () {
        finish(function () {
          reject(new Error("Tempo esgotado aguardando a autorização do Dropbox."));
        });
      }, 120000);

      var poll = setInterval(function () {
        var isClosed = false;
        try {
          isClosed = popup.closed;
        } catch (e) {
          return;
        }
        if (isClosed) {
          finish(function () {
            reject(new Error("Janela de autorização do Dropbox fechada antes de concluir."));
          });
        }
      }, 500);

      function finish(action) {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        clearInterval(poll);
        window.removeEventListener("message", onMessage);
        try { popup.close(); } catch (e) {}
        action();
      }

      function onMessage(event) {
        if (event.origin !== window.location.origin) return;
        var data = event.data;
        if (!data || data.type !== "escala-ici-dropbox-oauth-callback") return;
        finish(function () {
          if (data.error) {
            reject(new Error("Dropbox negou a autorização: " + data.error));
          } else if (data.state !== expectedState) {
            reject(new Error("Resposta de autorização do Dropbox inválida (state não confere)."));
          } else {
            resolve(data.code);
          }
        });
      }
      window.addEventListener("message", onMessage);
    });
  }

  async function exchangeCodeForToken(code, verifier, appKey, redirectUri) {
    var body = new URLSearchParams({
      code: code,
      grant_type: "authorization_code",
      client_id: appKey,
      redirect_uri: redirectUri,
      code_verifier: verifier
    });
    var response = await fetch("https://api.dropboxapi.com/oauth2/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString()
    });
    if (!response.ok) {
      throw new Error("Falha ao trocar código de autorização por token do Dropbox (HTTP " + response.status + ").");
    }
    var json = await response.json();
    var expiresAt = Date.now() + (json.expires_in ? json.expires_in * 1000 : 4 * 3600 * 1000) - 60000;
    return {
      access_token: json.access_token,
      refresh_token: json.refresh_token || null,
      expires_at: expiresAt
    };
  }

  async function refreshAccessToken(refreshToken, appKey) {
    var body = new URLSearchParams({
      grant_type: "refresh_token",
      refresh_token: refreshToken,
      client_id: appKey
    });
    var response = await fetch("https://api.dropboxapi.com/oauth2/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString()
    });
    if (!response.ok) {
      throw new Error("Falha ao renovar o token do Dropbox (HTTP " + response.status + ").");
    }
    var json = await response.json();
    var expiresAt = Date.now() + (json.expires_in ? json.expires_in * 1000 : 4 * 3600 * 1000) - 60000;
    return { access_token: json.access_token, refresh_token: refreshToken, expires_at: expiresAt };
  }

  async function authorizeInteractive(appKey, redirectUri, scope) {
    var pkce = await generatePkce();
    var state = randomBase64Url(16);
    var params = new URLSearchParams({
      client_id: appKey,
      response_type: "code",
      redirect_uri: redirectUri,
      token_access_type: "offline",
      scope: scope,
      code_challenge: pkce.challenge,
      code_challenge_method: "S256",
      state: state
    });
    var authUrl = "https://www.dropbox.com/oauth2/authorize?" + params.toString();
    var code = await openAuthPopup(authUrl, state);
    var token = await exchangeCodeForToken(code, pkce.verifier, appKey, redirectUri);
    if (!token.refresh_token) {
      var existing = getStoredToken();
      if (existing && existing.refresh_token) token.refresh_token = existing.refresh_token;
    }
    storeToken(token);
    return token;
  }

  async function getValidToken(appKey, redirectUri, scope) {
    var token = getStoredToken();
    if (token && token.expires_at > Date.now()) {
      return token;
    }
    if (token && token.refresh_token) {
      try {
        token = await refreshAccessToken(token.refresh_token, appKey);
        storeToken(token);
        return token;
      } catch (e) {
        clearToken();
      }
    }
    return await authorizeInteractive(appKey, redirectUri, scope);
  }

  async function callSharedLinkApi(sharedLinkUrl, accessToken) {
    return fetch("https://content.dropboxapi.com/2/sharing/get_shared_link_file", {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + accessToken,
        "Dropbox-API-Arg": JSON.stringify({ url: sharedLinkUrl })
      }
    });
  }

  async function fetchSharedLinkBytesBase64(sharedLinkUrl, appKey, redirectUri, scope) {
    var token = await getValidToken(appKey, redirectUri, scope);
    var response = await callSharedLinkApi(sharedLinkUrl, token.access_token);
    if (response.status === 401) {
      clearToken();
      token = await getValidToken(appKey, redirectUri, scope);
      response = await callSharedLinkApi(sharedLinkUrl, token.access_token);
    }
    if (!response.ok) {
      var text = "";
      try { text = await response.text(); } catch (e) {}
      throw new Error("Dropbox API retornou HTTP " + response.status + (text ? (": " + text.slice(0, 200)) : ""));
    }
    var buffer = await response.arrayBuffer();
    var bytes = new Uint8Array(buffer);
    var binary = "";
    for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
    return btoa(binary);
  }

  globalThis.escalaIciDropboxFetchSharedLinkBytesBase64 = function (sharedLinkUrl, appKey, redirectUri, scope, callback) {
    fetchSharedLinkBytesBase64(sharedLinkUrl, appKey, redirectUri, scope)
      .then(function (base64) { callback("success", base64); })
      .catch(function (error) { callback("error", error && error.message ? error.message : "Falha ao buscar a escala via Dropbox."); });
  };
})();
