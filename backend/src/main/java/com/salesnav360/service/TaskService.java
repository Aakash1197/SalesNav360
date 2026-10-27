package com.salesnav360.service;

import com.salesnav360.dto.TaskRequest;
import com.salesnav360.dto.TaskResponse;
import com.salesnav360.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    Page<TaskResponse> search(String query, TaskStatus status, String assignedTo, Pageable pageable);

    TaskResponse get(Long id);

    TaskResponse create(TaskRequest request, String actor);

    TaskResponse update(Long id, TaskRequest request, String actor);

    TaskResponse changeStatus(Long id, TaskStatus status, String actor);
}

