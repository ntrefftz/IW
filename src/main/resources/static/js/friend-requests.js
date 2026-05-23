"use strict";

(function () {
    const form = document.getElementById("friend-search-form");
    if (!form) {
        return;
    }

    const input = document.getElementById("friend-search-input");
    const feedback = document.getElementById("friend-feedback");
    const requestList = document.getElementById("friend-requests");
    const friendList = document.getElementById("friend-list");
    const root = config.rootUrl || "";

    function setFeedback(message, isError) {
        if (!feedback) {
            return;
        }
        feedback.textContent = message || "";
        feedback.classList.remove("text-danger", "text-success");
        if (message) {
            feedback.classList.add(isError ? "text-danger" : "text-success");
        }
    }

    function clearList(listEl, emptyMessage) {
        if (!listEl) {
            return;
        }
        listEl.innerHTML = "";
        if (emptyMessage) {
            const li = document.createElement("li");
            li.className = "list-group-item text-muted";
            li.textContent = emptyMessage;
            listEl.appendChild(li);
        }
    }

    function renderRequests(requests) {
        if (!requestList) {
            return;
        }
        requestList.innerHTML = "";
        if (!requests || requests.length === 0) {
            clearList(requestList, "No hay solicitudes pendientes.");
            return;
        }
        requests.forEach((req) => {
            const li = document.createElement("li");
            li.className = "list-group-item d-flex justify-content-between align-items-center";

            const info = document.createElement("div");
            info.className = "me-2";
            info.textContent = req.requesterName;

            const actions = document.createElement("div");

            const acceptBtn = document.createElement("button");
            acceptBtn.type = "button";
            acceptBtn.className = "btn btn-sm btn-success me-2";
            acceptBtn.textContent = "Aceptar";
            acceptBtn.addEventListener("click", () => handleRequestAction(req.id, "accept"));

            const rejectBtn = document.createElement("button");
            rejectBtn.type = "button";
            rejectBtn.className = "btn btn-sm btn-outline-danger";
            rejectBtn.textContent = "Rechazar";
            rejectBtn.addEventListener("click", () => handleRequestAction(req.id, "reject"));

            actions.appendChild(acceptBtn);
            actions.appendChild(rejectBtn);

            li.appendChild(info);
            li.appendChild(actions);
            requestList.appendChild(li);
        });
    }

    function renderFriends(friends) {
        if (!friendList) {
            return;
        }
        friendList.innerHTML = "";
        if (!friends || friends.length === 0) {
            clearList(friendList, "No tienes amigos todavía.");
            return;
        }
        friends.forEach((friend) => {
            const li = document.createElement("li");
            li.className = "list-group-item d-flex justify-content-between align-items-center";

            const info = document.createElement("div");
            const link = document.createElement("a");
            link.href = `${root}/user/${friend.userId}`;
            link.textContent = friend.username;
            link.className = "text-decoration-none";
            info.appendChild(link);

            const stats = document.createElement("small");
            stats.className = "text-muted ms-2";
            stats.textContent = `Partidas: ${friend.gamesPlayed} | Traiciones: ${friend.timesBetrayed}`;
            info.appendChild(stats);

            li.appendChild(info);
            friendList.appendChild(li);
        });
    }

    function loadRequests() {
        return go(`${root}/api/friends/requests`, "GET")
            .then(renderRequests)
            .catch(() => clearList(requestList, "No hay solicitudes pendientes."));
    }

    function loadFriends() {
        return go(`${root}/api/friends`, "GET")
            .then((data) => renderFriends(data.friends || []))
            .catch(() => clearList(friendList, "No tienes amigos todavía."));
    }

    function handleRequestAction(requestId, action) {
        setFeedback("");
        go(`${root}/api/friends/requests/${requestId}/${action}`, "POST", {})
            .then(() => {
                setFeedback(action === "accept" ? "Solicitud aceptada." : "Solicitud rechazada.", false);
                return Promise.all([loadRequests(), loadFriends()]);
            })
            .catch((err) => {
                setFeedback(err && err.text ? err.text : "Error al procesar la solicitud.", true);
            });
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        setFeedback("");
        const username = input.value.trim();
        if (!username) {
            setFeedback("Introduce un username.", true);
            return;
        }
        go(`${root}/api/friends/request`, "POST", { username })
            .then(() => {
                setFeedback("Solicitud enviada.", false);
                input.value = "";
                return loadRequests();
            })
            .catch((err) => {
                let msg = err && err.text ? err.text : "No se pudo enviar la solicitud.";
                if (err && err.text) {
                    try {
                        const parsed = JSON.parse(err.text);
                        if (parsed.error === "already friends") msg = "Ya sois amigos o hay una solicitud en curso.";
                        else if (parsed.error) msg = parsed.error;
                    } catch(e) {}
                }
                setFeedback(msg, true);
            });
    });

    loadRequests();
    loadFriends();
})();
