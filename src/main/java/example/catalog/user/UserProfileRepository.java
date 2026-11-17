package example.catalog.user;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.BasicAttribute;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Repository
class UserProfileRepository {
    private final RestClient api;
    private final BaseLdapPathContextSource ldap;
    private final String password;

    UserProfileRepository(RestClient.Builder builder, BaseLdapPathContextSource ldap,
                          @Value("${catalog.directory.api-url}") URI endpoint,
                          @Value("${catalog.directory.admin-password}") String password) {
        var transport = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var requests = new JdkClientHttpRequestFactory(transport);
        requests.setReadTimeout(Duration.ofSeconds(5));
        this.api = builder.baseUrl(endpoint.toString()).requestFactory(requests).build();
        this.ldap = ldap;
        this.password = password;
    }

    Session connect() {
        try {
            var login = api.post().uri("/auth/simple/login")
                    .body(Map.of("username", "admin", "password", password))
                    .retrieve().body(JsonNode.class);
            if (login == null || login.path("token").asText().isBlank()) {
                throw new ProfileUsernameException("The directory management login failed.");
            }
            return new Session(login.path("token").asText());
        } catch (RestClientException exception) {
            throw new ProfileUsernameException("The directory management service is unavailable.");
        }
    }

    void changePassword(String username, String newPassword) {
        var admin = LdapNameBuilder.newInstance(ldap.getBaseLdapName()).add("ou", "people")
                .add("uid", "admin").build();
        var target = LdapNameBuilder.newInstance().add("ou", "people")
                .add("uid", username).build();
        var context = ldap.getContext(admin.toString(), password);
        try {
            context.modifyAttributes(target, new ModificationItem[]{
                    new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", newPassword))
            });
        } catch (javax.naming.NamingException exception) {
            throw new ProfileUsernameException("The directory could not set the password.");
        } finally {
            org.springframework.ldap.support.LdapUtils.closeContext(context);
        }
    }

    final class Session {
        private final String token;

        private Session(String token) {
            this.token = token;
        }

        List<UserProfileView> list() {
            var result = execute("""
                    { users(filters: {memberOf: "ROLE_USER"}) {
                        id displayName email groups { displayName }
                    } }
                    """, Map.of());
            return StreamSupport.stream(result.path("users").spliterator(), false)
                    .filter(user -> StreamSupport.stream(user.path("groups").spliterator(), false)
                            .noneMatch(group -> {
                                String name = group.path("displayName").asText();
                                return name.equalsIgnoreCase("ROLE_ADMIN") || name.startsWith("lldap_");
                            }))
                    .map(user -> new UserProfileView(user.path("id").asText(),
                            user.path("displayName").asText(), user.path("email").asText()))
                    .toList();
        }

        void create(UserProfileCreateForm form) {
            String id = form.username();
            var groups = execute("{ groups { id displayName } }", Map.of()).path("groups");
            int role = StreamSupport.stream(groups.spliterator(), false)
                    .filter(group -> group.path("displayName").asText().equalsIgnoreCase("ROLE_USER"))
                    .mapToInt(group -> group.path("id").asInt()).findFirst()
                    .orElseThrow(() -> new ProfileUsernameException("The ROLE_USER directory group is missing."));
            execute("mutation ($user: CreateUserInput!) { createUser(user: $user) { id } }",
                    Map.of("user", Map.of("id", id, "displayName", form.displayName(), "email", form.email())));
            try {
                changePassword(id, form.password());
                execute("mutation ($id: String!, $role: Int!) { addUserToGroup(userId: $id, groupId: $role) { ok } }",
                        Map.of("id", id, "role", role));
            } catch (RuntimeException exception) {
                try {
                    delete(id);
                } catch (ProfileUsernameException cleanupFailure) {
                    throw new ProfileUsernameException("Account setup failed. A directory administrator must check the incomplete account.");
                }
                throw new ProfileUsernameException("Account setup failed. The incomplete account was removed.");
            }
        }

        void update(String id, UserProfileForm form) {
            execute("mutation ($user: UpdateUserInput!) { updateUser(user: $user) { ok } }",
                    Map.of("user", Map.of("id", id, "displayName", form.displayName(), "email", form.email())));
        }

        void delete(String id) {
            execute("mutation ($id: String!) { deleteUser(userId: $id) { ok } }", Map.of("id", id));
        }

        private JsonNode execute(String query, Map<String, ?> variables) {
            try {
                var result = api.post().uri("/api/graphql").headers(headers -> headers.setBearerAuth(token))
                        .body(Map.of("query", query, "variables", variables)).retrieve().body(JsonNode.class);
                if (result == null || result.hasNonNull("errors") || !result.hasNonNull("data")) {
                    throw new ProfileUsernameException("The directory rejected the request. Check for an existing username or email.");
                }
                var data = result.path("data");
                for (JsonNode operation : data) {
                    if (operation.has("ok") && !operation.path("ok").asBoolean()) {
                        throw new ProfileUsernameException("The directory did not complete the update.");
                    }
                }
                return data;
            } catch (RestClientException exception) {
                throw new ProfileUsernameException("The directory management service is unavailable.");
            }
        }
    }
}
