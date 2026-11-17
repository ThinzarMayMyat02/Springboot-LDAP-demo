package example.catalog.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {
    private final UserProfileRepository repository;

    UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public List<UserProfileView> list() {
        return repository.connect().list();
    }

    public UserProfileView get(String id) {
        return requireAccount(repository.connect(), id);
    }

    public void create(UserProfileCreateForm form) {
        repository.connect().create(form);
    }

    public void update(String id, UserProfileForm form) {
        var session = repository.connect();
        requireAccount(session, id);
        session.update(id, form);
    }

    public void delete(String id) {
        var session = repository.connect();
        requireAccount(session, id);
        session.delete(id);
    }

    private UserProfileView requireAccount(UserProfileRepository.Session session, String id) {
        return session.list().stream().filter(account -> account.username().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
