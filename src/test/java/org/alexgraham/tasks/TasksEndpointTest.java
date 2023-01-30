package org.alexgraham.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
public class TasksEndpointTest {

    @Test
    void canCreateTasks() {
        given()
                .when()
                .body("""
                        {
                            "title": "a new task!"
                        }
                        """)
                .contentType(ContentType.JSON)
                .post("/tasks")
                .then()
                .statusCode(201)
                .body(
                        containsString("\"id\":"),
                        containsString("\"title\":\"a new task!\""));
    }

    @Test
    void canGetNewlyCreatedTask() {
        Response response = given()
                .when()
                .body("""
                        {
                            "title": "a new task to get!"
                        }
                        """)
                .contentType(ContentType.JSON)
                .post("/tasks")
                .then()
                .statusCode(201)
                .extract().response();

        Task createdTask = response.getBody().as(Task.class);

        given()
                .when()
                .get("/tasks/" + createdTask.id)
                .then()
                .statusCode(200)
                .body(
                        containsString("\"id\":" + createdTask.id),
                        containsString("\"title\":\"" + createdTask.getTitle() + "\"")
                );
    }


}
