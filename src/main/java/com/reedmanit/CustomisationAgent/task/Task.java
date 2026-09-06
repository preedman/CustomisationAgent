package com.reedmanit.CustomisationAgent.task;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name")
    @JsonProperty("task_name")
    @JsonAlias({"taskName", "task_name"})
    private String taskName;

    @Column(name = "is_urgent")
    @JsonProperty("urgent")
    @JsonAlias({"urgent", "is_urgent", "isUrgent"})
    private boolean urgent = false;

    public Task() {
    }

    public Task(String taskName) {
        this.taskName = taskName;
        this.urgent = false;
    }

    public Task(String taskName, boolean urgent) {
        this.taskName = taskName;
        this.urgent = urgent;
    }

    public Task(Long id, String taskName) {
        this.id = id;
        this.taskName = taskName;
        this.urgent = false;
    }

    public Task(Long id, String taskName, boolean urgent) {
        this.id = id;
        this.taskName = taskName;
        this.urgent = urgent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }
}
