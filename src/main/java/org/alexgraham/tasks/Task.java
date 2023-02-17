package org.alexgraham.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import org.alexgraham.users.User;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Cacheable
@Table(name = "task", schema = "public")
public class Task extends PanacheEntity {


    // DEV NOTE: Use enum for "Complete" state as opposed to a boolean. It leaves the door open for extension
    // to different states in the future. For instance, we could implement "Deleted" as a state which
    // tombstones the Task. This opens up a possible feature where users can resurrect Tasks that have
    // been deleted in the last N days. Similarly, we could implement an "Archived" state.
    /**
     * The possible states a Task may be in.
     */
    public enum State {
        /**
         * The Task is open, which implies to the user that they have something to do.
         */
        Open,

        /**
         * The Task has been marked as completed.
         */
        Complete;

        /**
         * Parses a State from the given string.
         * @param s     The string to parse. To add a little ease of use (API-friendliness),
         *              the string evaluation is case-insensitive. So "OpEn" will parse
         *              successfully to an Open enum value.
         * @throws      IllegalArgumentException if the given string does not parse to a State.
         * @return      The State matching the given string.
         */
        public static State parse(String s) {
            // BONUS: new Java switch-statement style!
            return switch(s.toLowerCase()) {
                case "open" -> Open;
                case "complete" -> Complete;
                default -> throw new IllegalArgumentException(s + " is not a valid state");
            };
        }
    }

    /**
     * Models different valid (non-exceptional) Results that may happen during Task operations.
     */
    // DEV NOTE: This seems like a possible candidate for a generic model that could be used
    // in other domains. However, wait for 2-3 more use cases to crop up, to identify the pattern.
    public sealed interface Result permits Result.Updated, Result.NotModified {
        record Updated(Task task) implements Task.Result {}
        record NotModified() implements Task.Result {}
    }

    @Column(length = 128)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="ownerid", nullable=false)
    private User owner;

    @Column(length = 2048, nullable = true)
    private String description;

    // DEV NOTE: Because we're using the STRING type, we could not change the names of the enum values
    // without a transformation of the existing data (if we had an established DB in beta/prod).
    // IMO, a String is fine for a simple prototype. For a real product, I'd invest a little effort
    // into an explicit mapper. For more, see: https://thorben-janssen.com/hibernate-enum-mappings/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state = State.Open;

    public Task() {}

    public Task(String title, User owner) {
        this.title = title;
        this.owner = owner;
    }

    /**
     * Modifies the Task's State to Complete.
     *
     * @return      This Task
     */
    public Task complete() {
        this.state = State.Complete;
        return this;
    }

    public Task reopen() {
        this.state = State.Open;
        return this;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Sets the Title attribute of the Task, unless the given title is null or an empty string.
     *
     * @param newTitle  The new, non-null title
     * @return          This instance of the Task.
     */
    public Task setTitle(String newTitle) {
        if (newTitle != null && !newTitle.isBlank()) {
            this.title = newTitle;
        }
        return this;
    }

    public String getDescription() {
        return description;
    }

    public State getState() {
        return this.state;
    }

    @JsonIgnore
    public boolean isComplete() {
        return State.Complete.equals(state);
    }

    @JsonIgnore
    public boolean isOpen() {
        return State.Open.equals(state);
    }

    /**
     * Sets the Description of the Task, unless it is null.
     *
     * @param newDescription    The new non-null description (if null, it will be skipped).
     *                          To "clear it" it can be set to a blank string.
     * @return                  This instance of the Task.
     */
    public Task setDescription(String newDescription) {
        if (newDescription != null) {
            this.description = newDescription;
        }
        return this;
    }
}
