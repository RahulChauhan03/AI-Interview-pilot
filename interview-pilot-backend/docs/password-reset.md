# Password reset backend

The feature extends the existing package layout rather than creating parallel layers: `auth/controller` exposes the endpoints, `auth/service` owns the workflow and mail sender, `auth/repository` stores reset records, `auth/entity` maps the MySQL table, `auth/dto` validates request bodies, and the existing `security/config` makes the endpoints public and supplies BCrypt. Existing `exception` and `common/response` provide safe error envelopes. The project does not currently need a separate `util` or `config` package for this feature.

## Setup

`spring-boot-starter-mail` is included in `pom.xml`; it provides Spring's `JavaMailSender` over SMTP. Configure the environment, never Java source:

```bash
export MAIL_USERNAME='your-gmail-address@gmail.com'
export MAIL_PASSWORD='your-16-character-gmail-app-password'
export MAIL_FROM='your-gmail-address@gmail.com'
export FRONTEND_RESET_PASSWORD_URL='http://localhost:4200/reset-password'
```

For Gmail, enable two-step verification and create a Google App Password; do not use the normal account password. `MAIL_USERNAME` authenticates to the SMTP relay, `MAIL_PASSWORD` is the relay/app password, `MAIL_FROM` is the visible sender (normally the same authenticated address), and the recipient is the user's database email. The properties use Gmail submission defaults: host `smtp.gmail.com`, port `587`, SMTP authentication, and STARTTLS. For another provider, set `MAIL_HOST`, `MAIL_PORT`, and its TLS/auth settings instead.

`spring.mail.host` and `spring.mail.port` identify the SMTP server; `spring.mail.username` and `spring.mail.password` provide its credentials; `mail.smtp.auth` enables SMTP authentication; and `mail.smtp.starttls.enable` upgrades the connection to TLS. The three timeout properties prevent an unavailable SMTP server from holding the HTTP request indefinitely. `app.frontend.reset-password-url` is the Angular route prefix, while `app.password-reset.token-expiration-minutes` and `app.password-reset.request-cooldown-seconds` control token lifetime and throttling without a code change.

The provided SQL migration matches the entity. It is ready for Flyway if Flyway is introduced; with the current `spring.jpa.hibernate.ddl-auto=create`, Hibernate creates the same table for development. Use migrations and `validate`/`update` rather than `create` outside local development.

## API and security behavior

`POST /api/auth/forgot-password` accepts `{ "email": "user@example.com" }` and always returns HTTP 200 with the same generic message for a syntactically valid address. This prevents attackers from discovering registered accounts. Known accounts receive a 256-bit `SecureRandom` token encoded as URL-safe Base64; only its SHA-256 hash is stored. A database leak therefore does not yield usable reset links. Tokens expire after 30 minutes by default, are single-use, and issuing a new one invalidates previous active tokens.

`POST /api/auth/reset-password` accepts `{ "token": "...", "newPassword": "NewPassword@123" }`. It hashes the submitted token, verifies that its stored record is present, unused, and unexpired, BCrypt-encodes the new password using the existing application encoder, then invalidates all reset tokens for that user. Passwords are never emailed or stored in plaintext. BCrypt is intentionally slow and salted for passwords; general hashes such as MD5, SHA-1, and SHA-256 are not suitable password storage.

The reset request has a per-account 60-second cooldown to reduce token and mail spam. In production, also put an IP/user rate limiter at the gateway (for example Bucket4j, Redis, or a reverse proxy), monitor SMTP failures, and enforce provider sending limits. The frontend should compare a confirm-password field for usability; the backend remains authoritative for nonblank, length, upper-case, lower-case, and digit validation.

Invalid or used tokens return 400, expired tokens return 410, malformed request bodies return 400, and mail infrastructure failures return a generic 500 without exposing SMTP details. Unknown email addresses deliberately never return 404.

## Flow

```text
Angular -> AuthController -> AuthService -> UserRepository
                                      -> secure token + SHA-256 hash
                                      -> PasswordResetTokenRepository -> EmailService -> SMTP
Angular reset screen -> AuthController -> AuthService -> validate hash/expiry/used
                                      -> BCrypt PasswordEncoder -> user password + token invalidation
```
