# Spring Boot LDAP sample

A server-rendered Thymeleaf catalog with Spring Boot 3.5.16, Spring Security
6.5.11, Java 21, Maven, and H2. The Product and User modules each have a
controller, DTOs, service, and repository. Products use a JPA entity in H2.
Users are stored and managed in LLDAP.

## Run locally

Install JDK 21 and start Docker Desktop. On macOS, select JDK 21 in your terminal.

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
docker compose up -d
docker compose wait ldap-bootstrap
docker compose ps -a
./mvnw spring-boot:run
```

Wait for `ldap` to show as healthy and for `ldap-bootstrap` to exit with
code 0 before starting Spring Boot. If either fails, read the service logs with
`docker compose logs ldap ldap-bootstrap`. The Maven Wrapper downloads Maven
on its first run.

Open [the login page](http://127.0.0.1:8080/login).

| Role | Username | Password | Modules |
| --- | --- | --- | --- |
| USER | `demo-user` | `user-local-password` | Products |
| ADMIN | `demo-admin` | `admin-local-password` | Products and Users |

1. Sign in as `demo-user`. Add a product, open its details, edit it, and use
   the delete confirmation. Visiting `/admin/users` directly returns 403.
2. Sign out and sign in as `demo-admin`. Open **Users**, choose **Add user**,
   and enter a new username, display name, email, password, and role. Choose
   `ROLE_USER` for Products or `ROLE_ADMIN` for Products and Users. The app
   creates the directory account with the selected role. Sign out and use the new
   credentials to sign in. Admins can edit account names, email addresses, and roles
   or delete accounts from the Users module.
3. Run `./mvnw test` for the application tests.

## How it works

Spring Security searches for the LDAP account under `ou=people` and binds with
the submitted password. Directory groups are named `ROLE_USER` and
`ROLE_ADMIN` and grant those Spring Security authorities directly. The app
uses the read-only `app-reader` account for authentication lookups. The Users
module uses LLDAP's management API for account records and LDAP to set the
initial password. It manages application users and admins and excludes directory service
accounts. The role selector defaults to `ROLE_USER` for new accounts. Saving
a role replaces the other application role. Existing sessions keep their
permissions until the next sign-in. Browser sessions and CSRF protection cover
login, logout, and CRUD form submissions.

For an existing volume, run `docker compose up -d` followed by
`docker compose wait ldap-bootstrap` to add the new role groups to the demo
accounts. For additional accounts, assign the new role groups in LLDAP.
The existing volume can be reused.

The sample binds the application, LLDAP management API, and LDAP ports to loopback and uses fixed local
demo passwords. A deployed configuration should use TLS for browser and LDAP
traffic and source service credentials from a secret manager.

Stop Spring Boot with Ctrl+C and run `docker compose down` to stop LDAP.
H2 data is in memory and resets when Spring Boot restarts. The named Docker
volume preserves LDAP accounts across restarts. Set `LLDAP_ADMIN_PASSWORD`
for both Compose and the application when using a different directory admin
password.
