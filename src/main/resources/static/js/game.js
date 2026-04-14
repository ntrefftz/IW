(() => {
    const svg = document.querySelector("#unoBoard");
    const turnLabel = document.querySelector("#turnLabel");
    const directionLabel = document.querySelector("#directionLabel");
    const gameMessage = document.querySelector("#gameMessage");

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
        yourHand: [],
        opponentCounts: [0, 0, 0],
        topCardCode: "R0",
        drawPileCount: 0,
        turno: 0,
        sentido: 1,
        loading: false
    };

    function setMessage(msg) {
        if (gameMessage) {
            gameMessage.textContent = msg;
        }
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
        drawLabel(x, y - 12, `Jugador ${playerIndex + 1} (${count} cartas)`, state.turno === playerIndex);

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

        drawLabel(drawX, y - 14, `Mazo (${state.drawPileCount})`, true);
        drawBackCard(svg, drawX, y, "draw-pile", state.turno === 0 && !state.loading, () => {
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
            const playable = state.turno === 0 && !state.loading && isPlayable(card.code, state.topCardCode);

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
            sentido: incoming.sentido === -1 ? -1 : 1
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
        state.loading = false;
        render();
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
        if (!state.code || state.loading) {
            return;
        }

        state.loading = true;
        setMessage("Enviando accion...");

        go(`${config.rootUrl}/api/games/${state.code}/action`, "POST", actionPayload)
            .then((response) => {
                if (response.type === "GAME_STATE_UPDATE") {
                    applyServerState(response);
                    setMessage("Accion aplicada.");
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
                    setMessage("Estado sincronizado.");
                } else {
                    setMessage("No se pudo cargar el estado inicial.");
                }
            })
            .catch((e) => {
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

        turnLabel.textContent = String(state.turno + 1);
        directionLabel.textContent = state.sentido === 1 ? "Horario" : "Antihorario";
    }

    window.unoRepinta = applyServerState;

    setupWsHook();
    loadInitialState();
})();
