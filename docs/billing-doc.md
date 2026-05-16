# Documentación del Módulo de Facturación (Billing)

*Última actualización: 15 de Mayo de 2026*

Este documento describe el funcionamiento principal del módulo de facturación (Billing), encargado de gestionar los planes de los usuarios y el procesamiento de pagos mediante Stripe.

## 1. Entidades Principales

El dominio está separado en dos responsabilidades clave para mantener el diseño limpio:

*   **UserPlan (Plan de Usuario):** Gestiona el nivel de acceso del usuario. Define si un usuario es `VISITOR`, `FREEMIUM` o `PREMIUM`. Controla las fechas de inicio y fin (`startDate`, `endDate`) de las suscripciones.
*   **PaymentRecord (Registro de Pago):** Exclusivo para registrar las transacciones monetarias. Guarda la relación con Stripe (`stripePaymentIntentId`), el monto, y el estado del pago (`PENDING`, `COMPLETED`, `FAILED`).

## 2. Tipos de Planes (Roles)

*   **VISITOR:** Asignado automáticamente cuando el usuario se registra en la plataforma.
*   **FREEMIUM:** Asignado cuando un usuario vincula un dispositivo IoT adquirido a su cuenta. (Sin fecha de expiración).
*   **PREMIUM:** Asignado cuando un usuario paga una suscripción. Tiene una duración predeterminada de 30 días.

## 3. Flujos de Trabajo Principales

### A. Registro de un Nuevo Usuario
1.  Cuando un usuario se registra (vía IAM), el sistema emite el evento `UserRegisteredEvent`.
2.  El manejador `UserRegisteredEventHandler` del módulo Billing escucha este evento y automáticamente le crea un `UserPlan` inicial con el nivel **VISITOR**.

### B. Proceso de Pago y Upgrade a Premium
1.  **Intención de Pago:** El usuario solicita una suscripción. El sistema llama a Stripe para generar un *Payment Intent* y paralelamente crea un `PaymentRecord` en estado **PENDING**.
2.  **Confirmación de Stripe (Webhook):** Cuando el pago se procesa exitosamente en Stripe, este envía un evento (`payment_intent.succeeded`) a nuestro webhook (`StripeWebhookController`).
3.  **Cumplimiento (Fulfillment):** El sistema procesa el webhook, marca el `PaymentRecord` como **COMPLETED** y lanza el evento interno `SubscriptionPaidEvent`.
4.  **Upgrade Automático:** El manejador `SubscriptionPaidEventHandler` reacciona a este evento, busca el `UserPlan` del usuario y lo actualiza a **PREMIUM**, estableciendo un tiempo de expiración de 30 días a partir de la fecha actual.

### C. Downgrade a Freemium
*   Se expone el endpoint `POST /api/v1/subscriptions/downgrade/{userId}` el cual ejecuta el `DowngradeToFreemiumCommand`.
*   Esto cambia el plan del usuario en su `UserPlan` de `PREMIUM` (o `VISITOR`) a **FREEMIUM**, removiendo cualquier fecha de expiración (`endDate = null`).

## 4. Identificadores (UUID)
Para estandarizar y asegurar la integridad de datos entre los módulos y la base de datos, las relaciones hacia el usuario (el `userId`) se manejan estrictamente con formato `UUID` nativo.
