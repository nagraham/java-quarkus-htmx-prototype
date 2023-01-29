package org.alexgraham.tasks;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
@Cacheable
public class Task extends PanacheEntity {

    @Column(length = 128)
    public String title;

}
