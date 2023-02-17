package org.alexgraham.users;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import org.alexgraham.tasks.Task;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;
import java.util.UUID;

@Entity
@Cacheable
@Table(name = "user", schema = "public")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue( generator = "UUID" )
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Type(type="pg-uuid")
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy="owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    public User() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Task> taskList() {
        return tasks;
    }
}
