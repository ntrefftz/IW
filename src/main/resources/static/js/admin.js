document.addEventListener('DOMContentLoaded', function() {

  const botonAnadir = document.getElementById('botonAnadir');
  const modalUsuario = document.getElementById('modalUsuario');
  const cerrarModal = document.getElementById('cerrarModal');
  const formUsuario = document.getElementById('formUsuario');
  const tablaUsuarios = document.getElementById('tablaUsuarios').querySelector('tbody');

  botonAnadir.addEventListener('click', () => {
    modalUsuario.style.display = 'flex';
  });

  cerrarModal.addEventListener('click', () => {
    modalUsuario.style.display = 'none';
  });

  modalUsuario.addEventListener('click', (e) => {
    if (e.target === modalUsuario) {
      modalUsuario.style.display = 'none';
    }
  });

  formUsuario.addEventListener('submit', function(e) {
    e.preventDefault();

    const nombre = document.getElementById('nombreUsuario').value;
    const rol = document.getElementById('rolUsuario').value;
    const grupo = document.getElementById('grupoUsuario').value;
    const id = Math.floor(Math.random() * 10000);

    const nuevaFila = document.createElement('tr');

    nuevaFila.innerHTML = `
        <td>${id}</td>
        <td>${nombre}</td>
        
        <td><ul>${rol}</ul></td>
        <td><ul><li>${grupo}</li></ul></td>
        <td>Acciones</td>
        <td>
            <button class="btn btn-secondary" disabled>Deshabilitado...</button><br>
            <span class="text-muted ms-2">Este botón se implementará cuando haya base de datos</span>
        </td>

    `;


    tablaUsuarios.appendChild(nuevaFila);

    formUsuario.reset();
    modalUsuario.style.display = 'none';
  });

});

document.querySelectorAll('.btn-close-game').forEach(btn => {
    btn.addEventListener('click', (e) => {
        e.preventDefault();
        const form = e.target.closest('form');
        const fila = e.target.closest('tr');

        if (confirm("¿Cerrar esta partida y expulsar a todos los jugadores?")) {
            go(form.action, 'POST').then(data => {
                if (data.status === 'success') {
                    fila.style.transition = "all 0.5s";
                    fila.style.opacity = "0";
                    setTimeout(() => fila.remove(), 500);
                } else {
                    alert("Error: " + data.message);
                }
            });
        }
    });
});