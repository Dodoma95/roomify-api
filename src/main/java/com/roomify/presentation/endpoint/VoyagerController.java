package com.roomify.presentation.endpoint;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoyagerController {

    @GetMapping(value = "/voyager", produces = MediaType.TEXT_HTML_VALUE)
    public String voyager() {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>Roomify API — GraphQL Voyager</title>
                  <link rel="stylesheet" href="/voyager/voyager.css"/>
                  <style>
                    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                    body { overflow: hidden; }

                    #token-bar {
                      position: fixed; top: 0; left: 0; right: 0; z-index: 9999;
                      background: #1a1a2e; padding: 8px 16px;
                      display: flex; align-items: center; gap: 10px;
                      font-family: system-ui, sans-serif; font-size: 13px; color: #fff;
                      border-bottom: 2px solid #e535ab;
                    }
                    #token-bar label { white-space: nowrap; font-weight: 600; }
                    #token-input {
                      flex: 1; padding: 5px 10px; border-radius: 4px;
                      border: 1px solid #444; background: #2a2a3e; color: #fff;
                      font-size: 13px; font-family: monospace;
                    }
                    #token-input::placeholder { color: #555; }
                    #load-btn {
                      padding: 5px 14px; border-radius: 4px; border: none;
                      background: #e535ab; color: #fff; cursor: pointer;
                      font-size: 13px; font-weight: 600; white-space: nowrap;
                    }
                    #load-btn:hover { background: #c0208a; }
                    #status { font-size: 12px; color: #aaa; white-space: nowrap; }

                    #voyager {
                      height: calc(100vh - 42px);
                      margin-top: 42px;
                    }
                    #voyager-hint {
                      display: flex; align-items: center; justify-content: center;
                      height: 100%;
                      color: #5d7e86; font-family: system-ui, sans-serif; font-size: 1.1rem;
                    }
                  </style>
                </head>
                <body>

                <div id="token-bar">
                  <label for="token-input">🔑 Bearer JWT</label>
                  <input id="token-input" type="password"
                         placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9…"
                         autocomplete="off"/>
                  <button id="load-btn">Charger le schéma</button>
                  <span id="status"></span>
                </div>

                <div id="voyager">
                  <div id="voyager-hint">Entrez votre token JWT puis cliquez <strong>&nbsp;Charger le schéma</strong></div>
                </div>

                <script src="/voyager/voyager.standalone.js"></script>
                <script>
                  var STORAGE_KEY = 'roomify_voyager_token';
                  var tokenInput  = document.getElementById('token-input');
                  var loadBtn     = document.getElementById('load-btn');
                  var statusEl    = document.getElementById('status');
                  var mountEl     = document.getElementById('voyager');

                  var saved = localStorage.getItem(STORAGE_KEY);
                  if (saved) tokenInput.value = saved;

                  function loadVoyager(token) {
                    localStorage.setItem(STORAGE_KEY, token);
                    statusEl.textContent = 'Chargement…';
                    statusEl.style.color = '#aaa';
                    loadBtn.disabled = true;

                    fetch('/graphql', {
                      method: 'POST',
                      headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token,
                      },
                      body: JSON.stringify({ query: GraphQLVoyager.voyagerIntrospectionQuery }),
                    })
                    .then(function(res) {
                      if (!res.ok) throw new Error('HTTP ' + res.status + ' — token invalide ?');
                      return res.json();
                    })
                    .then(function(introspection) {
                      if (introspection.errors) throw new Error(introspection.errors[0].message);

                      mountEl.innerHTML = '';
                      GraphQLVoyager.init(mountEl, { introspection: introspection });

                      statusEl.textContent = '\\u2705 Sch\\u00e9ma charg\\u00e9';
                      statusEl.style.color = '#6fcf97';
                    })
                    .catch(function(err) {
                      statusEl.textContent = '\\u274c ' + err.message;
                      statusEl.style.color = '#eb5757';
                    })
                    .finally(function() {
                      loadBtn.disabled = false;
                    });
                  }

                  loadBtn.addEventListener('click', function() {
                    var token = tokenInput.value.trim();
                    if (!token) {
                      statusEl.textContent = '\\u26a0\\ufe0f Token requis';
                      statusEl.style.color = '#f2c94c';
                      return;
                    }
                    loadVoyager(token);
                  });

                  tokenInput.addEventListener('keydown', function(e) {
                    if (e.key === 'Enter') loadBtn.click();
                  });

                  if (saved) loadVoyager(saved);
                </script>
                </body>
                </html>
                """;
    }
}
