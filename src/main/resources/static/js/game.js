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
            W: "#343a40"
        }
    };

    const PLAYER_INDEX = 0;

    const state = {
        mazos: [],
        robo: [],
        jugadas: [],
        turno: 0,
        sentido: 1
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

    function ensureDeck() {
        if (state.robo.length > 0) {
            return;
        }

        if (state.jugadas.length <= 1) {
            return;
        }

        const top = state.jugadas.pop();
        state.robo = state.jugadas.splice(0);
        state.jugadas = [top];
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
            <g id="${id}" style="cursor:${clickable ? "pointer" : "default"};">
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
        const hand = state.mazos[playerIndex];
        if (!Array.isArray(hand)) {
            return;
        }

        const y = 48 + (playerIndex - 1) * 122;
        const x = 90 + (playerIndex - 1) * 285;
        drawLabel(x, y - 12, `Jugador ${playerIndex + 1} (${hand.length} cartas)`, state.turno === playerIndex);

        const visibleBacks = Math.min(7, hand.length);
        for (let i = 0; i < visibleBacks; i += 1) {
            drawBackCard(svg, x + i * 18, y, `op-${playerIndex}-${i}`);
        }
    }

    function drawPiles() {
        const drawX = cc.boardCenterX - 130;
        const y = cc.boardCenterY - 30;
        const discardX = cc.boardCenterX + 30;

        drawLabel(drawX, y - 14, `Mazo (${state.robo.length})`, true);
        drawBackCard(svg, drawX, y, "draw-pile", state.turno === PLAYER_INDEX, () => {
            if (state.turno !== PLAYER_INDEX) {
                return;
            }
            ensureDeck();
            if (state.robo.length === 0) {
                setMessage("No quedan cartas para robar.");
                return;
            }

            const card = state.robo.pop();
            state.mazos[PLAYER_INDEX].push(card);
            setMessage(`Robas ${card}.`);
            render();
        });

        drawLabel(discardX, y - 14, "Descarte", true);
        const top = state.jugadas[state.jugadas.length - 1] || "R0";
        drawFrontCard(svg, discardX, y, top, "discard-top");
    }

    function maybeBotTurns() {
        while (state.turno !== PLAYER_INDEX) {
            const hand = state.mazos[state.turno] || [];
            const top = state.jugadas[state.jugadas.length - 1];
            const playableIndex = hand.findIndex(c => isPlayable(c, top));

            if (playableIndex >= 0) {
                const played = hand.splice(playableIndex, 1)[0];
                state.jugadas.push(played);
            } else {
                ensureDeck();
                if (state.robo.length > 0) {
                    hand.push(state.robo.pop());
                }
            }

            state.turno = (state.turno + 1) % state.mazos.length;
        }
    }

    function drawPlayerHand() {
        const hand = state.mazos[PLAYER_INDEX] || [];
        const y = 560;
        const x0 = 95;

        drawLabel(x0, y - 16, `Tu mano (${hand.length} cartas)`, true);

        const top = state.jugadas[state.jugadas.length - 1];

        hand.forEach((cardCode, index) => {
            const x = x0 + index * cc.handGap;
            const playable = state.turno === PLAYER_INDEX && isPlayable(cardCode, top);

            drawFrontCard(svg, x, y, cardCode, `my-${index}`, {
                clickable: playable,
                dimmed: !playable,
                onClick: () => {
                    const played = hand.splice(index, 1)[0];
                    state.jugadas.push(played);

                    if (hand.length === 0) {
                        setMessage("Has ganado la ronda.");
                        render();
                        return;
                    }

                    state.turno = (state.turno + 1) % state.mazos.length;
                    maybeBotTurns();
                    setMessage(`Juegas ${played}.`);
                    render();
                }
            });
        });
    }

    function normalizeInput(input) {
        const incoming = input || {};
        const mazos = Array.isArray(incoming.mazos)
            ? incoming.mazos.map(m => Array.isArray(m) ? [...m] : [])
            : [];

        while (mazos.length < 4) {
            mazos.push([]);
        }

        return {
            mazos,
            robo: Array.isArray(incoming.robo) ? [...incoming.robo] : ["Y9", "A2", "V0", "R7", "Y8", "A9", "V+2", "R6", "Y0", "A6"],
            jugadas: Array.isArray(incoming.jugadas) && incoming.jugadas.length > 0 ? [...incoming.jugadas] : ["R3"],
            turno: Number.isInteger(incoming.turno) ? incoming.turno : 0,
            sentido: incoming.sentido === -1 ? -1 : 1
        };
    }

    function repinta(gameState) {
        const normalized = normalizeInput(gameState);
        state.mazos = normalized.mazos;
        state.robo = normalized.robo;
        state.jugadas = normalized.jugadas;
        state.turno = normalized.turno;
        state.sentido = normalized.sentido;

        maybeBotTurns();
        render();
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

    const sampleState = {
        mazos: [
            ["R1", "V2", "A3", "Y4", "R+2", "WC", "Y5"],
            ["Y1", "A8", "V9", "R5", "W+4"],
            ["V1", "V7", "Y3", "A5", "R4"],
            ["R9", "A1", "Y7", "V3", "R2", "A+2"]
        ],
        robo: ["Y9", "A2", "V0", "R7", "Y8", "A9", "V+2", "R6", "Y0", "A6"],
        jugadas: ["R3"],
        turno: 0,
        sentido: 1
    };

    setMessage("Pulsa una carta valida de tu mano o el mazo para robar.");

    // De cara a WS: el servidor podra invocar window.unoRepinta(payload).
    window.unoRepinta = repinta;

    repinta(sampleState);
})();
