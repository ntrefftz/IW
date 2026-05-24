    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll(".js-chat-mute").forEach(btn => {
            btn.addEventListener("click", (e) => {
                const username = e.target.closest("[data-username]").dataset.username;
                go(`/admin/chatBanByUsername/${encodeURIComponent(username)}`, "POST")
                    .then(() => console.log("Usuario muteado:", username))
                    .catch(err => console.error("Error muteando:", err));
            });
        });

        document.querySelectorAll(".js-chat-ban").forEach(btn => {
            btn.addEventListener("click", (e) => {
                const username = e.target.closest("[data-username]").dataset.username;
                go(`/admin/banByUsername/${encodeURIComponent(username)}`, "POST")
                    .then(() => console.log("Usuario baneado:", username))
                    .catch(err => console.error("Error baneando:", err));
            });
        });

        const closeBtn = document.getElementById("closeLobbyBtn");
        if (closeBtn) {
            closeBtn.addEventListener("click", () => {
                const code = closeBtn.dataset.code;
                go(`/admin/closeGame/${encodeURIComponent(code)}`, "POST")
                    .then(() => console.log("Lobby cerrado:", code))
                    .catch(err => console.error("Error cerrando lobby:", err));
            });
        }
    });