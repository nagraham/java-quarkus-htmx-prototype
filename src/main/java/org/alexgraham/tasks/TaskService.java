package org.alexgraham.tasks;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.alexgraham.users.User;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TaskService {

    private static final List<Task.State> DEFAULT_STATES = List.of(Task.State.Open);

    @ReactiveTransactional
    public Uni<Task> completeTask(Long taskId) {
        return Task.<Task>findById(taskId)
                .onItem().ifNull().failWith(new TaskNotFoundException())
                .flatMap(task -> task.complete().persist());
    }

    /**
     * Get all Tasks associated with the given owner.
     * <p>
     * By default, only Open Tasks are included in the results. Other states can be viewed
     * with the
     * <p>
     * The Tasks have the following sorting rules:
     * <ol>
     *     <li>Open tasks that have been ranked by the user are ordered by that ranking</li>
     *     <li>Any Tasks not ranked by the user (e.g. new Tasks) come next</li>
     * </ol>
     *
     * The TaskRanking may not encompass the full set of tasks the User has created. Any
     * tasks not in the TaskRanking will be appended to the end.
     *
     * @param ownerId   The id of the {@link User} who owns the tasks
     * @param states    An optional list of {@link Task.State} to filter results. If null
     *                  or empty, it will default to returning Open tasks.
     * @return          The ranked set of the Tasks.
     */
    @ReactiveTransactional
    public Uni<List<Task>> queryByOwner(String ownerId, List<Task.State> states) {

        // set the default status to Task
        if (states == null || states.isEmpty()) {
            states = DEFAULT_STATES;
        }

        Uni<List<Task>> taskUni = Task.<Task>find(
                "ownerid = ?1 AND state in (?2)",
                Sort.by("id"),
                ownerId,
                states
        ).list();

        // Get the TaskRanking to help with sorting the Open Tasks
        Uni<TaskRanking> taskRankingUni = TaskRanking.<TaskRanking>find("ownerid = ?1", ownerId)
                .firstResult()
                .replaceIfNullWith(() -> new TaskRanking().setRankedTaskIds(new ArrayList<>()));

        // Join the two async results
        return Uni.combine().all().unis(taskUni, taskRankingUni)
                .combinedWith((tasks, taskRanking) -> {
                    Map<Long, Task> tasksById = tasks.stream().collect(Collectors.toMap(task -> task.id, task -> task));
                    Stream<Task> rankedTasks = taskRanking.getRankedTaskIds().stream().map(tasksById::get).filter(Objects::nonNull);
                    Set<Long> rankedTaskSet = new HashSet<>(taskRanking.getRankedTaskIds());
                    Stream<Task> unRankedTasks = tasks.stream().filter(task -> !rankedTaskSet.contains(task.id));
                    return Stream.concat(rankedTasks, unRankedTasks).toList();
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
