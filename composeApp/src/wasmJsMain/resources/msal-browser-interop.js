(function () {
  var msalApplication = null;
  var loginInFlight = false;

  function rejection(kind, detail) {
    return { kind: kind, detail: detail || "Falha ao autenticar com a conta corporativa." };
  }

  function safeErrorText(error) {
    if (!error) return "";
    var code = typeof error.errorCode === "string" ? error.errorCode : "";
    var message = typeof error.message === "string" ? error.message : "";
    var name = typeof error.name === "string" ? error.name : "";
    return [code, name, message].filter(Boolean).join(" ");
  }

  function lowerText(error) {
    return safeErrorText(error).toLowerCase();
  }

  function accountToIdentityJson(account) {
    if (!account) return null;
    var claims = account.idTokenClaims || {};
    var username = account.username || "";
    return JSON.stringify({
      tenantId: account.tenantId || "",
      objectId: claims.oid || account.homeAccountId || "",
      username: username,
      displayName: claims.name || username,
      email: claims.preferred_username || username,
      accountId: account.homeAccountId || ""
    });
  }

  function scopesFromCsv(scopesCsv) {
    return String(scopesCsv || "")
      .split(",")
      .map(function (scope) { return scope.trim(); })
      .filter(Boolean);
  }

  function requireApp(missingKind) {
    if (!msalApplication) {
      return Promise.reject(rejection(missingKind || "invalid_configuration", "MSAL Web ainda não foi inicializado."));
    }
    return Promise.resolve(msalApplication);
  }

  function activeAccount() {
    if (!msalApplication) return null;
    var accounts = msalApplication.getAllAccounts();
    var account = accounts && accounts.length ? accounts[0] : null;
    if (account && typeof msalApplication.setActiveAccount === "function") {
      msalApplication.setActiveAccount(account);
    }
    return account;
  }

  function classifySilentError(error) {
    var text = lowerText(error);
    var detail = safeErrorText(error) || "Falha ao renovar a sessão corporativa.";
    if (text.indexOf("no_account_error") >= 0 || text.indexOf("no account") >= 0 || text.indexOf("no_account_found") >= 0) {
      return rejection("account_not_found", detail);
    }
    if (text.indexOf("interaction_required") >= 0 || text.indexOf("interactionrequired") >= 0 || text.indexOf("login_required") >= 0 || text.indexOf("consent_required") >= 0) {
      return rejection("interaction_required", detail);
    }
    if (text.indexOf("network_error") >= 0 || text.indexOf("no_network_connectivity") >= 0 || text.indexOf("post_request_failed") >= 0 || text.indexOf("get_request_failed") >= 0 || text.indexOf("failed to fetch") >= 0 || text.indexOf("network") >= 0) {
      return rejection("network_error", detail);
    }
    return rejection("unknown", detail);
  }

  function classifyLoginError(error) {
    var text = lowerText(error);
    var detail = safeErrorText(error) || "Falha ao entrar com a conta corporativa.";
    // BrowserAuthErrorCodes observados no pacote: popup_window_error, empty_window_error,
    // user_cancelled, interaction_in_progress e monitor_popup_timeout.
    if (text.indexOf("popup") >= 0 && (text.indexOf("blocked") >= 0 || text.indexOf("failed to open") >= 0 || text.indexOf("popup_window_error") >= 0 || text.indexOf("empty_window_error") >= 0)) {
      return rejection("popup_blocked", detail);
    }
    if (text.indexOf("user_cancelled") >= 0 || text.indexOf("user_canceled") >= 0 || text.indexOf("popup closed") >= 0 || text.indexOf("user cancelled") >= 0 || text.indexOf("user canceled") >= 0) {
      return rejection("cancelled", detail);
    }
    if (text.indexOf("unauthorized_client") >= 0 || text.indexOf("invalid_client") >= 0 || text.indexOf("aadsts500") >= 0) {
      return rejection("tenant_not_allowed", detail);
    }
    if (text.indexOf("aadsts9002326") >= 0 || text.indexOf("50011") >= 0 || text.indexOf("redirect_uri_mismatch") >= 0 || text.indexOf("redirect uri") >= 0 || text.indexOf("redirect_uri") >= 0) {
      return rejection("invalid_configuration", detail);
    }
    if (text.indexOf("network_error") >= 0 || text.indexOf("no_network_connectivity") >= 0 || text.indexOf("post_request_failed") >= 0 || text.indexOf("get_request_failed") >= 0 || text.indexOf("failed to fetch") >= 0 || text.indexOf("network") >= 0) {
      return rejection("network_error", detail);
    }
    return rejection("unknown", detail);
  }

  globalThis.escalaIciMsalInit = function (tenantId, clientId, redirectUri) {
    if (msalApplication) return Promise.resolve(true);
    if (!window.msal || typeof window.msal.createStandardPublicClientApplication !== "function") {
      return Promise.reject(rejection("invalid_configuration", "Biblioteca MSAL Web indisponível."));
    }
    return window.msal.createStandardPublicClientApplication({
      auth: {
        clientId: clientId,
        authority: "https://login.microsoftonline.com/" + tenantId,
        redirectUri: redirectUri
      },
      cache: {
        cacheLocation: "localStorage"
      }
    }).then(function (app) {
      msalApplication = app;
      activeAccount();
      return true;
    }).catch(function (error) {
      return Promise.reject(classifyLoginError(error));
    });
  };

  globalThis.escalaIciMsalGetActiveIdentityJson = function () {
    return accountToIdentityJson(activeAccount());
  };

  globalThis.escalaIciMsalAcquireTokenSilent = function (scopesCsv) {
    return requireApp("unknown").then(function (app) {
      var account = activeAccount();
      if (!account) {
        return Promise.reject(rejection("account_not_found", "Nenhuma conta corporativa ativa encontrada."));
      }
      return app.acquireTokenSilent({
        scopes: scopesFromCsv(scopesCsv),
        account: account
      }).then(function (result) {
        if (result && result.account && typeof app.setActiveAccount === "function") {
          app.setActiveAccount(result.account);
        }
        var json = accountToIdentityJson((result && result.account) || activeAccount());
        if (!json) {
          return Promise.reject(rejection("account_not_found", "Nenhuma conta corporativa ativa encontrada."));
        }
        return json;
      }).catch(function (error) {
        return Promise.reject(classifySilentError(error));
      });
    });
  };

  globalThis.escalaIciMsalLoginPopup = function (scopesCsv) {
    if (loginInFlight) {
      return Promise.reject(rejection("already_in_progress", "Login já em andamento."));
    }
    loginInFlight = true;
    return requireApp().then(function (app) {
      return app.loginPopup({ scopes: scopesFromCsv(scopesCsv) }).then(function (result) {
        if (result && result.account && typeof app.setActiveAccount === "function") {
          app.setActiveAccount(result.account);
        }
        var json = accountToIdentityJson((result && result.account) || activeAccount());
        if (!json) {
          return Promise.reject(rejection("account_not_found", "Nenhuma conta corporativa ativa encontrada."));
        }
        return json;
      });
    }).catch(function (error) {
      if (error && typeof error.kind === "string") {
        return Promise.reject(error);
      }
      return Promise.reject(classifyLoginError(error));
    }).finally(function () {
      loginInFlight = false;
    });
  };

  globalThis.escalaIciMsalLogoutPopup = function () {
    return requireApp().then(function (app) {
      return app.logoutPopup().then(function () {
        return true;
      }).catch(function () {
        return true;
      });
    }).catch(function () {
      return true;
    });
  };
})();
