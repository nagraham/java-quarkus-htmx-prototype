package org.alexgraham.tasks;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import org.alexgraham.users.User;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TaskService {

    public Uni<List<Task>> queryByOwner(String ownerId) {
        return Task.find("ownerid = ?1", ownerId).list();
    }

    public Uni<Task> persist(String title, UUID ownerId) {
        return User.<User>findById(ownerId)
                .onItem().transform(user -> new Task(title, user))
                .flatMap(taskToPersist -> Panache.<Task>withTransaction(taskToPersist::persist));
    }

}
