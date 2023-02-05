package org.alexgraham.tasks;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import org.alexgraham.users.User;

@Entity
@Cacheable
@Table(name = "task", schema = "public")
public class Task extends PanacheEntity {

    @Column(length = 128)
    private String title;

    @ManyToOne
    @JoinColumn(name="ownerid", nullable=false)
    private User owner;

    public Task() {}

    public Task(String title, User owner) {
        this.title = title;
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public Task setTitle(String newTitle) {
        this.title = newTitle;
        return this;
    }

    public User getOwner() {
        return owner;
    }
}
