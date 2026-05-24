(() => {
    const chatForm = document.querySelector("#lobbyChatForm");
    const chatInput = document.querySelector("#lobbyChatInput");
    const chatMessages = document.querySelector("#lobbyChatMessages");
    const chatEmpty = document.querySelector("#lobbyChatEmpty");
    const chatError = document.querySelector("#lobbyChatError");
    const chatButton = chatForm ? chatForm.querySelector("button[type='submit']") : null;

    const state = {
        code: (window.initialUnoState && window.initialUnoState.lobbyCode) || "",
        currentUsername: (window.initialUnoState && window.initialUnoState.currentUsername) || ""
    };
    const isAdminChat = window.isAdminChat === true;

    function absolutePath(path) {
        const base = (config && config.rootUrl ? config.rootUrl : "").replace(/\/+$/, "");
        const normalized = path.startsWith("/") ? path : `/${path}`;
        return `${base}${normalized}`;
    }


    function setChatError(msg) {
        if (!chatError) {
            return;
        }
        if (msg) {
            chatError.textContent = msg;
            chatError.classList.remove("d-none");
        } else {
            chatError.textContent = "";
            chatError.classList.add("d-none");
        }
    }
    function muteUser(username) {
        if (!username) {
            return;
        }
        go(`/admin/chatBanByUsername/${encodeURIComponent(username)}`, "POST")
            .catch((e) => console.error("Error muteando:", e));
    }

    function banUser(username) {
        if (!username) {
            return;
        }
        go(`/admin/banByUsername/${encodeURIComponent(username)}`, "POST")
            .catch((e) => console.error("Error baneando:", e));
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
        row.className = "mb-2 d-flex justify-content-between align-items-start gap-2";

        const content = document.createElement("div");
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

        content.appendChild(header);
        content.appendChild(text);

        row.appendChild(content);

        if (isAdminChat) {
            const actions = document.createElement("div");
            actions.className = "btn-group btn-group-sm ms-2";

            const muteBtn = document.createElement("button");
            muteBtn.type = "button";
            muteBtn.className = "btn btn-outline-warning";
            muteBtn.textContent = "Mutear";
            muteBtn.addEventListener("click", () => muteUser(author));

            const banBtn = document.createElement("button");
            banBtn.type = "button";
            banBtn.className = "btn btn-outline-danger";
            banBtn.textContent = "Banear";
            banBtn.addEventListener("click", () => banUser(author));

            actions.appendChild(muteBtn);
            actions.appendChild(banBtn);
            row.appendChild(actions);
        }

        chatMessages.appendChild(row);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function submitChat(event) {
        event.preventDefault();
        if (!chatInput || !chatButton || !state.code) {
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


    function onWsMessage(message) {
        if (!message || !message.type) {
            return;
        }
        if (state.code && message.code && message.code !== state.code) {
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

    setupWsHook();

    if (chatForm) {
        chatForm.addEventListener("submit", submitChat);
    }
})();
