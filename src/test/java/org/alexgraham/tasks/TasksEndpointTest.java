package org.alexgraham.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.alexgraham.users.User;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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

    @Test
    void listTasksReturnsAllTasksOwnedByUser() {
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
    void listTasks_returnsEmptyIfUserDoesNotHaveAny() {
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

    @Test
    void updateTask_whenTaskExists_updatesSuccessfully() {
        User user = createUser("test-user-updating-tasks");
        Task task = createTask(user, "original-title");

        Response response = given()
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
                .post("/tasks/" + task.id)
                .then()
                .statusCode(200)
                .extract().response();
        Task updatedTask = response.getBody().as(Task.class);

        assertThat(updatedTask.getTitle(), is("updated-title"));
    }

    @Test
    void updateTask_whenTaskDoesntExist_return404() {
        User user = createUser("test-user-updating-tasks");

        Response response = given()
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
                .statusCode(404)
                .extract().response();
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



}
