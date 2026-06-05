# Notifications Bounded Context Class Diagrams

This document contains both the unified Bounded Context class diagram and the layered individual diagrams.

## Unified Class Diagram

```mermaid
---
title: DDD Class Diagram - Notifications Bounded Context (4 Layers)
---

classDiagram

namespace interfaces {
    class NotificationController {
        +testEmail() ResponseEntity
    }
    class NotificationsContextFacade {
        <<interface>>
        +sendVerificationCode(email, code) void
    }
}

namespace application {
    class EmailCommandServiceImpl {
        -EmailDeliveryService emailDeliveryService
        -EmailLogRepository emailLogRepository
        +handle(SendVerificationCodeCommand) void
        +handle(SendWelcomeEmailCommand) void
    }
    class NotificationsContextFacadeImpl {
        -EmailCommandService emailCommandService
        +sendVerificationCode(email, code) void
    }
    class AlertIncidentChangedKafkaConsumer {
        -PushNotificationDeliveryService pushNotificationDeliveryService
        -PushNotificationLogRepository pushNotificationLogRepository
        -ExternalDeviceService externalDeviceService
        -ExternalAlertingService externalAlertingService
        +consume(record) void
    }
    class ExternalDeviceService {
        <<interface>>
        +fetchOwnerUserIdByDeviceId(deviceId) Optional~UUID~
    }
    class ExternalAlertingService {
        <<interface>>
        +findAlertDetailsById(alertId) Optional~AlertDetails~
    }
    class SendVerificationCodeCommand {
        +EmailRecipient recipient
        +String verificationCode
    }
    class SendWelcomeEmailCommand {
        +EmailRecipient recipient
        +String name
    }
}

namespace domain {
    class EmailLog {
        -UUID id
        -EmailRecipient recipient
        -EmailSubject subject
        -EmailContent content
        -String status
        -Instant sentAt
        -String errorMessage
    }
    class PushNotificationLog {
        -UUID id
        -UUID userId
        -String title
        -String message
        -String status
        -Instant sentAt
        -String externalId
    }
    class EmailDeliveryService {
        <<interface>>
        +send(recipient, subject, content) void
    }
    class PushNotificationDeliveryService {
        <<interface>>
        +sendPush(userId, title, message) String
    }
    class EmailRecipient {
        +String value
    }
    class EmailSubject {
        +String value
    }
    class EmailContent {
        +String value
    }
    class EmailLogRepository {
        <<interface>>
        +save(emailLog) EmailLog
    }
    class PushNotificationLogRepository {
        <<interface>>
        +save(pushNotificationLog) PushNotificationLog
    }
}

namespace infrastructure {
    class JpaEmailLogRepository {
        <<interface>>
    }
    class JpaPushNotificationLogRepository {
        <<interface>>
    }
    class SmtpEmailService {
        -JavaMailSender mailSender
        +send(recipient, subject, content) void
    }
    class OneSignalPushNotificationService {
        -String oneSignalAppId
        -String oneSignalApiKey
        +sendPush(userId, title, message) String
    }
}

NotificationController --> EmailCommandServiceImpl : uses
AlertIncidentChangedKafkaConsumer --> PushNotificationDeliveryService : uses
AlertIncidentChangedKafkaConsumer --> PushNotificationLogRepository : uses
AlertIncidentChangedKafkaConsumer --> ExternalDeviceService : uses
AlertIncidentChangedKafkaConsumer --> ExternalAlertingService : uses

EmailCommandServiceImpl --> EmailDeliveryService : uses
EmailCommandServiceImpl --> EmailLogRepository : uses

NotificationsContextFacadeImpl ..|> NotificationsContextFacade : implements
NotificationsContextFacadeImpl --> EmailCommandServiceImpl : uses

EmailLog --> EmailRecipient : contains
EmailLog --> EmailSubject : contains
EmailLog --> EmailContent : contains

SmtpEmailService ..|> EmailDeliveryService : implements
OneSignalPushNotificationService ..|> PushNotificationDeliveryService : implements
JpaEmailLogRepository ..|> EmailLogRepository : implements
JpaPushNotificationLogRepository ..|> PushNotificationLogRepository : implements
```

---

## Layered Diagrams

### 1. Interfaces Layer

```mermaid
classDiagram
class NotificationController {
    +testEmail() ResponseEntity
}
class NotificationsContextFacade {
    <<interface>>
    +sendVerificationCode(email, code) void
}
```

### 2. Application Layer

```mermaid
classDiagram
class EmailCommandServiceImpl {
    -EmailDeliveryService emailDeliveryService
    -EmailLogRepository emailLogRepository
    +handle(SendVerificationCodeCommand) void
    +handle(SendWelcomeEmailCommand) void
}
class NotificationsContextFacadeImpl {
    -EmailCommandService emailCommandService
    +sendVerificationCode(email, code) void
}
class AlertIncidentChangedKafkaConsumer {
    -PushNotificationDeliveryService pushNotificationDeliveryService
    -PushNotificationLogRepository pushNotificationLogRepository
    -ExternalDeviceService externalDeviceService
    -ExternalAlertingService externalAlertingService
    +consume(record) void
}
class ExternalDeviceService {
    <<interface>>
    +fetchOwnerUserIdByDeviceId(deviceId) Optional~UUID~
}
class ExternalAlertingService {
    <<interface>>
    +findAlertDetailsById(alertId) Optional~AlertDetails~
}
class SendVerificationCodeCommand {
    +EmailRecipient recipient
    +String verificationCode
}
class SendWelcomeEmailCommand {
    +EmailRecipient recipient
    +String name
}

NotificationsContextFacadeImpl --> EmailCommandServiceImpl : uses
AlertIncidentChangedKafkaConsumer --> ExternalDeviceService : uses
AlertIncidentChangedKafkaConsumer --> ExternalAlertingService : uses
```

### 3. Domain Layer

```mermaid
classDiagram
class EmailLog {
    -UUID id
    -EmailRecipient recipient
    -EmailSubject subject
    -EmailContent content
    -String status
    -Instant sentAt
    -String errorMessage
}
class PushNotificationLog {
    -UUID id
    -UUID userId
    -String title
    -String message
    -String status
    -Instant sentAt
    -String externalId
}
class EmailDeliveryService {
    <<interface>>
    +send(recipient, subject, content) void
}
class PushNotificationDeliveryService {
    <<interface>>
    +sendPush(userId, title, message) String
}
class EmailRecipient {
    +String value
}
class EmailSubject {
    +String value
}
class EmailContent {
    +String value
}
class EmailLogRepository {
    <<interface>>
    +save(emailLog) EmailLog
}
class PushNotificationLogRepository {
    <<interface>>
    +save(pushNotificationLog) PushNotificationLog
}

EmailLog --> EmailRecipient : contains
EmailLog --> EmailSubject : contains
EmailLog --> EmailContent : contains
```

### 4. Infrastructure Layer

```mermaid
classDiagram
class JpaEmailLogRepository {
    <<interface>>
}
class JpaPushNotificationLogRepository {
    <<interface>>
}
class SmtpEmailService {
    -JavaMailSender mailSender
    +send(recipient, subject, content) void
}
class OneSignalPushNotificationService {
    -String oneSignalAppId
    -String oneSignalApiKey
    +sendPush(userId, title, message) String
}
```
