package com.salesnav360.service.impl;

import com.salesnav360.dto.TaskRequest;
import com.salesnav360.dto.TaskResponse;
import com.salesnav360.entity.TaskRecord;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.TaskRelatedEntityType;
import com.salesnav360.enums.TaskStatus;
import com.salesnav360.repository.ClientRepository;
import com.salesnav360.repository.LeadRepository;
import com.salesnav360.repository.TaskRepository;
import com.salesnav360.repository.UserAccountRepository;
import com.salesnav360.service.AuditEventService;
import com.salesnav360.service.TaskService;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.OPEN, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.CANCELLED),
            TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.OPEN, TaskStatus.COMPLETED, TaskStatus.CANCELLED),
            TaskStatus.COMPLETED, EnumSet.of(TaskStatus.OPEN),
            TaskStatus.CANCELLED, EnumSet.of(TaskStatus.OPEN));

    private final TaskRepository taskRepository;
    private final UserAccountRepository userAccountRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final AuditEventService auditEventService;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            UserAccountRepository userAccountRepository,
            ClientRepository clientRepository,
            LeadRepository leadRepository,
            AuditEventService auditEventService) {
        this.taskRepository = taskRepository;
        this.userAccountRepository = userAccountRepository;
        this.clientRepository = clientRepository;
        this.leadRepository = leadRepository;
        this.auditEventService = auditEventService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> search(String query, TaskStatus status, String assignedTo, Pageable pageable) {
        return taskRepository.search(clean(query), status, clean(assignedTo), pageable).map(TaskResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(find(id));
    }

    @Override
    @Transactional
    public TaskResponse create(TaskRequest request, String actor) {
        validateRequest(request);
        TaskRecord task = taskRepository.save(new TaskRecord(request, actor));
        auditEventService.record(actor, "TASK_CREATED", "TASK", task.getId(), "Created task " + task.getId());
        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public TaskResponse update(Long id, TaskRequest request, String actor) {
        validateRequest(request);
        TaskRecord task = find(id);
        task.update(request);
        auditEventService.record(actor, "TASK_UPDATED", "TASK", id, "Updated task " + id);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public TaskResponse changeStatus(Long id, TaskStatus nextStatus, String actor) {
        TaskRecord task = find(id);
        if (task.getStatus() != nextStatus && !ALLOWED_TRANSITIONS.get(task.getStatus()).contains(nextStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requested task status transition is not allowed.");
        }
        TaskStatus previousStatus = task.getStatus();
        task.changeStatus(nextStatus);
        auditEventService.record(actor, "TASK_STATUS_CHANGED", "TASK", id,
                "Changed task " + id + " from " + previousStatus + " to " + nextStatus);
        return TaskResponse.from(task);
    }

    private void validateRequest(TaskRequest request) {
        validateAssignedTo(request.assignedTo());
        boolean hasType = request.relatedEntityType() != null;
        boolean hasId = request.relatedEntityId() != null;
        if (hasType != hasId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A related record type and identifier must be provided together.");
        }
        if (request.relatedEntityType() == TaskRelatedEntityType.CLIENT
                && clientRepository.findByIdAndStatusNot(request.relatedEntityId(), ClientStatus.ARCHIVED).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The linked active client was not found.");
        }
        if (request.relatedEntityType() == TaskRelatedEntityType.LEAD
                && !leadRepository.existsById(request.relatedEntityId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The linked lead was not found.");
        }
    }

    private void validateAssignedTo(String username) {
        if (username != null && !username.isBlank()
                && !userAccountRepository.existsByUsernameAndEnabledTrue(username.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assigned representative must be an active SalesNav360 user.");
        }
    }

    private TaskRecord find(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task was not found."));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

