(() => {
    const svg = document.querySelector("#unoBoard");
    const turnLabel = document.querySelector("#turnLabel");
    const directionLabel = document.querySelector("#directionLabel");
    const turnBox = document.querySelector("#turnBox");
    const gameMessage = document.querySelector("#gameMessage");
    const gameModeBadge = document.querySelector("#gameModeBadge");
    const gameVisibilityBadge = document.querySelector("#gameVisibilityBadge");

    if (!svg || !turnLabel || !directionLabel) {
        return;
    }

    const cc = {
        w: 80,
        h: 120,
        r: 8,
        handGap: 42,
        boardCenterX: 550,
        boardCenterY: 290,
        colors: {
            R: "#dc3545",
            V: "#198754",
            A: "#0d6efd",
            Y: "#ffc107",
            W: "#343a40",
            X: "#6c757d"
        }
    };

    const state = {
        code: (window.initialUnoState && window.initialUnoState.lobbyCode) || "",
        currentUsername: (window.initialUnoState && window.initialUnoState.currentUsername) || "",
        players: Array.isArray(window.initialUnoState && window.initialUnoState.players)
            ? [...window.initialUnoState.players]
            : [],
        lobbyMode: (window.initialUnoState && window.initialUnoState.lobbyMode) || "UNO",
        lobbyPrivate: !!(window.initialUnoState && window.initialUnoState.lobbyPrivate),
        yourHand: [],
        opponentCounts: [0, 0, 0],
        topCardCode: "R0",
        drawPileCount: 0,
        turno: 0,
        sentido: 1,
        gameStatus: "PARTIDA",
        winnerUsername: "",
        currentTurnUsername: "",
        loading: false,
        redirecting: false
    };

    const alertLevels = ["secondary", "primary", "info", "warning", "success", "danger", "dark"];

    function setMessage(msg, level = "secondary") {
        if (gameMessage) {
            gameMessage.textContent = msg;
            alertLevels.forEach(l => gameMessage.classList.remove(`alert-${l}`));
            gameMessage.classList.add(`alert-${level}`);
        }
    }

    function absolutePath(path) {
        const base = (config && config.rootUrl ? config.rootUrl : "").replace(/\/+$/, "");
        const normalized = path.startsWith("/") ? path : `/${path}`;
        return `${base}${normalized}`;
    }

    function renderLobbyMeta() {
        if (gameModeBadge) {
            gameModeBadge.textContent = state.lobbyMode;
        }
        if (gameVisibilityBadge) {
            gameVisibilityBadge.textContent = state.lobbyPrivate ? "Privada" : "Publica";
        }
    }

    function applyLobbyState(update) {
        if (!update) {
            return;
        }
        if (Array.isArray(update.players) && update.players.length > 0) {
            state.players = update.players.map(p => (p || "").toString());
        }
        if (typeof update.modalidad === "string" && update.modalidad.length > 0) {
            state.lobbyMode = update.modalidad;
        }
        if (typeof update.privado === "boolean") {
            state.lobbyPrivate = update.privado;
        }
        renderLobbyMeta();

        if (update.estado === "LOBBY" && !state.redirecting) {
            scheduleLobbyRedirect(`/lobby?code=${encodeURIComponent(state.code)}`, 1200);
        }
    }

    function scheduleLobbyRedirect(path, delayMs = 0) {
        if (state.redirecting) {
            return;
        }
        state.redirecting = true;
        const finalPath = path || `/lobby?code=${encodeURIComponent(state.code)}`;
        window.setTimeout(() => {
            window.location.assign(absolutePath(finalPath));
        }, Math.max(delayMs, 0));
    }

    function buildRelativePlayers() {
        const rawPlayers = Array.isArray(state.players)
            ? state.players
                .map(name => (name || "").toString().trim())
                .filter(name => name.length > 0)
            : [];

        const me = (state.currentUsername || "").trim();
        if (rawPlayers.length === 0) {
            const fallback = me ? [me] : ["Tu"];
            while (fallback.length < 4) {
                fallback.push(`Jugador ${fallback.length + 1}`);
            }
            return fallback;
        }

        const myPos = me ? rawPlayers.findIndex(name => name === me) : -1;
        let ordered = [];

        if (myPos >= 0) {
            ordered = rawPlayers.slice(myPos).concat(rawPlayers.slice(0, myPos));
        } else if (me) {
            const others = rawPlayers.filter(name => name !== me);
            ordered = [me, ...others];
        } else {
            ordered = [...rawPlayers];
        }

        while (ordered.length < 4) {
            ordered.push(`Jugador ${ordered.length + 1}`);
        }
        return ordered;
    }

    function playerNameForRelativeIndex(relativeIndex) {
        const order = buildRelativePlayers();
        const candidate = order[relativeIndex];
        if (candidate && candidate.trim().length > 0) {
            return candidate;
        }
        return `Jugador ${relativeIndex + 1}`;
    }

    function currentTurnLabel() {
        if (state.turno === 0) {
            return "TU TURNO";
        }
        const turnName = (state.currentTurnUsername || playerNameForRelativeIndex(state.turno) || "otro jugador").toUpperCase();
        return `TURNO DE ${turnName}`;
    }

    function refreshTurnBanner() {
        if (turnLabel) {
            turnLabel.textContent = currentTurnLabel();
        }

        if (turnBox) {
            turnBox.classList.remove("border-success", "border-warning", "bg-success-subtle", "bg-warning-subtle");
            if (state.turno === 0) {
                turnBox.classList.add("border-success", "bg-success-subtle");
            } else {
                turnBox.classList.add("border-warning", "bg-warning-subtle");
            }
        }
    }

    function refreshStatusMessage() {
        if (state.gameStatus === "TERMINADA") {
            const winner = (state.winnerUsername || "").trim();
            if (winner && winner === state.currentUsername) {
                setMessage("HAS GANADO LA PARTIDA", "success");
                return;
            }
            if (winner) {
                setMessage(`${winner} HA GANADO LA PARTIDA, HAS PERDIDO`, "danger");
                return;
            }
            setMessage("LA PARTIDA HA TERMINADO", "secondary");
            return;
        }

        const turnName = (state.currentTurnUsername || playerNameForRelativeIndex(state.turno) || "otro jugador").toUpperCase();
        if (state.turno === 0) {
            if (state.yourHand.length === 1) {
                setMessage("ME QUEDA UNA. ES MI TURNO.", "warning");
            } else {
                setMessage("ES TU TURNO. JUEGA O ROBA CARTA.", "primary");
            }
            return;
        }

        if (state.yourHand.length === 1) {
            setMessage(`ME QUEDA UNA. TURNO DE ${turnName}.`, "warning");
            return;
        }

        setMessage(`TURNO DE ${turnName}.`, "secondary");
    }

    function parseCard(code) {
        if (typeof code !== "string" || code.length < 2) {
            return { suit: "W", text: "?" };
        }
        return {
            suit: code[0].toUpperCase(),
            text: code.slice(1) || "?"
        };
    }

    function isPlayable(cardCode, topCardCode) {
        const card = parseCard(cardCode);
        const top = parseCard(topCardCode || "R0");
        if (card.suit === "W") {
            return true;
        }
        return card.suit === top.suit || card.text === top.text;
    }

    function drawLabel(x, y, text, bold = false) {
        svg.insertAdjacentHTML("beforeend", `
            <text x="${x}" y="${y}" font-size="14" font-family="Arial" font-weight="${bold ? "bold" : "normal"}" fill="#333">${text}</text>
        `);
    }

    function drawFrontCard(container, x, y, cardCode, id, options = {}) {
        const parsed = parseCard(cardCode);
        const color = cc.colors[parsed.suit] || cc.colors.W;
        const clickable = options.clickable === true;

        container.insertAdjacentHTML("beforeend", `
            <g id="${id}" style="cursor:${clickable ? "pointer" : "default"}; opacity:${options.dimmed ? "0.55" : "1"};">
                <rect x="${x}" y="${y}" width="${cc.w}" height="${cc.h}" rx="${cc.r}" ry="${cc.r}" fill="${color}"/>
                <rect x="${x + 10}" y="${y + 10}" width="${cc.w - 20}" height="${cc.h - 20}" rx="${cc.r - 2}" ry="${cc.r - 2}" fill="#f8f9fa"/>
                <text x="${x + cc.w / 2}" y="${y + cc.h / 2 + 10}" font-size="32" font-weight="bold" font-family="Arial" text-anchor="middle" fill="#111">${parsed.text}</text>
            </g>
        `);

        if (clickable && typeof options.onClick === "function") {
            const node = container.querySelector(`#${id}`);
            if (node) {
                node.addEventListener("click", options.onClick);
            }
        }
    }

    function drawBackCard(container, x, y, id, clickable = false, onClick = null) {
        container.insertAdjacentHTML("beforeend", `
            <g id="${id}" style="cursor:${clickable ? "pointer" : "default"}; opacity:${clickable ? "1" : "0.95"};">
                <rect x="${x}" y="${y}" width="${cc.w}" height="${cc.h}" rx="${cc.r}" ry="${cc.r}" fill="#d11f30"/>
                <rect x="${x + 8}" y="${y + 8}" width="${cc.w - 16}" height="${cc.h - 16}" rx="${cc.r - 2}" ry="${cc.r - 2}" fill="#1f2937"/>
                <text x="${x + cc.w / 2}" y="${y + cc.h / 2 + 8}" font-size="18" font-weight="bold" font-family="Arial" text-anchor="middle" fill="#fff">UNO</text>
            </g>
        `);

        if (clickable && typeof onClick === "function") {
            const node = container.querySelector(`#${id}`);
            if (node) {
                node.addEventListener("click", onClick);
            }
        }
    }

    function drawOpponent(playerIndex) {
        const count = state.opponentCounts[playerIndex - 1] || 0;
        const y = 48 + (playerIndex - 1) * 122;
        const x = 90 + (playerIndex - 1) * 285;
        const name = playerNameForRelativeIndex(playerIndex);
        drawLabel(x, y - 12, `${name} (${count} cartas)`, state.turno === playerIndex);

        const visibleBacks = Math.min(7, count);
        for (let i = 0; i < visibleBacks; i += 1) {
            drawBackCard(svg, x + i * 18, y, `op-${playerIndex}-${i}`);
        }
    }

    function chooseColorIfNeeded(cardCode) {
        if (!cardCode.startsWith("W")) {
            return null;
        }

        const response = window.prompt("Elige color: RED, GREEN, BLUE, YELLOW", "RED");
        if (!response) {
            return null;
        }

        const c = response.trim().toUpperCase();
        if (["RED", "GREEN", "BLUE", "YELLOW"].includes(c)) {
            return c;
        }
        setMessage("Color no valido. Usa RED, GREEN, BLUE o YELLOW.");
        return undefined;
    }

    function drawPiles() {
        const drawX = cc.boardCenterX - 130;
        const y = cc.boardCenterY - 30;
        const discardX = cc.boardCenterX + 30;
        const canAct = state.gameStatus === "PARTIDA" && state.turno === 0 && !state.loading;

        drawLabel(drawX, y - 14, `Mazo (${state.drawPileCount})`, true);
        drawBackCard(svg, drawX, y, "draw-pile", canAct, () => {
            sendAction({ actionType: "DRAW_CARD" });
        });

        drawLabel(discardX, y - 14, "Descarte", true);
        drawFrontCard(svg, discardX, y, state.topCardCode || "R0", "discard-top");
    }

    function drawPlayerHand() {
        const hand = state.yourHand || [];
        const y = 560;
        const x0 = 95;

        drawLabel(x0, y - 16, `Tu mano (${hand.length} cartas)`, true);

        hand.forEach((card, index) => {
            const x = x0 + index * cc.handGap;
            const playable = state.gameStatus === "PARTIDA"
                && state.turno === 0
                && !state.loading
                && isPlayable(card.code, state.topCardCode);

            drawFrontCard(svg, x, y, card.code, `my-${index}`, {
                clickable: playable,
                dimmed: !playable,
                onClick: () => {
                    const chosenColor = chooseColorIfNeeded(card.code);
                    if (chosenColor === undefined) {
                        return;
                    }
                    const payload = {
                        actionType: "PLAY_CARD",
                        cardId: card.id
                    };
                    if (chosenColor) {
                        payload.chosenColor = chosenColor;
                    }
                    sendAction(payload);
                }
            });
        });
    }

    function normalizeFromServer(update) {
        const incoming = update || {};
        const hand = Array.isArray(incoming.yourHand) ? incoming.yourHand : [];

        const normalizedHand = hand.map(c => ({
            id: c.id,
            code: c.code
        }));

        const counts = Array.isArray(incoming.opponentCounts) ? [...incoming.opponentCounts] : [];
        while (counts.length < 3) {
            counts.push(0);
        }

        return {
            yourHand: normalizedHand,
            opponentCounts: counts,
            topCardCode: incoming.topCardCode || "R0",
            drawPileCount: Number.isInteger(incoming.drawPileCount) ? incoming.drawPileCount : 0,
            turno: Number.isInteger(incoming.turno) ? incoming.turno : 0,
            sentido: incoming.sentido === -1 ? -1 : 1,
            gameStatus: typeof incoming.gameStatus === "string" ? incoming.gameStatus : "PARTIDA",
            winnerUsername: typeof incoming.winnerUsername === "string" ? incoming.winnerUsername : "",
            currentTurnUsername: typeof incoming.currentTurnUsername === "string" ? incoming.currentTurnUsername : ""
        };
    }

    function applyServerState(update) {
        const normalized = normalizeFromServer(update);
        state.yourHand = normalized.yourHand;
        state.opponentCounts = normalized.opponentCounts;
        state.topCardCode = normalized.topCardCode;
        state.drawPileCount = normalized.drawPileCount;
        state.turno = normalized.turno;
        state.sentido = normalized.sentido;
        state.gameStatus = normalized.gameStatus;
        state.winnerUsername = normalized.winnerUsername;
        state.currentTurnUsername = normalized.currentTurnUsername;
        state.loading = false;
        render();
    }

    function handleGameOver(message) {
        if (!message) {
            return;
        }
        state.loading = false;
        state.gameStatus = "TERMINADA";
        if (typeof message.winnerUsername === "string") {
            state.winnerUsername = message.winnerUsername;
        }
        render();
        setMessage(message.message || "LA PARTIDA HA TERMINADO", message.isWinner ? "success" : "danger");
        const waitMs = Number.isInteger(message.autoRedirectMs) ? message.autoRedirectMs : 3000;
        scheduleLobbyRedirect(message.redirectPath || `/lobby?code=${encodeURIComponent(state.code)}`, waitMs);
    }

    function onWsMessage(message) {
        if (!message || !message.type) {
            return;
        }
        if (state.code && message.code && message.code !== state.code) {
            return;
        }

        if (message.type === "GAME_STATE_UPDATE") {
            applyServerState(message);
            return;
        }

        if (message.type === "ACTION_REJECTED") {
            state.loading = false;
            setMessage(message.message || "Accion rechazada");
            return;
        }

        if (message.type === "GAME_OVER") {
            handleGameOver(message);
            return;
        }

        if (message.type === "LOBBY_STATE_UPDATE") {
            applyLobbyState(message);
            return;
        }

        if (message.type === "LOBBY_CLOSED") {
            window.location.assign(absolutePath("/lobby-select"));
            return;
        }

        if (message.type === "GAME_START") {
            setMessage("Partida iniciada. Esperando estado inicial...");
        }
    }

    function setupWsHook() {
        if (!ws || typeof ws.receive !== "function") {
            return;
        }

        const oldReceive = ws.receive;
        ws.receive = (m) => {
            oldReceive(m);
            onWsMessage(m);
        };
    }

    function sendAction(actionPayload) {
        if (!state.code || state.loading || state.gameStatus !== "PARTIDA") {
            return;
        }

        state.loading = true;
        setMessage("ENVIANDO ACCION...", "info");

        go(`${config.rootUrl}/api/games/${state.code}/action`, "POST", actionPayload)
            .then((response) => {
                if (response.type === "GAME_STATE_UPDATE") {
                    applyServerState(response);
                } else if (response.type === "GAME_OVER") {
                    handleGameOver(response);
                } else if (response.type === "ACTION_REJECTED") {
                    state.loading = false;
                    setMessage(response.message || "Accion rechazada");
                }
            })
            .catch((e) => {
                state.loading = false;
                setMessage(`Error al enviar accion: ${e.text || e.status || "desconocido"}`);
            });
    }

    function loadInitialState() {
        if (!state.code) {
            setMessage("No hay codigo de lobby asociado.");
            return;
        }

        setMessage("Cargando estado de partida...");
        go(`${config.rootUrl}/api/games/${state.code}/state`, "GET")
            .then((response) => {
                if (response.type === "GAME_STATE_UPDATE") {
                    applyServerState(response);
                } else {
                    setMessage("No se pudo cargar el estado inicial.");
                }
            })
            .catch((e) => {
                if (e && e.status === 409) {
                    scheduleLobbyRedirect(`/lobby?code=${encodeURIComponent(state.code)}`, 0);
                    return;
                }
                setMessage(`Error al cargar estado inicial: ${e.text || e.status || "desconocido"}`);
            });
    }

    function render() {
        svg.innerHTML = "";

        drawOpponent(1);
        drawOpponent(2);
        drawOpponent(3);
        drawPiles();
        drawPlayerHand();

        refreshTurnBanner();
        refreshStatusMessage();
        directionLabel.textContent = state.sentido === 1 ? "Horario" : "Antihorario";
    }

    window.unoRepinta = applyServerState;

    setupWsHook();
    renderLobbyMeta();
    loadInitialState();
})();
