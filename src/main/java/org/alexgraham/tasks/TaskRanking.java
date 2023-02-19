package org.alexgraham.tasks;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import org.alexgraham.users.User;

import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;

/**
 * This record persists the rankings of an Owner's Tasks, which provides the implementation for
 * manual sorting of Tasks.
 */
@Entity
@Cacheable
@Table(name = "task_ranks", schema = "public")
public class TaskRanking extends PanacheEntity {

    @OneToOne
    @JoinColumn(name="ownerid", nullable=false)
    private User owner;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="ranked_task_id_table", joinColumns=@JoinColumn(name="id"))
    @Column(name="ranked_task_ids")
    private List<Long> rankedTaskIds;

    public TaskRanking() {}

    public TaskRanking(User owner) {
        this.owner = owner;
    }

    public List<Long> getRankedTaskIds() {
        return rankedTaskIds;
    }

    public TaskRanking setRankedTaskIds(List<Long> rankedTaskIds) {
        this.rankedTaskIds = rankedTaskIds;
        return this;
    }
}
