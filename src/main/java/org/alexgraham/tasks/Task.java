package org.alexgraham.tasks;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import org.alexgraham.users.User;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Cacheable
@Table(name = "task", schema = "public")
public class Task extends PanacheEntity {

    @Column(length = 128)
    private String title;

    @ManyToOne
    @JoinColumn(name="ownerid", nullable=false)
    private User owner;

    @Column(length = 2048, nullable = true)
    private String description;

    public Task() {}

    public Task(String title, User owner) {
        this.title = title;
        this.owner = owner;
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

    public User getOwner() {
        return owner;
    }

    public String getDescription() {
        return description;
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
