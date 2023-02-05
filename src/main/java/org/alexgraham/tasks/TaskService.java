package org.alexgraham.tasks;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.alexgraham.users.User;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TaskService {

    public Uni<List<Task>> queryByOwner(String ownerId) {
        // todo: sort by rank/order instead
        return Task.find("ownerid = ?1", Sort.by("id"), ownerId).list();
    }

    @ReactiveTransactional
    public Uni<Task> persist(String title, UUID ownerId) {
        // We first find the owner to verify they actually exist, before creating the task
        return User.<User>findById(ownerId).flatMap(user -> new Task(title, user).persist());
    }

    @ReactiveTransactional
    public Uni<Task> update(Long taskId, Task taskWithUpdates) {
        return Task.<Task>findById(taskId)
                .onItem().ifNull().failWith(new TaskNotFoundException())
                .flatMap(task -> task.setTitle(taskWithUpdates.getTitle()).persist());
    }
}
