package com.salesnav360.controller;

import com.salesnav360.dto.PageResponse;
import com.salesnav360.dto.TaskRequest;
import com.salesnav360.dto.TaskResponse;
import com.salesnav360.dto.TaskStatusRequest;
import com.salesnav360.enums.TaskStatus;
import com.salesnav360.security.SalesNavUserDetails;
import com.salesnav360.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/salesnav/tasks")
public class TaskController {

    private static final List<String> SORT_FIELDS = List.of("title", "dueDate", "createdAt", "status", "priority");

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public PageResponse<TaskResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dueDate") String sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        if (!SORT_FIELDS.contains(sort)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported task sort field.");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(direction, sort));
        return PageResponse.from(taskService.search(query, status, assignedTo, pageable));
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return taskService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    public TaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return taskService.update(id, request, user.getUsername());
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return taskService.changeStatus(id, request.status(), user.getUsername());
    }
}

