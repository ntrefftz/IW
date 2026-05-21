Feature: Lobbies UI

  Background:
    # Para probar los lobbies, primero necesitamos estar autenticados.
    # Reutilizamos la lógica de tu ejemplo para hacer login con el usuario 'a'.
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click(".form-signin button")
    Then waitForUrl(baseUrl + '/')

  ### 1. NAVEGACIÓN Y SELECTOR DE LOBBIES ###

  Scenario: Navegar al selector y ver elementos básicos
    Given driver baseUrl + '/lobby-select'
    Then match html('body') contains 'Buscador de lobbies'
    # Comprobamos que la tabla de partidas públicas está renderizada
    And match html('.table-responsive') contains 'Código de partida'

  ### 2. ENTRADA DE JUGADORES ###
# HAY QUE CREARLA CON USUARIO b Y PONER EL CODIGO EN ESTE TEST
  Scenario: Unirse a una partida existente correctamente 
    Given driver baseUrl + '/lobby-select'
    And input('#gameCode', 'ABC123')
    When submit().click("#btn-unirse")
    # Si todo va bien, el backend nos debería redirigir a la sala de espera
    Then waitForUrl(baseUrl + '/lobby')
    And match html('h1') contains 'Sala de espera'

  Scenario: Fallo al unirse a un lobby (ej. código incorrecto o contraseña mala)
    Given driver baseUrl + '/lobby-select'
    And input('#gameCode', 'FALSO1')
    When submit().click("#btn-unirse")
    # Asumiendo que el backend recarga la página de selección si hay error
    Then waitForUrl(baseUrl + '/lobby-select')
    Then match html('.alert-danger span') contains 'Partida no encontrada'

  ### 3. LÍMITE DE JUGADORES (MAX PLAYERS) ###
 # HAY QUE CREARLA A MANO Y PONER 4 PERSONAS QUE NO SEAN EL USUARIO a
  Scenario: Intento de unirse a una sala que ya ha alcanzado el límite máximo 
    Given driver baseUrl + '/lobby-select'
    And input('#gameCode', 'LLENA99')
    When submit().click("#btn-unirse")
    # Asumiendo que se queda en la misma página mostrando un error
    Then waitForUrl(baseUrl + '/lobby-select')
    Then match html('.error') contains 'La sala esta llena'

  ### 4. INTERACCIÓN DENTRO DEL LOBBY (JS y DOM) ###

  Scenario: Crear un lobby y hacerlo privado
    Given driver baseUrl + '/lobby-select'
    When submit.click('{button}Crear lobby')
    Then waitForUrl(baseUrl + '/lobby')
    When click('#privado')
    And submit().click("{button}Guardar configuración")
    Then match html('.visibilityBadge') contains 'Privada'
    