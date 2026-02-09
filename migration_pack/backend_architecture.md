# Architecture Backend - Spring Boot (PostgreSQL)

## 1. Technologies & Stack

- **Langage** : Java 17 (LTS) ou 21.
- **Framework** : Spring Boot 3.2+
- **Base de Données** : PostgreSQL 15+.
- **ORM** : Spring Data JPA (Hibernate).
- **Sécurité** : Spring Security 6 + JWT (jjwt).
- **Outils** : Lombok, Maven/Gradle, Docker.
- **Documentation API** : OpenAPI (Swagger UI).

## 2. Structure du Projet

Package racine : `com.sucrestore.api`

```
com.sucrestore.api
├── config               # Configurations (Security, WebMvc, Swagger)
├── controller           # Points d'entrée REST (API)
├── dto                  # Data Transfer Objects (Request/Response)
├── entity               # Entités JPA mappées à la BD
├── exception            # Gestion globale des erreurs
├── repository           # Interfaces Spring Data JPA
├── security             # Filtres JWT et Service UserDetails
├── service              # Logique métier (Interfaces + Implémentations)
│   └── impl
└── util                 # Utilitaires (Date, String, etc.)
```

## 3. Entités JPA (Mapping)

Les entités correspondent aux tables définies dans la base de données.

- `User` (`@Entity`, `@Table(name="users")`)
- `Category` (`@Entity`, `@Table(name="categories")`)
- `Product` (`@Entity`, `@Table(name="products")`)
- `Order` (`@Entity`, `@Table(name="orders")`) -- Attention au mot-clé SQL `ORDER`, utiliser `@Table(name="orders")`.
- `OrderItem` (`@Entity`, `@Table(name="order_items")`)
- `SliderImage` (`@Entity`, `@Table(name="slider_images")`)

**Annotations Clés** :

- `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` (Postgres SERIAL/IDENTITY)
- `@Column(nullable = false, unique = true)`
- `@ManyToOne`, `@OneToMany(mappedBy = "...", cascade = CascadeType.ALL)`
- `@CreationTimestamp`, `@UpdateTimestamp`

## 4. Configuration (`application.yml`)

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dbstore
    username: your_postgres_user
    password: your_postgres_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update # ou validate en prod
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Configuration JWT Personnalisée
app:
  jwt:
    secret: 5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
    expiration: 86400000 # 24h
```

## 5. Dépendances Maven (pom.xml)

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## 8. Spécifications API Détaillées (Contrats)

### A. DTOs Commandes (Order)

Pour correspondre exactement au formulaire frontend du PHP :

**`OrderRequest.java`**

```java
public record OrderRequest(
    @NotBlank String customerName,
    @NotBlank String customerPhone,
    @NotBlank String customerAddress,
    String customerNotes,
    Double customerLatitude,
    Double customerLongitude,
    @NotEmpty List<OrderItemRequest> items
) {}

public record OrderItemRequest(
    @NotNull Long productId,
    @Min(1) int quantity
) {}
```

**`OrderResponse.java`**

```java
public record OrderResponse(
    boolean success,
    String message,
    String orderId,     // format "ORD-..."
    String whatsappLink // Généré coté backend ou frontend
) {}
```

### B. Sécurité & Contrôle d'Accès (PreAuthorize)

Correspondance avec `admin/config_admin.php` :

- **Super Admin** : `@PreAuthorize("hasRole('SUPER_ADMIN')")`
  - Accès total : `DELETE /api/admin/orders/{id}`, `POST /api/admin/users`.
- **Admin** : `@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")`
  - Gestion : `PUT /api/admin/orders/{id}`, `POST /api/admin/slider`.
  - Interdit de supprimer des commandes ou de gérer les utilisateurs.
- **Gestionnaire** : `@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")`
  - Lecture/Édition simple : `GET /api/admin/orders`, `PUT /api/admin/orders/{id}/status`.

### C. Gestion des Erreurs (GlobalExceptionHandler)

L'API doit renvoyer des erreurs structurées pour que le frontend React puisse afficher des Toasts :

```json
{
  "timestamp": "2024-03-20T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Le stock est insuffisant pour le produit 'X'",
  "path": "/api/orders"
}
```
