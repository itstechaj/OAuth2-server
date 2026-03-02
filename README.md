# OAuth 2.0 Server — Architecture & Flow Diagram

## Why LoginPageController exists in Auth Server

The `LoginPageController` in auth-server is **not** a login implementation — it's a **redirect stub**. Here's why it's needed:

Spring Security's form login requires a `/login` URL mapping on the **same server**. When an unauthenticated user hits `/authorize`, Spring Security redirects to `/login` **on port 9000**. The `LoginPageController` **catches that redirect** and immediately forwards to the Login Server at `http://localhost:8081/login`. It contains zero login logic — just one redirect.

```
Spring Security: "User not authenticated → redirect to /login"
          ↓
LoginPageController: "redirect to http://localhost:8081/login"
          ↓
Login Server: "Show the actual login form"
```

Without it, Spring Security would show its own default login form on port 9000, defeating the purpose of a separate login server.

---

## Complete Architecture

```mermaid
graph TB
    subgraph "Auth Server :9000"
        A["/authorize endpoint"]
        B["LoginPageController /login"]
        C["Spring Security Filter"]
        D["ExternalAuthProvider"]
        E["/oauth2/token endpoint"]
        F["/api/clients endpoint"]
        G["/api/introspect endpoint"]
        H["JDBC Client Repository"]
        I["JWT Token Generator"]
    end

    subgraph "Login Server :8081"
        J["Login/Signup UI"]
        K["/api/authenticate"]
        L["UserRepository"]
    end

    subgraph "MySQL :3306 — oauth2_db"
        M["oauth2_registered_client"]
        N["oauth2_authorization"]
        O["users"]
    end

    F -->|store client| H
    H -->|read/write| M
    A -->|unauthenticated| C
    C -->|redirect| B
    B -->|redirect to :8081| J
    D -->|validate credentials| K
    K -->|query| L
    L -->|read/write| O
    E -->|issue token| I
    E -->|lookup client| H
    E -->|store authorization| N
    G -->|decode JWT| I
```

---

## Authorization Code Flow (step-by-step)

```mermaid
sequenceDiagram
    participant Client as Client App
    participant AS as Auth Server :9000
    participant SS as Spring Security
    participant LP as LoginPageController
    participant LS as Login Server :8081
    participant DB as MySQL

    Client->>AS: GET /authorize?response_type=code&client_id=X&redirect_uri=Y&scope=Z&state=S
    AS->>SS: Check authentication
    SS->>LP: User not authenticated → redirect /login
    LP->>LS: HTTP 302 → http://localhost:8081/login?auth_server_url=...
    LS->>LS: Show login/signup form
    Note over LS: User enters username & password
    LS->>AS: POST /login (username, password) — form submits directly to auth server
    AS->>SS: Process login
    SS->>LS: REST call POST /api/authenticate (validate credentials)
    LS->>DB: Query users table
    DB->>LS: User found, password matches
    LS->>SS: 200 OK
    SS->>AS: Authentication success
    AS->>AS: Generate authorization code
    AS->>DB: Store authorization in oauth2_authorization
    AS->>Client: HTTP 302 → redirect_uri?code=ABC&state=S
    Client->>AS: POST /oauth2/token (code=ABC, client_id, client_secret)
    AS->>DB: Validate code from oauth2_authorization
    AS->>Client: JWT access_token + refresh_token
    Client->>AS: POST /api/introspect (token=...)
    AS->>Client: {active: true, client_id, sub, scopes, exp, ...}
```

---

## Client Credentials Flow

```mermaid
sequenceDiagram
    participant Client as Client App
    participant AS as Auth Server :9000
    participant DB as MySQL

    Client->>AS: POST /oauth2/token (grant_type=client_credentials, client_id, client_secret, scope)
    AS->>DB: Lookup client in oauth2_registered_client
    DB->>AS: Client found, secret matches
    AS->>Client: JWT access_token
    Client->>AS: POST /api/introspect (token=...)
    AS->>Client: {active: true, client_id, scopes, exp, ...}
```

---

## Client Registration Flow

```mermaid
sequenceDiagram
    participant Admin as Admin/Developer
    participant AS as Auth Server :9000
    participant DB as MySQL

    Admin->>AS: POST /api/clients {clientId, scopes, grantTypes, redirectUri}
    AS->>AS: Generate UUID client_secret
    AS->>AS: Encode secret with BCrypt
    AS->>DB: Store in oauth2_registered_client
    AS->>Admin: {client_id, client_secret (raw), scopes, grant_types}
```
