# Hotel Manager API 🏨

API REST para la gestión de un hotel: usuarios, habitaciones y reservas. Desarrollada con **Java 21** y **Spring Boot**, con especial atención a la **seguridad**: autenticación con JWT, control de acceso por roles y protección del login frente a ataques de fuerza bruta.

> 🚧 El frontend (HTML, CSS, JavaScript y Bootstrap) está en desarrollo y se publicará más adelante.

## ✨ Funcionalidades

- 👤 **Registro e inicio de sesión** de usuarios con contraseñas cifradas con **BCrypt**.
- 🔐 **Autenticación sin estado con JWT**, validada en cada petición mediante un filtro propio.
- 🛡️ **Control de acceso por roles** (`CLIENTE` y `ADMIN`) con Spring Security.
- 🚦 **Rate limiting en todos los inicios de sesión** (clientes y portal de empleados): máximo 5 intentos por minuto por IP para frenar ataques de fuerza bruta.
- 🛏️ **Búsqueda de habitaciones disponibles** por fechas, tipo y precio máximo.
- 📅 **Gestión de reservas**: crear, consultar y cancelar las reservas propias.
- 📊 **Panel de administración**: estadísticas, gestión de reservas y habitaciones, y exportación de reportes en CSV.
- ⚠️ **Gestión centralizada de errores** con respuestas JSON coherentes.
- ✅ **Pruebas de integración** de autenticación, tokens y permisos.

## 🛠️ Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot, Spring Web MVC |
| Seguridad | Spring Security, JWT (jjwt), BCrypt |
| Persistencia | Spring Data JPA, Hibernate, MySQL |
| Pruebas | JUnit 5, MockMvc, Mockito |
| Build | Maven |

## 🧱 Arquitectura

Arquitectura por capas, separando responsabilidades:

```
src/main/java/com/hotel/
├── config/         # Configuración de Spring Security y CORS
├── controllers/    # Endpoints REST y gestión global de excepciones
├── dto/            # Objetos de entrada y salida de la API
├── filters/        # Filtro JWT y rate limiting
├── listeners/      # Eventos de arranque y parada
├── models/         # Entidades JPA (Usuario, Habitacion, Reserva)
├── repositories/   # Acceso a datos con Spring Data JPA
├── services/       # Lógica de negocio
└── utils/          # Generación y validación de tokens JWT
```

## 🔌 Endpoints principales

URL base: `http://localhost:8080/hotel-manager`

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/registro` | Público | Registro de un nuevo cliente |
| `POST` | `/api/login` | Público (rate limit) | Inicio de sesión; devuelve un JWT |
| `GET` | `/api/habitaciones/disponibles` | Público | Habitaciones disponibles con filtros |
| `GET` `POST` `DELETE` | `/api/reservas` | Usuario autenticado | Consultar, crear y cancelar reservas propias |
| `PUT` | `/api/usuarios/password` | Usuario autenticado | Cambiar la contraseña |
| `GET` | `/api/admin/estadisticas` | `ADMIN` | Estadísticas del hotel |
| `GET` `POST` `DELETE` | `/api/admin/reservas` | `ADMIN` | Gestión de todas las reservas |
| `PUT` | `/api/admin/habitaciones` | `ADMIN` | Modificar habitaciones |
| `GET` | `/api/admin/reportes/reservas` | `ADMIN` | Exportar reservas en CSV |

Las rutas protegidas requieren la cabecera:

```
Authorization: Bearer <token>
```

## 🚀 Puesta en marcha

### Requisitos

- Java 21
- MySQL 8 en ejecución

### 1. Clona el repositorio

```bash
git clone https://github.com/andreapg-tech/hotel-manager-api.git
cd hotel-manager-api
```

### 2. Crea la base de datos

```sql
CREATE DATABASE hotelweb;
```

Las tablas se crean automáticamente al arrancar la aplicación.

### 3. Configura las variables de entorno

La clave de firma de los JWT y las credenciales de la base de datos **no se guardan en el repositorio**. Copia el archivo de ejemplo y edítalo:

```bash
cp .env.example .env
```

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `JWT_SECRET` | Clave para firmar los tokens (mínimo 32 caracteres) | *Obligatoria* |
| `DB_URL` | URL de conexión a MySQL | `jdbc:mysql://localhost:3306/hotelweb?serverTimezone=UTC` |
| `DB_USERNAME` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Contraseña de MySQL | *(vacía)* |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend autorizados, separados por comas | `http://localhost:5500,http://127.0.0.1:5500` |

El archivo `.env` está en el `.gitignore`, así que nunca se sube al repositorio.

### 4. Arranca la API

```bash
./mvnw spring-boot:run
```

En Windows: `mvnw.cmd spring-boot:run`

## ✅ Pruebas

```bash
./mvnw test
```

Las pruebas arrancan el contexto completo de Spring, así que MySQL debe estar en ejecución. Usan su propia clave JWT de prueba (`src/test/resources/config/application.properties`), por lo que no necesitan el archivo `.env`.

Las pruebas de integración comprueban, entre otros casos:

- Inicio de sesión correcto y generación del token.
- Rechazo de peticiones sin token o con un token inválido.
- Acceso denegado cuando un `CLIENTE` intenta usar rutas de `ADMIN`.
- Respuestas correctas ante rutas inexistentes y métodos no soportados.

## 👤 Autora

**Andrea Pérez González** · [LinkedIn](https://www.linkedin.com/in/andreap%C3%A9rezgonz%C3%A1lez) · [GitHub](https://github.com/andreapg-tech)
