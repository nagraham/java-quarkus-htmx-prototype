package org.alexgraham.tasks;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.alexgraham.users.User;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
public class TaskService {

    @ReactiveTransactional
    public Uni<Task> completeTask(Long taskId) {
        return Task.<Task>findById(taskId)
                .onItem().ifNull().failWith(new TaskNotFoundException())
                .flatMap(task -> task.complete().persist());
    }

    /**
     * Get all Tasks associated with the given owner, sorted by the ranking the customer
     * has applied.
     * <p>
     * By default, completed Tasks are not included in the results.
     * <p>
     * The TaskRanking may not encompass the full set of tasks the User has created. Any
     * tasks not in the TaskRanking will be appended to the end.
     *
     * @param ownerId   The id of the {@link User} who owns the tasks
     * @return          The ranked set of the Tasks.
     */
    @ReactiveTransactional
    public Uni<List<Task>> queryByOwner(String ownerId) {
        // Get the tasks for the owner (the base sort is by id for now)
        // TODO: sort by creation date
        Uni<List<Task>> taskUni = Task.<Task>find(
                "ownerid = ?1 AND state != ?2",
                Sort.by("id"),
                ownerId,
                Task.State.Complete
        ).list();

        // Get the TaskRanking
        Uni<TaskRanking> taskRankingUni = TaskRanking.<TaskRanking>find("ownerid = ?1", ownerId)
                .firstResult()
                .replaceIfNullWith(() -> new TaskRanking().setRankedTaskIds(new ArrayList<>()));

        // Join the two async results, and set the Tasks by order.
        return Uni.combine().all().unis(taskUni, taskRankingUni)
                .combinedWith((tasks, taskRanking) -> {
                    Map<Long, Task> tasksById = new HashMap<>();
                    Set<Long> rankedTaskSet = new HashSet<>(taskRanking.getRankedTaskIds());
                    tasks.forEach(task -> tasksById.put(task.id, task));
                    return Stream.concat(
                            taskRanking.getRankedTaskIds().stream().map(tasksById::get),
                            tasks.stream().filter(task -> !rankedTaskSet.contains(task.id))
                    ).toList();
                })
                .flatMap(tasks -> Uni.createFrom().item(tasks));
    }

    @ReactiveTransactional
    public Uni<Task> persist(String title, UUID ownerId) {
        // We first find the owner to verify they actually exist, before creating the task
        return User.<User>findById(ownerId).flatMap(user -> new Task(title, user).persist());
    }

    /**
     * This a new set of task rankings in the {@link TaskRanking} model associated with the
     * Owner of the tasks.
     * <p>
     * These objects are persisted lazily (only when tasks are re-ranked). If the User does
     * not already have a {@link TaskRanking}, one will be created.
     *
     * @param ownerId       The Owner of the tasks that are being re-ranked.
     * @param rankings      The ranked task ids.
     * @return              The updated TaskRanking object.
     */
    @ReactiveTransactional
    public Uni<TaskRanking> saveTaskRankings(UUID ownerId, List<Long> rankings) {
        return TaskRanking.<TaskRanking>find("ownerid = ?1", ownerId)
                .firstResult()
                .onItem().ifNull().switchTo(User.<User>findById(ownerId).map(TaskRanking::new))
                .flatMap(taskRanking -> taskRanking.setRankedTaskIds(rankings).persist());
    }

    /**
     * Updates mutable data attributes on the {@Link Task}.
     *
     * @param taskId            The id of the Task to update.
     * @param taskWithUpdates   A Task object which contains mutable attributes to modify. If any
     *                          attributes are null, they will be ignored.
     * @return
     */
    @ReactiveTransactional
    public Uni<Task> update(Long taskId, Task taskWithUpdates) {
        return Task.<Task>findById(taskId)
                .onItem().ifNull().failWith(new TaskNotFoundException())
                .flatMap(task -> task
                        .setTitle(taskWithUpdates.getTitle())
                        .setDescription(taskWithUpdates.getDescription())
                        .persist());
    }
}
