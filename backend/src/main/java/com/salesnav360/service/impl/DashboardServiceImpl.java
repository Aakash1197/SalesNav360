package com.salesnav360.service.impl;

import com.salesnav360.dto.ActivityResponse;
import com.salesnav360.dto.DashboardResponse;
import com.salesnav360.dto.TaskResponse;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.ClientType;
import com.salesnav360.enums.LeadStage;
import com.salesnav360.enums.TaskStatus;
import com.salesnav360.repository.AuditEventRepository;
import com.salesnav360.repository.ClientRepository;
import com.salesnav360.repository.LeadRepository;
import com.salesnav360.repository.TaskRepository;
import com.salesnav360.service.DashboardService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Set<LeadStage> OPEN_STAGES = Set.of(
            LeadStage.NEW, LeadStage.CONTACTED, LeadStage.QUALIFIED,
            LeadStage.PROPOSAL, LeadStage.NEGOTIATION);
    private static final Set<LeadStage> FOLLOW_UP_EXCLUDED_STAGES = Set.of(
            LeadStage.WON, LeadStage.LOST, LeadStage.UNQUALIFIED);
    private static final Set<TaskStatus> ACTIVE_TASK_STATUSES = Set.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS);

    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final AuditEventRepository auditEventRepository;
    private final TaskRepository taskRepository;

    public DashboardServiceImpl(
            ClientRepository clientRepository,
            LeadRepository leadRepository,
            AuditEventRepository auditEventRepository,
            TaskRepository taskRepository) {
        this.clientRepository = clientRepository;
        this.leadRepository = leadRepository;
        this.auditEventRepository = auditEventRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getOverview() {
        long clients = clientRepository.countByTypeAndStatusNot(ClientType.CLIENT, ClientStatus.ARCHIVED);
        long investors = clientRepository.countByTypeAndStatusNot(ClientType.INVESTOR, ClientStatus.ARCHIVED);
        long totalLeads = leadRepository.count();
        long openLeads = leadRepository.countByStageIn(OPEN_STAGES);
        long wonDeals = leadRepository.countByStage(LeadStage.WON);
        long eligibleLeads = totalLeads - leadRepository.countByStage(LeadStage.UNQUALIFIED);
        BigDecimal closedWonRevenue = leadRepository.sumExpectedValueByStage(LeadStage.WON);
        BigDecimal activePipelineValue = leadRepository.sumExpectedValueByStages(OPEN_STAGES);
        BigDecimal averageDealSize = wonDeals == 0
                ? BigDecimal.ZERO
                : closedWonRevenue.divide(BigDecimal.valueOf(wonDeals), 2, RoundingMode.HALF_UP);
        double conversionRate = eligibleLeads == 0
                ? 0
                : BigDecimal.valueOf(wonDeals)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(eligibleLeads), 1, RoundingMode.HALF_UP)
                        .doubleValue();
        long followUpsDueToday = leadRepository.countDueOn(LocalDate.now(), FOLLOW_UP_EXCLUDED_STAGES);
        long activeTasks = taskRepository.countByStatusIn(ACTIVE_TASK_STATUSES);
        long completedTasks = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long overdueTasks = taskRepository.countByDueDateBeforeAndStatusIn(LocalDate.now(), ACTIVE_TASK_STATUSES);
        List<TaskResponse> upcomingTasks = taskRepository
                .findTop5ByStatusInAndDueDateGreaterThanEqualOrderByDueDateAsc(ACTIVE_TASK_STATUSES, LocalDate.now())
                .stream()
                .map(TaskResponse::from)
                .toList();

        List<ActivityResponse> recentActivity = auditEventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(ActivityResponse::from)
                .toList();
        Map<String, Long> leadStages = new LinkedHashMap<>();
        for (LeadStage stage : LeadStage.values()) {
            leadStages.put(stage.name(), leadRepository.countByStage(stage));
        }
        return new DashboardResponse(
                clients, investors, totalLeads, openLeads, wonDeals, followUpsDueToday,
                conversionRate, closedWonRevenue, averageDealSize, activePipelineValue,
                activeTasks, completedTasks, overdueTasks, upcomingTasks, leadStages, recentActivity);
    }
}

