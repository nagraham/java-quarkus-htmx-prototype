package org.alexgraham.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.alexgraham.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class TasksEndpointTest {

    @Test
    void canCreateTasks() {
        User user = createUser("create task user");

        given()
                .when()
                .body("""
                        {
                            "title": "a new task!"
                        }
                        """)
                .contentType(ContentType.JSON)
                .header(new Header("X-User-Id", user.getId().toString()))
                .post("/tasks")
                .then()
                .statusCode(201)
                .body(
                        containsString("\"id\":"),
                        containsString("\"title\":\"a new task!\""));
    }

    @Test
    void canGetNewlyCreatedTask() {
        User user = createUser("create-and-get-task-user");

        Response response = given()
                .when()
                .body("""
                        {
                            "title": "a new task to get!"
                        }
                        """)
                .contentType(ContentType.JSON)
                .header(new Header("X-User-Id", user.getId().toString()))
                .post("/tasks")
                .then()
                .statusCode(201)
                .extract().response();

        Task createdTask = response.getBody().as(Task.class);

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/tasks/" + createdTask.id)
                .then()
                .statusCode(200)
                .body(
                        containsString("\"id\":" + createdTask.id),
                        containsString("\"title\":\"" + createdTask.getTitle() + "\"")
                );
    }

    @Nested
    @DisplayName("List Tasks")
    class ListTasks {

        @Test
        void byDefault_returnsAllActiveTasksOwnedByUser() {
            User user = createUser("test-list-user");

            createTask(user, "task-1");
            createTask(user, "task-2");
            createTask(user, "task-3");

            Response response = given()
                    .when()
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .get("/tasks")
                    .then()
                    .statusCode(200)
                    .extract().response();
            assertThat(response.jsonPath().getList("title"), contains("task-1", "task-2", "task-3"));
        }

        @Test
        void byDefault_doesNotReturnCompletedTasks() {
            User user = createUser("test-list-user");
            Task task1 = createTask(user, "task-1");
            Task task2 = createTask(user, "task-2");
            Task task3 = createTask(user, "task-3");
            Task task4 = createTask(user, "task-4");

            completeTask(user, task2.id);
            completeTask(user, task4.id);
            List<Task> tasks = listTasksByUser(user);

            assertThat(tasks.stream().map(Task::getTitle).collect(Collectors.toList()), contains("task-1", "task-3"));
        }

        @Test
        void filterByState_onlyComplete_onlyReturnsCompletedTasks() {
            User user = createUser("test-list-user");
            Task task1 = createTask(user, "task-1");
            Task task2 = createTask(user, "task-2");
            Task task3 = createTask(user, "task-3");
            Task task4 = createTask(user, "task-4");

            completeTask(user, task2.id);
            completeTask(user, task4.id);
            List<Task> tasks = listTasksByUser(user, List.of("state=complete"));

            assertThat(tasks, hasSize(2));
            assertThat(tasks.stream().map(Task::getTitle).collect(Collectors.toList()), contains("task-2", "task-4"));
        }

        @Test
        void filterByMultipleState_OpenAndComplete_returnAllTasks() {
            User user = createUser("test-list-user");
            Task task1 = createTask(user, "task-1");
            Task task2 = createTask(user, "task-2");
            Task task3 = createTask(user, "task-3");
            Task task4 = createTask(user, "task-4");

            completeTask(user, task2.id);
            completeTask(user, task3.id);
            List<Task> tasks = listTasksByUser(user, List.of("state=open", "state=complete"));

            assertThat(tasks, hasSize(4));
            assertThat(tasks.stream().map(Task::getTitle).collect(Collectors.toList()),
                    contains(task1.getTitle(), task2.getTitle(), task3.getTitle(), task4.getTitle()));
        }

        @Test
        void filterWithNonExistantState_return400() {
            User user = createUser("test-list-user");
            Task task1 = createTask(user, "task-1");
            Task task2 = createTask(user, "task-2");
            Task task3 = createTask(user, "task-3");
            Task task4 = createTask(user, "task-4");

            completeTask(user, task2.id);
            completeTask(user, task4.id);
            given()
                    .when()
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .get("/tasks?state=foobar")
                    .then()
                    .statusCode(400);
        }

        @Test
        void returnsEmptyIfUserDoesNotHaveAny() {
            User user = createUser("test-user-without-tasks");

            Response response = given()
                    .when()
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .get("/tasks")
                    .then()
                    .statusCode(200)
                    .extract().response();
            assertThat(response.jsonPath().getList("title"), is(empty()));
        }
    }

    @Nested
    @DisplayName("Updating Task")
    class UpdateTask {

        @Test
        void whenUpdatingOnlyTheTaskTitle_updatesSuccessfully() {
            User user = createUser("test-user-updating-tasks");
            Task task = createTask(user, "original-title");

            Task updatedTask = updateTask(user, task.id, String.format(
                    """
                    {
                        "title": "%s"
                    }
                    """, "updated-title"));

            assertThat(updatedTask.getTitle(), is("updated-title"));
        }

        @Test
        void whenUpdatingOnlyTheTaskTitle_doesNotClearDescription() {
            User user = createUser("test-user-updating-tasks");
            Task task = createTask(user, "original-title");
            updateTask(user, task.id, String.format(
                    """
                    {
                        "description": "%s"
                    }
                    """, "a-description"));

            Task updatedTask = updateTask(user, task.id, String.format(
                    """
                    {
                        "title": "%s"
                    }
                    """, "updated-title"));

            assertThat(updatedTask.getTitle(), is("updated-title"));
            assertThat(updatedTask.getDescription(), is("a-description"));
        }

        @Test
        void whenUpdatingOnlyTaskDescription_updatesSuccessfully() {
            User user = createUser("test-user-updating-desc");
            Task task = createTask(user, "original-title");

            Task updatedTask = updateTask(user, task.id, String.format(
                    """
                    {
                        "description": "%s"
                    }
                    """, "updated-description"));

            assertThat(updatedTask.getDescription(), is("updated-description"));
            // assert the title doesn't change
            assertThat(updatedTask.getTitle(), is(task.getTitle()));
        }

        @Test
        void whenSettingDescriptionToBlankString_updatesSuccessfully() {
            User user = createUser("test-user-updating-desc");
            Task task = createTask(user, "original-title");

            updateTask(user, task.id, String.format(
                    """
                    {
                        "description": "%s"
                    }
                    """, "updated-description"));
            Task updatedTask = updateTask(user, task.id, String.format(
                    """
                    {
                        "description": "%s"
                    }
                    """, ""));

            assertThat(updatedTask.getDescription(), is(blankString()));
            // assert the title doesn't change
            assertThat(updatedTask.getTitle(), is(task.getTitle()));
        }

        @Test
        void whenTaskDoesntExist_return404() {
            User user = createUser("test-user-updating-tasks");

            given()
                    .when()
                    .body(String.format(
                            """
                            {
                                "title": "%s"
                            }
                            """,
                            "updated-title"))
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .contentType(ContentType.JSON)
                    .post("/tasks/" + Long.toString(123))
                    .then()
                    .statusCode(404);
        }

    }

    @Nested
    @DisplayName("Reranking Tasks")
    class RerankingTasks {

        @Test
        void rerankingSomeTasks() {
            User user = createUser("test-reranking-user");
            Task task1 = createTask(user, "task-1");
            Task task2 = createTask(user, "task-2");
            Task task3 = createTask(user, "task-3");
            Task task4 = createTask(user, "task-4");

            given()
                    .when()
                    .body(String.format("""
                            {
                                "rankings": [%d, %d, %d, %d]
                            }
                            """, task3.id, task1.id, task2.id, task4.id))
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .post("/tasks/rerank")
                    .then()
                    .statusCode(200);

            List<Task> reRankedTasks = listTasksByUser(user);
            assertThat(
                    reRankedTasks.stream().map(Task::getTitle).collect(Collectors.toList()),
                    containsInRelativeOrder("task-3", "task-1", "task-2", "task-4"));
        }

        @Test
        void multipleReRankings_respectsTheLastRank() {
            User user = createUser("test-reranking-user");
            Task task1 = createTask(user, "task-1");
            Task task2 = createTask(user, "task-2");
            Task task3 = createTask(user, "task-3");
            Task task4 = createTask(user, "task-4");

            given()
                    .when()
                    .body(String.format("""
                            {
                                "rankings": [%d, %d, %d, %d]
                            }
                            """, task3.id, task1.id, task2.id, task4.id))
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .post("/tasks/rerank")
                    .then()
                    .statusCode(200);

            given()
                    .when()
                    .body(String.format("""
                            {
                                "rankings": [%d, %d, %d, %d]
                            }
                            """, task4.id, task1.id, task2.id, task3.id))
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .post("/tasks/rerank")
                    .then()
                    .statusCode(200);

            List<Task> reRankedTasks = listTasksByUser(user);
            assertThat(
                    reRankedTasks.stream().map(Task::getTitle).collect(Collectors.toList()),
                    containsInRelativeOrder("task-4", "task-1", "task-2", "task-3"));
        }
    }

    @Nested
    @DisplayName("Completing Tasks")
    class CompletingTasks {

        @Test
        void whenTaskExists_isStillOpen_isReturnedAsComplete() {
            User user = createUser("test-completion-user");
            Task task = createTask(user, "task-to-complete");

            Task completedTask = given()
                    .when()
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .post("/tasks/" + task.id + "/complete")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response()
                    .getBody()
                    .as(Task.class);

            assertThat(task.id, is(completedTask.id));
            assertThat(completedTask.getState(), is(Task.State.Complete));
        }

        @Test
        void whenTaskExists_alreadyComplete_isReturnedAsComplete() {
            User user = createUser("test-completion-user");
            Task task = createTask(user, "task-to-complete");

            // Complete once
            given()
                    .when()
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .post("/tasks/" + task.id + "/complete")
                    .then()
                    .statusCode(200);

            // Complete it again
            Task completedTask = given()
                    .when()
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .post("/tasks/" + task.id + "/complete")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response()
                    .getBody()
                    .as(Task.class);

            assertThat(completedTask.getState(), is(Task.State.Complete));
        }

        @Test
        void whenTaskDoesntExist_return404() {
            User user = createUser("test-completion-user");

            given()
                    .when()
                    .contentType(ContentType.JSON)
                    .header(new Header("X-User-Id", user.getId().toString()))
                    .post("/tasks/" + 123 + "/complete")
                    .then()
                    .statusCode(404);
        }
    }

    /* ********************************************************
     *   HELPER METHODS
     * ******************************************************** */

    User createUser(String name) {
        Response response = given()
                .when()
                .body(String.format(
                        """
                        {
                            "name": "%s"
                        }
                        """, name))
                .contentType(ContentType.JSON)
                .post("/users")
                .then()
                .statusCode(201)
                .extract().response();

        return response.getBody().as(User.class);
    }

    Task completeTask(User user, Long taskId) {
        return given()
                .when()
                .contentType(ContentType.JSON)
                .header(new Header("X-User-Id", user.getId().toString()))
                .post("/tasks/" + taskId + "/complete")
                .then()
                .statusCode(200)
                .extract()
                .response()
                .getBody()
                .as(Task.class);
    }

    Task createTask(User user, String title) {
        Response response = given()
                .when()
                .body(String.format(
                        """
                        {
                            "title": "%s"
                        }
                        """,
                        title))
                .contentType(ContentType.JSON)
                .header(new Header("X-User-Id", user.getId().toString()))
                .post("/tasks")
                .then()
                .statusCode(201)
                .extract()
                .response();
        return response.getBody().as(Task.class);
    }

    List<Task> listTasksByUser(User user) {
        return listTasksByUser(user, List.of());
    }

    List<Task> listTasksByUser(User user, List<String> queryParams) {

        String queryPath = "";
        if (queryParams != null && !queryParams.isEmpty()) {
            queryPath = "?" + String.join("&", queryParams);
        }

        return given()
                .when()
                .contentType(ContentType.JSON)
                .header(new Header("X-User-Id", user.getId().toString()))
                .get( "/tasks" + queryPath)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", Task.class);
    }

    Task updateTask(User user, Long taskId, String body) {
        return given()
                .when()
                .body(body)
                .header(new Header("X-User-Id", user.getId().toString()))
                .contentType(ContentType.JSON)
                .post("/tasks/" + taskId)
                .then()
                .statusCode(200)
                .extract()
                .response()
                .getBody().as(Task.class);
    }

}
