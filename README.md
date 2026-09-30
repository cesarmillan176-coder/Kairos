# Kairos

**Kairos** es una aplicación web para buscar series de televisión, consultar sus detalles y compartir opiniones con calificaciones. La información de las series proviene de la API pública de [TVmaze](https://www.tvmaze.com/api), mientras que las series consultadas y los comentarios de los usuarios se guardan en **MongoDB**.

- **Frontend publicado:** [kairos-deploy.web.app](https://kairos-deploy.web.app)
- **Backend API:** [kairos-back-923012871834.us-central1.run.app](https://kairos-back-923012871834.us-central1.run.app/show?show_id=1)

---

## 🛠️ Tecnologías

- **Backend:** Java 21, Spring Boot 3.4 / 4.x, Spring Data MongoDB, RestClient.
- **Frontend:** HTML5, CSS3, JavaScript (Vanilla ES6+), Vite.
- **Base de Datos:** MongoDB.
- **Infraestructura & Despliegue:** Docker, Cloud Run, Firebase Hosting.

---

## 🚀 Ejecución en Local

### Requisitos
- Java 21 y Maven 3.8+
- Node.js 20+
- MongoDB 7+ (local o mediante Docker)

### 1. Iniciar MongoDB (opcional con Docker)
```bash
docker compose -f back/compose.yml up -d
```

### 2. Iniciar el Backend
```bash
mvn -f back/pom.xml spring-boot:run
```
*El backend se ejecutará en `http://localhost:8080`.*

### 3. Iniciar el Frontend
En otra terminal:
```bash
npm --prefix front install
npm --prefix front run dev
```
*Abre `http://localhost:5173` en tu navegador.*

---

## 📌 Endpoints Principales

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/search?search_query={nombre}` | Busca series en TVmaze e incluye los comentarios guardados. |
| `GET` | `/show?show_id={id}` | Obtiene el detalle de una serie (con caché en MongoDB) y sus opiniones. |
| `POST` | `/comments` | Guarda una opinión (`show_id`, `comment`, `rating` de 0 a 5). |

### Ejemplos de uso (cURL)

**Buscar una serie:**
```bash
curl "http://localhost:8080/search?search_query=dark"
```

**Guardar un comentario:**
```bash
curl -X POST "http://localhost:8080/comments" \
  -H "Content-Type: application/json" \
  -d '{"show_id": 1, "comment": "Excelente serie", "rating": 5}'
```

---

## 🧪 Pruebas

Para ejecutar las pruebas unitarias:
```bash
mvn -f back/pom.xml test
```

Para ejecutar las pruebas de integración (requiere MongoDB activo):
```bash
mvn -f back/pom.xml clean verify -Pintegration
```

---

## 📁 Estructura del Proyecto

```
Kairos/
├── back/             # Proyecto Spring Boot (API REST)
├── front/            # Aplicación Frontend (Vite + Vanilla JS)
└── docs/             # Colección Postman y documentación PDF
```
