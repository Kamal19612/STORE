-- =================================================================================
-- SCHÉMA DE BASE DE DONNÉES - PROJET MIGRATION SPRING BOOT (PostgreSQL)
-- SGBD : PostgreSQL 15+
-- Application : SUCRE STORE
-- =================================================================================

-- Nettoyage préalable (Optionnel)
DROP TABLE IF EXISTS "slider_images", "order_items", "orders", "products", "categories", "users" CASCADE;
DROP TYPE IF EXISTS "user_role", "order_status";

-- ---------------------------------------------------------------------------------
-- 1. Enum Types (PostgreSQL Specific)
-- ---------------------------------------------------------------------------------
CREATE TYPE "user_role" AS ENUM ('SUPER_ADMIN', 'ADMIN', 'MANAGER');
CREATE TYPE "order_status" AS ENUM ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED');

-- ---------------------------------------------------------------------------------
-- 2. Table : USERS
-- ---------------------------------------------------------------------------------
CREATE TABLE "users" (
  "id" BIGSERIAL PRIMARY KEY,
  "username" VARCHAR(50) NOT NULL UNIQUE,
  "email" VARCHAR(100) NOT NULL UNIQUE,
  "password" VARCHAR(255) NOT NULL,
  "role" "user_role" NOT NULL DEFAULT 'MANAGER',
  "active" BOOLEAN DEFAULT TRUE,
  "last_login" TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL,
  "created_at" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX "idx_users_email" ON "users" ("email");
CREATE INDEX "idx_users_role" ON "users" ("role");

-- ---------------------------------------------------------------------------------
-- 3. Table : CATEGORIES
-- ---------------------------------------------------------------------------------
CREATE TABLE "categories" (
  "id" BIGSERIAL PRIMARY KEY,
  "name" VARCHAR(100) NOT NULL UNIQUE,
  "slug" VARCHAR(100) NOT NULL UNIQUE,
  "description" TEXT DEFAULT NULL,
  "active" BOOLEAN DEFAULT TRUE,
  "created_at" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------------------
-- 4. Table : PRODUCTS
-- ---------------------------------------------------------------------------------
CREATE TABLE "products" (
  "id" BIGSERIAL PRIMARY KEY,
  "category_id" BIGINT NOT NULL,
  "name" VARCHAR(255) NOT NULL,
  "slug" VARCHAR(255) NOT NULL UNIQUE,
  "description" TEXT,
  "price" DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  "stock_quantity" INT DEFAULT 0,
  "image_url" VARCHAR(255) DEFAULT NULL,
  "available" BOOLEAN DEFAULT TRUE,
  "created_at" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fk_products_category" FOREIGN KEY ("category_id") REFERENCES "categories" ("id") ON DELETE RESTRICT
);

CREATE INDEX "idx_products_category" ON "products" ("category_id");
CREATE INDEX "idx_products_available" ON "products" ("available");

-- ---------------------------------------------------------------------------------
-- 5. Table : ORDERS
-- ---------------------------------------------------------------------------------
CREATE TABLE "orders" (
  "id" BIGSERIAL PRIMARY KEY,
  "order_number" VARCHAR(20) NOT NULL UNIQUE,
  "customer_name" VARCHAR(100) NOT NULL,
  "customer_phone" VARCHAR(20) NOT NULL,
  "customer_address" TEXT NOT NULL,
  "customer_notes" TEXT DEFAULT NULL,
  "customer_latitude" DECIMAL(10, 8) DEFAULT NULL,
  "customer_longitude" DECIMAL(11, 8) DEFAULT NULL,
  "subtotal" DECIMAL(10, 2) NOT NULL,
  "tax" DECIMAL(10, 2) DEFAULT 0.00,
  "total" DECIMAL(10, 2) NOT NULL,
  "status" "order_status" NOT NULL DEFAULT 'PENDING',
  "created_at" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX "idx_orders_status" ON "orders" ("status");
CREATE INDEX "idx_orders_phone" ON "orders" ("customer_phone");
CREATE INDEX "idx_orders_date" ON "orders" ("created_at");

-- ---------------------------------------------------------------------------------
-- 6. Table : ORDER_ITEMS
-- ---------------------------------------------------------------------------------
CREATE TABLE "order_items" (
  "id" BIGSERIAL PRIMARY KEY,
  "order_id" BIGINT NOT NULL,
  "product_id" BIGINT DEFAULT NULL,
  "product_name" VARCHAR(255) NOT NULL,
  "product_price" DECIMAL(10, 2) NOT NULL,
  "quantity" INT NOT NULL,
  "subtotal" DECIMAL(10, 2) NOT NULL,
  CONSTRAINT "fk_items_order" FOREIGN KEY ("order_id") REFERENCES "orders" ("id") ON DELETE CASCADE,
  CONSTRAINT "fk_items_product" FOREIGN KEY ("product_id") REFERENCES "products" ("id") ON DELETE SET NULL
);

-- ---------------------------------------------------------------------------------
-- 7. Table : SLIDER_IMAGES
-- Source: admin/slider.php
-- ---------------------------------------------------------------------------------
CREATE TABLE "slider_images" (
  "id" BIGSERIAL PRIMARY KEY,
  "title" VARCHAR(100) DEFAULT NULL,   -- Titre optionnel pour SEO/Alt
  "image_url" VARCHAR(255) NOT NULL,   -- Chemin relatif ou URL absolue
  "link_url" VARCHAR(255) DEFAULT NULL, -- Lien de redirection au clic
  "display_order" INT DEFAULT 0,       -- Pour trier les slides (ORDER BY display_order ASC)
  "active" BOOLEAN DEFAULT TRUE,       -- Pour masquer sans supprimer
  "created_at" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Note: In PostgreSQL, updating 'updated_at' usually requires a trigger.
-- For Spring Boot JPA, simply use @UpdateTimestamp on the entity field to handle logic effortlessly without triggers.
