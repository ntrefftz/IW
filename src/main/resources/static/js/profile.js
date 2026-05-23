"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#avatar-form");
    const fileInput = document.querySelector("#f_avatar");
    const preview = document.querySelector("#avatar");
    const submitButton = document.querySelector("#postAvatar");
    const feedback = document.querySelector("#avatar-feedback");
    const currentAvatar = document.querySelector("#current-avatar");
    const allowedTypes = ["image/jpeg", "image/png"];
    const maxSizeBytes = 2 * 1024 * 1024;
    const maxSizeLabel = "2 MB";

    if (!form || !fileInput || !preview || !submitButton) {
        return;
    }

    const setFeedback = (type, message) => {
        if (!feedback) {
            return;
        }
        feedback.textContent = message;
        feedback.className = `alert ${type} py-2 mt-2`;
    };

    const clearFeedback = () => {
        if (!feedback) {
            return;
        }
        feedback.textContent = "";
        feedback.className = "small mt-2";
    };

    const validateFile = (file) => {
        if (!file) {
            return { ok: false, message: "Selecciona una imagen antes de subirla." };
        }
        const name = (file.name || "").toLowerCase();
        const hasValidExt = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
        if (!(allowedTypes.includes(file.type) || hasValidExt)) {
            return { ok: false, message: "Solo se permiten archivos PNG o JPG/JPEG." };
        }
        if (file.size > maxSizeBytes) {
            return { ok: false, message: `El archivo supera el limite de ${maxSizeLabel}.` };
        }
        return { ok: true, message: "" };
    };

    const fileToDataUrl = (file) => new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });

    fileInput.addEventListener("change", () => {
        const file = fileInput.files && fileInput.files[0];
        if (!file) {
            preview.removeAttribute("src");
            preview.classList.add("d-none");
            submitButton.classList.add("d-none");
            return;
        }
        const validation = validateFile(file);
        if (!validation.ok) {
            setFeedback("alert-warning", validation.message);
            fileInput.value = "";
            preview.removeAttribute("src");
            preview.classList.add("d-none");
            submitButton.classList.add("d-none");
            return;
        }
        clearFeedback();
        fileToDataUrl(file).then(dataUrl => {
            preview.src = dataUrl;
            preview.classList.remove("d-none");
            submitButton.classList.remove("d-none");
        });
    });

    submitButton.addEventListener("click", (event) => {
        event.preventDefault();
        const file = fileInput.files && fileInput.files[0];
        const validation = validateFile(file);
        if (!validation.ok) {
            setFeedback("alert-warning", validation.message);
            return;
        }

        submitButton.disabled = true;
        setFeedback("alert-info", "Subiendo imagen...");

        fileToDataUrl(file)
            .then((dataUrl) => {
                preview.src = dataUrl;
                return postImage(preview, form.action, "photo", file.name);
            })
            .then(() => {
                setFeedback("alert-success", "Imagen subida con exito.");
                if (currentAvatar) {
                    currentAvatar.src = `${form.action}?t=${Date.now()}`;
                }
                preview.classList.add("d-none");
                submitButton.classList.add("d-none");
                fileInput.value = "";
            })
            .catch((err) => {
                let message = "No se pudo subir la imagen.";
                if (err && err.status === 413) {
                    message = `El archivo supera el limite de ${maxSizeLabel}.`;
                } else if (err && err.status === 415) {
                    message = "Solo se permiten archivos PNG o JPG/JPEG.";
                } else if (err && err.text) {
                    message = err.text;
                }
                setFeedback("alert-danger", message);
            })
            .finally(() => {
                submitButton.disabled = false;
            });
    });
});
