Feature: Lobbies UI

  Background:
    # Para probar los lobbies, primero necesitamos estar autenticados.
    # Reutilizamos la lógica de tu ejemplo para hacer login con el usuario 'a'.
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click(".form-signin button")
    Then waitForUrl(baseUrl + '/admin')

  ### 1. NAVEGACIÓN Y SELECTOR DE LOBBIES ###

  Scenario: Navegar al selector y ver elementos básicos
    Given driver baseUrl + '/lobby-select'
    Then match html('body') contains 'Buscador de lobbies'
    # Comprobamos que la tabla de partidas públicas está renderizada
    And match html('.table-responsive') contains 'Código de partida'

  ### 2. ENTRADA DE JUGADORES ###

  Scenario: Unirse a una partida existente correctamente
    Given driver baseUrl + '/lobby-select'
    And input('#gameCode', 'ABC123')
    And input('#gamePassword', 'secreta')
    # Hacemos click en el botón que tiene el texto "Unirse"
    When submit().click("{button}Unirse")
    # Si todo va bien, el backend nos debería redirigir a la sala de espera
    Then waitForUrl(baseUrl + '/lobby')
    And match html('body') contains '¡Bienvenido a la sala de espera!'

  Scenario: Fallo al unirse a un lobby (ej. código incorrecto o contraseña mala)
    Given driver baseUrl + '/lobby-select'
    And input('#gameCode', 'FALSO1')
    And input('#gamePassword', 'mal')
    When submit().click("{button}Unirse")
    # Asumiendo que el backend recarga la página de selección si hay error
    Then waitForUrl(baseUrl + '/lobby-select')
    # IMPORTANTE: Descomenta y ajusta la siguiente línea según la clase CSS
    # que uses en tu plantilla para los mensajes de error flash (ej. .error, .alert-danger)
    # Then match html('.alert-danger') contains 'partida no encontrada'

  ### 3. LÍMITE DE JUGADORES (MAX PLAYERS) ###

  Scenario: Intento de unirse a una sala que ya ha alcanzado el límite máximo
    Given driver baseUrl + '/lobby-select'
    And input('#gameCode', 'LLENA99')
    When submit().click("{button}Unirse")
    # Asumiendo que se queda en la misma página mostrando un error
    Then waitForUrl(baseUrl + '/lobby-select')
    # Then match html('.error') contains 'La sala está llena'

  ### 4. INTERACCIÓN DENTRO DEL LOBBY (JS y DOM) ###

  Scenario: Interactuar con la configuración de partida en el lobby
    Given driver baseUrl + '/lobby'
    # Usamos click() simple sin submit() porque es JS de cliente (no recarga la página)
    When click('#p_privada')
    # Al hacer click, el bloque #privada_content debería ser visible. 
    # Validamos ejecutando JS en el navegador:
    Then match script("!document.getElementById('privada_content').classList.contains('d-none')") == true
    
    # Probar la búsqueda de amigos en partida privada
    And input('#username', '9781')
    When click('#fetchUser')
    # Aquí puedes añadir un delay si la petición AJAX tarda, o esperar a un elemento nuevo
    # Then karate.stop(1000)