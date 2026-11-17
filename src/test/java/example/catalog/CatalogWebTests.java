package example.catalog;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import example.catalog.user.UserProfileService;
import example.catalog.user.UserProfileView;
import example.catalog.user.UserRole;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogWebTests {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserProfileService users;

    @Test
    void enforcesRolesAndCsrf() throws Exception {
        mvc.perform(get("/products")).andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(get("/login")).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/admin/users").with(user("demo-user").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/users").with(user("demo-user").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/products").with(user("demo-admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void bothRolesCanManageProducts() throws Exception {
        var regular = user("demo-user").roles("USER");
        var admin = user("demo-admin").roles("ADMIN");
        mvc.perform(get("/products/new").with(regular)).andExpect(status().isOk());
        mvc.perform(post("/products").with(regular).with(csrf())
                        .param("name", "").param("price", "-1"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "price"));
        mvc.perform(post("/products").with(regular).with(csrf())
                        .param("name", "Desk lamp").param("price", "25.50"))
                .andExpect(redirectedUrl("/products"));
        mvc.perform(get("/products").with(admin))
                .andExpect(content().string(containsString("Desk lamp")));
        mvc.perform(get("/products/1").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/products/1/edit").with(admin)).andExpect(status().isOk());
        mvc.perform(post("/products/1").with(admin).with(csrf())
                        .param("name", "Table lamp").param("price", "22.00"))
                .andExpect(redirectedUrl("/products"));
        mvc.perform(get("/products/1/delete").with(regular)).andExpect(status().isOk());
        mvc.perform(post("/products/1/delete").with(regular).with(csrf()))
                .andExpect(redirectedUrl("/products"));
        mvc.perform(get("/products/1").with(regular)).andExpect(status().isNotFound());
    }

    @Test
    void adminCanSubmitDirectoryAccountForms() throws Exception {
        var account = new UserProfileView("new-user", "New User", "new@example.test", UserRole.ROLE_ADMIN);
        when(users.list()).thenReturn(List.of(account));
        when(users.get("new-user")).thenReturn(account);
        var admin = user("demo-admin").roles("ADMIN");
        mvc.perform(get("/admin/users/new").with(admin))
                .andExpect(content().string(containsString("autocomplete=\"new-password\"")));
        mvc.perform(post("/admin/users").with(admin).with(csrf())
                        .param("username", "new-user").param("displayName", "New User")
                        .param("email", "new@example.test").param("password", "short"))
                .andExpect(model().attributeHasFieldErrors("form", "password"));
        mvc.perform(post("/admin/users").with(admin).with(csrf())
                        .param("username", "new-user").param("displayName", "New User")
                        .param("email", "new@example.test").param("password", "local-test-password").param("role", "ROLE_ADMIN"))
                .andExpect(redirectedUrl("/admin/users"));
        verify(users).create(new example.catalog.user.UserProfileCreateForm(
                "new-user", "New User", "new@example.test", "local-test-password", UserRole.ROLE_ADMIN));
        mvc.perform(get("/admin/users").with(admin))
                .andExpect(content().string(containsString("new-user")));
        mvc.perform(get("/admin/users/new-user/edit").with(admin))
                .andExpect(content().string(containsString("value=\"ROLE_ADMIN\" selected=\"selected\"")));
        mvc.perform(post("/admin/users").with(admin).with(csrf())
                        .param("username", "new-user").param("displayName", "New User")
                        .param("email", "new@example.test").param("password", "local-test-password")
                        .param("role", "SUPERUSER"))
                .andExpect(model().attributeHasFieldErrors("form", "role"));
        mvc.perform(post("/admin/users/new-user").with(admin).with(csrf())
                        .param("displayName", "New User").param("email", "new@example.test"))
                .andExpect(model().attributeHasFieldErrors("form", "role"));
        mvc.perform(post("/admin/users/new-user").with(admin).with(csrf())
                        .param("username", "other").param("displayName", "Updated Name")
                        .param("email", "new@example.test").param("role", "ROLE_USER"))
                .andExpect(redirectedUrl("/admin/users"));
        verify(users).update("new-user", new example.catalog.user.UserProfileForm("Updated Name", "new@example.test", UserRole.ROLE_USER));
        mvc.perform(get("/admin/users/new-user/delete").with(admin)).andExpect(status().isOk());
        mvc.perform(post("/admin/users/new-user/delete").with(admin).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"));
        verify(users).delete("new-user");
    }
}
