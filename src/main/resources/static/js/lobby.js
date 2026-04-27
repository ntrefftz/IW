(() => {
    const cfg = window.lobbyRealtimeConfig || {};
    if (!cfg.code) {
        return;
    }

    const playersList = document.querySelector("#playersList");
    const playerCountBadge = document.querySelector("#playerCountBadge");
    const modeBadge = document.querySelector("#modeBadge");
    const visibilityBadge = document.querySelector("#visibilityBadge");
    const statusBadge = document.querySelector("#statusBadge");

    const modalidadSelect = document.querySelector("#modalidad");
    const publicoRadio = document.querySelector("#publico");
    const privadoRadio = document.querySelector("#privado");

    const ownerOnly = Array.from(document.querySelectorAll("[data-owner-only]"));
    const ownerDisabled = Array.from(document.querySelectorAll("[data-owner-disabled]"));

    const chatForm = document.querySelector("#lobbyChatForm");
    const chatInput = document.querySelector("#lobbyChatInput");
    const chatMessages = document.querySelector("#lobbyChatMessages");
    const chatEmpty = document.querySelector("#lobbyChatEmpty");
    const chatError = document.querySelector("#lobbyChatError");
    const chatButton = chatForm ? chatForm.querySelector("button[type='submit']") : null;

    const state = {
        code: String(cfg.code),
        currentUsername: cfg.currentUsername || "",
        maxPlayers: Number.isInteger(cfg.maxPlayers) ? cfg.maxPlayers : 4,
        host: "",
        players: [],
        modalidad: "UNO",
        privado: false,
        estado: "LOBBY",
        redirecting: false
    };
    let lobbyPollingId = null;

    function absolutePath(path) {
        const base = (config && config.rootUrl ? config.rootUrl : "").replace(/\/+$/, "");
        const normalized = path.startsWith("/") ? path : `/${path}`;
        return `${base}${normalized}`;
    }

    function normalizeState(update) {
        const incoming = update || {};
        const players = Array.isArray(incoming.players)
            ? incoming.players.map(name => (name || "Jugador sin nombre").toString())
            : [];

        return {
            code: String(incoming.code || state.code),
            host: (incoming.host || "").toString(),
            players,
            modalidad: (incoming.modalidad || "UNO").toString(),
            privado: incoming.privado === true,
            estado: (incoming.estado || "LOBBY").toString()
        };
    }

    function sameLobbyCode(a, b) {
        if (!a || !b) {
            return false;
        }
        return String(a).trim().toUpperCase() === String(b).trim().toUpperCase();
    }

    function applyLobbyState(update) {
        const normalized = normalizeState(update);
        state.code = normalized.code;
        state.host = normalized.host;
        state.players = normalized.players;
        state.modalidad = normalized.modalidad;
        state.privado = normalized.privado;
        state.estado = normalized.estado;
        renderLobbyState();

        if (state.estado === "PARTIDA") {
            redirectToGame();
        }
    }

    function renderLobbyState() {
        if (modeBadge) {
            modeBadge.textContent = state.modalidad;
        }
        if (visibilityBadge) {
            visibilityBadge.textContent = state.privado ? "Privada" : "Publica";
        }
        if (statusBadge) {
            statusBadge.textContent = state.estado;
        }
        if (playerCountBadge) {
            playerCountBadge.textContent = `Jugadores: ${state.players.length}/${state.maxPlayers}`;
        }

        if (modalidadSelect) {
            modalidadSelect.value = state.modalidad;
        }
        if (publicoRadio) {
            publicoRadio.checked = !state.privado;
        }
        if (privadoRadio) {
            privadoRadio.checked = state.privado;
        }

        if (playersList) {
            playersList.innerHTML = "";
            for (let i = 0; i < state.maxPlayers; i += 1) {
                const li = document.createElement("li");
                li.className = "list-group-item d-flex justify-content-between align-items-center";

                const playerName = state.players[i];
                const nameSpan = document.createElement("span");
                if (playerName) {
                    nameSpan.textContent = playerName;
                } else {
                    nameSpan.textContent = "Esperando jugador...";
                    nameSpan.className = "text-muted";
                }
                li.appendChild(nameSpan);

                if (playerName && state.host && playerName === state.host) {
                    const ownerBadge = document.createElement("span");
                    ownerBadge.className = "badge text-bg-light border";
                    ownerBadge.textContent = "Owner";
                    li.appendChild(ownerBadge);
                }

                playersList.appendChild(li);
            }
        }

        const isOwner = !!state.currentUsername && !!state.host && state.currentUsername === state.host;
        ownerOnly.forEach(el => el.classList.toggle("d-none", !isOwner));
        ownerDisabled.forEach(el => {
            el.disabled = !isOwner;
        });
    }

    function setChatError(message) {
        if (!chatError) {
            return;
        }
        if (message) {
            chatError.textContent = message;
            chatError.classList.remove("d-none");
        } else {
            chatError.textContent = "";
            chatError.classList.add("d-none");
        }
    }

    function formatTime(value) {
        if (!value) {
            return "";
        }
        const d = new Date(value);
        if (Number.isNaN(d.getTime())) {
            return "";
        }
        return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    }

    function appendChatMessage(message) {
        if (!chatMessages || !message) {
            return;
        }
        if (chatEmpty) {
            chatEmpty.remove();
        }

        const row = document.createElement("div");
        row.className = "mb-2";

        const header = document.createElement("div");
        header.className = "small text-muted";
        const author = (message.from || "Anonimo").toString();
        const time = formatTime(message.sentAt);
        header.textContent = time ? `${author} · ${time}` : author;

        const text = document.createElement("div");
        text.className = "small";
        if (author === state.currentUsername) {
            text.classList.add("text-primary");
        }
        text.textContent = (message.message || "").toString();

        row.appendChild(header);
        row.appendChild(text);
        chatMessages.appendChild(row);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function redirectToGame(path) {
        if (state.redirecting) {
            return;
        }
        state.redirecting = true;
        if (lobbyPollingId !== null) {
            window.clearInterval(lobbyPollingId);
            lobbyPollingId = null;
        }
        const finalPath = path || `/game?code=${encodeURIComponent(state.code)}`;
        window.location.assign(absolutePath(finalPath));
    }

    function pollLobbyState() {
        if (!state.code || state.redirecting) {
            return;
        }

        go(absolutePath(`/api/games/${encodeURIComponent(state.code)}/state`), "GET")
            .then((response) => {
                if (response && response.type === "GAME_STATE_UPDATE") {
                    const targetCode = response.code ? String(response.code) : state.code;
                    redirectToGame(`/game?code=${encodeURIComponent(targetCode)}`);
                }
            })
            .catch(() => {
                // Aun no hay estado de partida disponible o ha fallado temporalmente.
            });
    }

    function startLobbyPolling() {
        if (lobbyPollingId !== null || !state.code) {
            return;
        }
        pollLobbyState();
        lobbyPollingId = window.setInterval(pollLobbyState, 1500);
    }

    function onWsMessage(message) {
        if (!message || !message.type) {
            return;
        }
        if (message.code && !sameLobbyCode(message.code, state.code)) {
            return;
        }

        if (message.type === "LOBBY_STATE_UPDATE") {
            applyLobbyState(message);
            return;
        }

        if (message.type === "GAME_STATE_UPDATE") {
            const targetCode = message.code ? String(message.code) : state.code;
            redirectToGame(`/game?code=${encodeURIComponent(targetCode)}`);
            return;
        }

        if (message.type === "LOBBY_CHAT_MESSAGE") {
            appendChatMessage(message);
            return;
        }

        if (message.type === "LOBBY_CHAT_REJECTED") {
            setChatError(message.message || "No se pudo enviar el mensaje.");
            return;
        }

        if (message.type === "GAME_START") {
            redirectToGame(message.redirectPath || null);
            return;
        }

        if (message.type === "LOBBY_CLOSED") {
            window.location.assign(absolutePath("/lobby-select"));
        }
    }

    function setupWsHook() {
        if (!window.ws || typeof ws.receive !== "function") {
            return;
        }

        const previousReceive = ws.receive;
        ws.receive = (message) => {
            previousReceive(message);
            onWsMessage(message);
        };
    }

    function submitChat(event) {
        event.preventDefault();
        if (!chatInput || !chatButton) {
            return;
        }

        const text = chatInput.value.trim();
        if (!text) {
            return;
        }

        setChatError("");
        chatInput.disabled = true;
        chatButton.disabled = true;

        go(absolutePath(`/api/lobbies/${encodeURIComponent(state.code)}/chat`), "POST", { message: text })
            .then((response) => {
                if (response && response.type === "LOBBY_CHAT_REJECTED") {
                    setChatError(response.message || "No se pudo enviar el mensaje.");
                    return;
                }
                chatInput.value = "";
            })
            .catch((e) => {
                setChatError(e.text || "Error al enviar el mensaje");
            })
            .finally(() => {
                chatInput.disabled = false;
                chatButton.disabled = false;
                chatInput.focus();
            });
    }

    setupWsHook();
    applyLobbyState(cfg.initialState || {});
    startLobbyPolling();

    if (chatForm) {
        chatForm.addEventListener("submit", submitChat);
    }
})();