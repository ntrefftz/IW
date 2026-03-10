Feature: Lobbies

  Background:
    Given url baseUrl
    # NOTA: En Spring Security normalmente necesitas estar autenticado y tener un token CSRF.
    # Asumimos que tienes una feature de login que te devuelve la sesión y el CSRF.
    # * def login = call read('classpath:utils/login.feature') { username: 'a', password: 'aa' }
    # * def csrfToken = login.csrfToken
    # * configure cookies = login.cookies
    * def csrfToken = 'tjlkh6ixocZ3QK2yk44E2NsA8JEc2M1lb6KvoASYh5VlcFrKj19UtM3UxKdaIZqH8aMw7-5k3fAou_xIDpWWljaq4vNXFTz4'

  Scenario: Cargar la página de selección de lobbies correctamente
    Given path '/lobby-select'
    When method get
    Then status 200
    And match response contains 'Buscador de lobbies'
    And match response contains 'Partidas públicas'
    # Verifica que el formulario de unirse existe
    And match response contains 'action="/lobbies/join"'

  Scenario: Unirse a una partida existente usando un código correcto
    Given path '/lobbies/join'
    And form field _csrf = csrfToken
    And form field code = 'ABC123'
    And form field password = ''
    When method post
    # Asumiendo que si tiene éxito, redirige a la sala de espera (/lobby o similar)
    Then status 302
    And match header Location contains '/lobby'

  Scenario: Fallar al intentar unirse a una partida con contraseña incorrecta
    Given path '/lobbies/join'
    And form field _csrf = csrfToken
    And form field code = 'ABC123'
    And form field password = 'contrasenaIncorrecta'
    When method post
    # Asumiendo que recarga la página con un error o da un Bad Request
    Then status 400
    # Alternativa si haces redirección con error:
    # Then status 302
    # And match header Location contains 'error'

  Scenario: Cargar el lobby y comprobar opciones de configuración (creación de sala)
    Given path '/lobby'
    When method get
    Then status 200
    And match response contains '¡Bienvenido a la sala de espera!'
    # Comprobar que los botones de tipo de partida están presentes
    And match response contains 'id="p_publica"'
    And match response contains 'id="p_privada"'
    # Comprobar que está la zona para invitar amigos
    And match response contains 'id="fetchUser"'

  Scenario: Denegar acceso si se intenta exceder el máximo de jugadores del lobby
    # Para probar esto hay que llenar a mano una lobby.
    Given path '/lobbies/join'
    And form field _csrf = csrfToken
    And form field code = 'SALA_LLENA' 
    When method post
    # Debería devolver un error de familia 400
    Then status 400
    # And match response contains 'La sala está llena'