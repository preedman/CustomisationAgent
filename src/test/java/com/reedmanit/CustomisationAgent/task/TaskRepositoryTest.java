package com.reedmanit.CustomisationAgent.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void shouldSaveAndFindTask() {
        Task task = new Task("Buy server space", true);
        Task savedTask = taskRepository.save(task);

        assertThat(savedTask.getId()).isNotNull();
        assertThat(savedTask.getTaskName()).isEqualTo("Buy server space");
        assertThat(savedTask.isUrgent()).isTrue();

        Optional<Task> found = taskRepository.findById(savedTask.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTaskName()).isEqualTo("Buy server space");
        assertThat(found.get().isUrgent()).isTrue();
    }
}
