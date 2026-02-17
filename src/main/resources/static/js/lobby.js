const btnPublica = document.querySelector("#p_publica");
const btnPrivada = document.querySelector("#p_privada");
const publicaContent = document.querySelector("#publica_content");
const privadaContent = document.querySelector("#privada_content");
const friends = document.querySelector("#friends");
const addFriend = document.querySelector("#username");

const showPublica = () => {
    publicaContent.classList.remove("d-none");
    privadaContent.classList.add("d-none");
};

const showPrivada = () => {
    privadaContent.classList.remove("d-none");
    publicaContent.classList.add("d-none");
};

btnPublica.onclick = e => {
    showPublica();
};

btnPrivada.onclick = e => {
    showPrivada();
};