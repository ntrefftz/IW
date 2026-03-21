# Frenemy

- en [src/main/java/es/ucm/fdi/iw](https://github.com/ntrefftz/IW/tree/main/src/main/java/es/ucm/fdi/iw) están los ficheros de configuración-mediante-código de la aplicación (ojo porque en otro sitio está el fichero principal de configuración-mediante-propiedades, [application.properties](https://github.com/ntrefftz/IW/blob/main/src/main/resources/application.properties)):

    * **AppConfig.java** - configura LocalData (usado para gestionar subida y bajada de ficheros de usuario) y fichero de internacionalización (que debería llamarse `Messages_XX.properties`, donde `XX` es un código como `es` para español ó `en` para inglés; y vivir en el directorio [resources](https://github.com/ntrefftz/IW/tree/main/src/main/resources).
    * **IwApplication.java** - punto de entrada de Spring Boot
    * **IwUserDetailsService.java** - autenticación mediante base de datos. Referenciado desde SecurityConfig.java. La base de datos se inicializa tras cada arranque desde el [import.sql](https://github.com/ntrefftz/IW/blob/main/src/main/resources/import.sql), aunque tocando [application.properties](https://github.com/ntrefftz/IW/blob/main/src/main/resources/application.properties) puedes hacer que se guarde y cargue de disco, ignorando el _import_.
    * **LocalData.java** - facilita guardar y devolver ficheros de usuario (es decir, que no forman parte de los fuentes de tu aplicación). Para ello colabora con AppConfig y usa el directorio especificado en [application.properties](https://github.com/ntrefftz/IW/blob/main/src/main/resources/application.properties)
    * **LoginSuccessHandler.java** - añade una variable de sesión llamada `u` nada más entrar un usuario, con la información de ese usuario. Esta variable es accesible desde Thymeleaf con `${session.user}`, y desde cualquier _Mapping_ de controllador usando el argumento `HttpSession session`, y leyendo su valor vía `(User)session.getAttribute("u")`. También añade a la sesión algo de configuración para websockets (variables `ws` y `url`), que se escriben como JS en las cabeceras de las páginas en el fragmento [head.html](https://github.com/ntrefftz/IW/blob/main/src/main/resources/templates/fragments/head.html).
    * **SecurityConfig.java** - establece la configuración de seguridad. Modifica su método `configure` para decir quién puede hacer qué, mediante `hasRole` y `permitAll`. 
    * **StartupConfig.java** - se ejecuta nada más lanzarse la aplicación. Se puede utilizar para lanzar la app en modo `debug` a partir del [application.properties](https://github.com/ntrefftz/IW/blob/main/src/main/resources/application.properties), accesible desde Thymeleaf mediante `${application.debug}`
    * **WebSocketConfig.java** - configura uso de websockets
    * **WebSocketSecurityConfig.java** - seguridad para websockets

- en [src/main/java/es/ucm/fdi/iw/controller](https://github.com/ntrefftz/IW/tree/main/src/main/java/es/ucm/fdi/iw/controller) hay 3 controladores:

  * **RootController.java** - para usuarios que acaban de llegar al sitio, gestiona los puntos accesibles por usuarios no registrados.
  * **AdminController.java** - para administradores, gestionando todo lo que hay bajo `/admin`. No hace casi nada, pero sólo pueden llegar allí los que tengan rol administrador (porque así lo dice en SecurityConfig.config)
  * **UserControlller.java** - para usuarios registrados, gestionando todo lo que hay bajo `/profile`. Tiene funcionalidad útil para construir páginas:
  
    + Un ejemplo de método para gestionar un formulario de cambiar información del usuario (bajo `@PostMapping("/{id}")`)
    + Puede devolver imágenes de avatar, y permite también subirlas. Ver métodos `getPic` (bajo `@GetMapping("{id}/pic")`) y `postPic` (bajo `@PostMapping("{id}/pic")`)
    + Puede gestionar también peticiones AJAX (= que no devuelven vistas) para consultar mensajes recibidos, consultar cuántos mensajes no-leídos tiene ese usuario, y enviar un mensaje a ese usuario (`retrieveMessages`, `checkUnread` y `postMsg`, respectivamente). Esta última función también envía el mensaje via websocket al usuario, si es que está conectado en ese momento.
    
- en [src/main/resources](https://github.com/ntrefftz/IW/tree/main/src/main/resources) están los recursos no-de-código-de-servidor, y en particular, las vistas, los recursos web estáticos, el contenido inicial de la BBDD, y las propiedades generales de la aplicación.

  * **static/**  - contiene recursos estáticos web, como ficheros .js, .css, ó imágenes que no cambian
  
    - **js/stomp.js** - necesario para usar STOMP sobre websockets (que es lo que usaremos para enviar y recibir mensajes)
    - **js/iw.js** - configura websockets, y contiene funciones de utilidad para gestionar AJAX y previsualización de imágenes

  * **templates/** - contiene vistas, y fragmentos de vista (en `templates/fragments`)
  
    - **fragments/head.html** - para incluir en el `<head>` de tus páginas. Incluída desde  
    - **fragments/nav.html** - para incluir al comienzo del `<body>`, contiene una navbar.
    - **fragments/footer.html** - para incluir al final del `<body>`, con un footer. Se debe tener en cuenta que es donde se cargan los .js de bootstrap, además de `stomp.js` e `iw.js`.
    - **error.html** - usada cuando se producen errores. Tiene un comportamiento muy distinto cuando la aplicación está en modo `debug` y cuando no lo está. 
    - **profile.html** - vista de usuario. Debería mostrar información sobre un usuario. Aun en desarrollo.
    - **admin.html** - aún en desarrollo.
    - **authors.html** - muestra el equipo de desarrollo con una foto de cada integrante.
    - **game.html** - vista de la partida. Debería mostrar el juego.
    - **index.html** - vista de la página principal. Muestra una breve explicación de la funcionalidad de la página web.
    - **lobby-select.html** - vista del selector de lobbies. Muestra lobbies creados por la gente en los que te puedes unir. También ha un botón para crear un lobby.
    - **lobby.html** - muestra la sala de espera antes de entrar a una partida.
    - **login.html** - muestra la página para iniciar sesión. Te pide un nombre de usuario y una contraseña.

  * **application.properties** - contiene la configuración general de la aplicación. Ojo porque ciertas configuraciones se hacen en los ficheros `XyzConfig.java` vistos anteriormente. Por ejemplo, qué roles pueden acceder a qué rutas se configura desde `SecurityConfig.java`.
  * **import.sql** - contiene código SQL para inicializar la BBDD. La configuración inicial hace que la BBDD se borre y reinicialice a cada arranque, lo cual es útil para pruebas. Es posible cambiarla para que la BBDD persista entre arraques de la aplicación, y se ignore el `import.sql`.
    





Proxima entrega, 26/03

Desplegar en VM -> Go con BD
50% funcionalidad -> La principal
README -> Actualizar con: descripción/propuesta
                          - img bd
                          - Que estado en vistas


## Estructura completa del proyecto (21/03/2026)

### Árbol de carpetas y archivos

```text
IW/
├── BDdiagram.png                            (Diagrama de la base de datos actualizado hasta la fecha)
├── LICENSE
├── README.md                                 
├── credentials.json.template                (Plantilla para archivo de credenciales del servidor remoto)
├── deploy.py                                (Script de inicialización para el servidor remoto)
├── iwdata/                                  (actualmente vacío; datos de usuario en disco)
├── pom.xml                                  (Archivo de configuración del proyecto)
├── requirements.txt                         (Archivo de dependencias)
├── src/
│   ├── main/
│   │   ├── java/ ["backend"]
│   │   │   └── es/ucm/fdi/iw/
│   │   │       ├── AppConfig.java
│   │   │       ├── IwApplication.java
│   │   │       ├── IwUserDetailsService.java
│   │   │       ├── LocalData.java
│   │   │       ├── LoginSuccessHandler.java
│   │   │       ├── SecurityConfig.java
│   │   │       ├── StartupConfig.java
│   │   │       ├── WebSocketConfig.java
│   │   │       ├── WebSocketSecurityConfig.java
│   │   │       ├── controller/
│   │   │       │   ├── AdminController.java
│   │   │       │   ├── ApiController.java
│   │   │       │   ├── RootController.java
│   │   │       │   └── UserController.java
│   │   │       └── model/
│   │   │           ├── Friendship.java
│   │   │           ├── Game.java
│   │   │           ├── Lorem.java
│   │   │           ├── Message.java
│   │   │           ├── Topic.java
│   │   │           ├── Transferable.java
│   │   │           └── User.java
│   │   └── resources/               ["Frontend"]
│   │       ├── application-container.properties
│   │       ├── application.properties
│   │       ├── import.sql
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   ├── admin.css
│   │       │   │   ├── bootstrap-5.3.3.css
│   │       │   │   ├── bootstrap.css.map
│   │       │   │   ├── custom.css
│   │       │   │   └── simple-datatables-10.css
│   │       │   ├── img/
│   │       │   │   ├── UNO-placeholder-pic.png
│   │       │   │   ├── default-pic.jpg
│   │       │   │   ├── dos.jpg
│   │       │   │   ├── favicon.ico
│   │       │   │   ├── favicon2.ico
│   │       │   │   ├── jff-pic.jpg
│   │       │   │   ├── ljg-pic.jpg
│   │       │   │   ├── logo.png
│   │       │   │   ├── nomercy.jpg
│   │       │   │   ├── ntf-pic.jpg
│   │       │   │   ├── pcs-pic.jpg
│   │       │   │   ├── pgg-pic.jpg
│   │       │   │   ├── profile_sketch.png
│   │       │   │   └── uno.jpg
│   │       │   └── js/
│   │       │       ├── admin.js
│   │       │       ├── ajax-demo.js
│   │       │       ├── bootstrap.bundle-5.3.3.js
│   │       │       ├── bootstrap.bundle.js.map
│   │       │       ├── iw.js
│   │       │       ├── js-eval.js
│   │       │       ├── lobby.js
│   │       │       ├── simple-datatables-10.js
│   │       │       ├── stomp.js
│   │       │       └── util.js
│   │       └── templates/
│   │           ├── admin.html
│   │           ├── authors.html
│   │           ├── error.html
│   │           ├── game.html
│   │           ├── index.html
│   │           ├── lobby-select.html
│   │           ├── lobby.html
│   │           ├── login.html
│   │           ├── profile.html
│   │           ├── user.html
│   │           └── fragments/
│   │               ├── footer.html
│   │               ├── head.html
│   │               └── nav.html
│   └── test/
│       └── java/
│           ├── karate-config.js
│           ├── logback-test.xml
│           ├── es/ucm/fdi/iw/
│           │   └── PlantillaApplicationTests.java
│           ├── external/
│           │   ├── ExternalRunner.java
│           │   ├── lobby.feature
│           │   ├── login.feature
│           │   └── ws.feature
│           └── internal/users/
│               ├── InternalRunner.java
│               └── users.feature
└── target/                                  (generado por Maven; no se edita a mano)
```

### Función de cada carpeta y archivo

#### Raíz del proyecto

- `.git/`: historial, referencias y metadatos del repositorio Git.
- `.gitignore`: define ficheros/carpetas que no se versionan (`target/`, credenciales, etc.).
- `.vscode/settings.json`: ajuste local de VS Code para análisis de nulls en Java.
- `BDdiagram.png`: diagrama de base de datos del proyecto.
- `LICENSE`: licencia de distribución del código.
- `README.md`: documentación principal del proyecto.
- `credentials.json.template`: plantilla de credenciales para despliegue remoto.
- `deploy.py`: script Python para empaquetar (`mvn package`), subir BD + datos + JAR y arrancar en servidor remoto mediante SSH/túnel.
- `iwdata/`: almacenamiento en disco de datos subidos por usuarios (por ejemplo avatares).
- `pom.xml`: configuración Maven (dependencias Spring Boot/JPA/Security/WebSocket/Karate, plugins y build).
- `requirements.txt`: dependencias Python para despliegue (`sshtunnel`, `fabric`).

#### `src/main/java/es/ucm/fdi/iw`

**Configuración y arranque**
- `IwApplication.java`: punto de entrada Spring Boot.
- `AppConfig.java`: define beans globales (`LocalData` y `MessageSource`).
- `SecurityConfig.java`: seguridad HTTP (rutas públicas/privadas, login, auth manager, encoder).
- `WebSocketConfig.java`: endpoint STOMP `/ws` y broker (`/topic`, `/queue`).
- `WebSocketSecurityConfig.java`: reglas de autorización para mensajes STOMP.
- `StartupConfig.java`: inicializa propiedad global `debug` en contexto al arrancar.
- `IwUserDetailsService.java`: carga usuarios/roles desde BD para autenticación.
- `LoginSuccessHandler.java`: al iniciar sesión guarda usuario y datos WS en sesión y redirige según rol.
- `LocalData.java`: utilidades para rutas/archivos persistidos en disco local.

**Controladores (`controller/`)**
- `RootController.java`: rutas públicas y de navegación base (`/`, `/login`, `/authors`, etc.).
- `AdminController.java`: administración de usuarios y consulta de mensajes globales.
- `UserController.java`: perfil de usuario, cambio de datos, foto de perfil, mensajería privada y contadores de no leídos.
- `ApiController.java`: API REST auxiliar (status, conteo usuarios, chat por tópicos y evaluación JS con `karate-js`).

**Modelo (`model/`)**
- `User.java`: entidad de usuario, roles, relaciones y DTO de transferencia.
- `Message.java`: entidad de mensajes (privados o de tópico) y DTO JSON.
- `Topic.java`: entidad de grupo/canal con miembros y mensajes.
- `Game.java`: entidad de partida/lobby, estado y ganador.
- `Friendship.java`: entidad de relación entre jugadores y métricas entre ambos.
- `Transferable.java`: interfaz común para convertir entidades a DTOs de transferencia.
- `Lorem.java`: utilidades para generar nombres/apellidos de prueba.

#### `src/main/resources`

**Configuración**
- `application.properties`: configuración local/dev (H2 en memoria, debug activo, rutas, logging).
- `application-container.properties`: configuración de despliegue (puerto 80, H2 en fichero, debug desactivado, caches activas).
- `import.sql`: datos iniciales y usuarios de arranque (`a`, `b`) + reinicio de secuencia.

**Recursos web estáticos (`static/`)**
- `css/admin.css`: estilos específicos de la vista de administración.
- `css/custom.css`: estilos personalizados globales.
- `css/bootstrap-5.3.3.css`: framework CSS Bootstrap (vendor).
- `css/bootstrap.css.map`: source map de Bootstrap.
- `css/simple-datatables-10.css`: estilos de DataTables (vendor).

- `js/iw.js`: utilidades JS base (`fetch` con CSRF, carga/subida de imágenes, inicialización WebSocket).
- `js/ajax-demo.js`: demo de AJAX y mensajería para la vista de usuario.
- `js/admin.js`: interacción de modal y tabla en panel admin.
- `js/lobby.js`: alterna UI de partida pública/privada en lobby.
- `js/util.js`: librería de utilidades JS (DOM, random, validaciones, escape/XSS, etc.).
- `js/js-eval.js`: función mínima usada por API para probar evaluación JS en servidor.
- `js/bootstrap.bundle-5.3.3.js`: JS de Bootstrap (vendor).
- `js/bootstrap.bundle.js.map`: source map de Bootstrap JS.
- `js/simple-datatables-10.js`: librería DataTables (vendor).
- `js/stomp.js`: cliente STOMP para WebSocket.

- `img/logo.png`: logo usado en navegación.
- `img/favicon.ico`, `img/favicon2.ico`: iconos del sitio.
- `img/default-pic.jpg`: avatar por defecto cuando un usuario no subió foto.
- `img/profile_sketch.png`: recurso gráfico de perfil.
- `img/UNO-placeholder-pic.png`: placeholder visual en la vista de partida.
- `img/uno.jpg`, `img/dos.jpg`, `img/nomercy.jpg`: imágenes de modos/juegos en lobby.
- `img/jff-pic.jpg`, `img/ljg-pic.jpg`, `img/ntf-pic.jpg`, `img/pcs-pic.jpg`, `img/pgg-pic.jpg`: fotos del equipo en `authors.html`.

**Plantillas Thymeleaf (`templates/`)**
- `index.html`: portada con descripción del proyecto.
- `login.html`: formulario de autenticación.
- `authors.html`: presentación del equipo de desarrollo.
- `admin.html`: panel de administración y listados con acciones.
- `user.html`: perfil funcional de usuario (mensajes, avatar, utilidades AJAX).
- `profile.html`: maqueta/preview alternativa de perfil con estadísticas.
- `lobby-select.html`: selector de lobbies y UI para unirse/filtrar.
- `lobby.html`: sala de espera y configuración pública/privada.
- `game.html`: maqueta provisional de pantalla de partida.
- `error.html`: página de error (modo normal y modo debug).

**Fragmentos reutilizables (`templates/fragments/`)**
- `head.html`: cabecera común (CSS, favicon y objeto JS `config`).
- `nav.html`: barra de navegación dinámica según autenticación/rol.
- `footer.html`: pie común y carga base de scripts globales.

#### `src/test/java`

- `es/ucm/fdi/iw/PlantillaApplicationTests.java`: tests JUnit/Spring Boot con MockMvc para endpoints API básicos.
- `external/ExternalRunner.java`: runner Karate de pruebas de integración externas.
- `external/login.feature`: escenarios E2E de login/logout.
- `external/lobby.feature`: escenarios E2E de flujo de lobbies.
- `external/ws.feature`: escenario E2E de mensajería vía WebSocket.
- `internal/users/InternalRunner.java`: runner Karate para pruebas internas de usuarios.
- `internal/users/users.feature`: ejemplos de pruebas API HTTP (jsonplaceholder como muestra).
- `karate-config.js`: configuración global Karate (baseUrl, driver, entorno).
- `logback-test.xml`: configuración de logging durante tests (consola + `target/karate.log`).

#### `target/` se reconstruye automáticamente con Maven; no debe editarse manualmente.

                          