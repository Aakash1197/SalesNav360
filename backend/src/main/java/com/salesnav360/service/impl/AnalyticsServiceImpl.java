package com.salesnav360.service.impl;

import com.salesnav360.dto.AnalyticsSummaryResponse;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.LeadStage;
import com.salesnav360.enums.PolicyStatus;
import com.salesnav360.enums.QuotationStatus;
import com.salesnav360.enums.TaskStatus;
import com.salesnav360.repository.ClientRepository;
import com.salesnav360.repository.LeadRepository;
import com.salesnav360.repository.PolicyRepository;
import com.salesnav360.repository.QuotationRepository;
import com.salesnav360.repository.TaskRepository;
import com.salesnav360.service.AnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final List<LeadStage> OPEN_STAGES = List.of(
            LeadStage.NEW, LeadStage.CONTACTED, LeadStage.QUALIFIED,
            LeadStage.PROPOSAL, LeadStage.NEGOTIATION);
    private static final Set<TaskStatus> OPEN_TASK_STATUSES = Set.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS);

    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final PolicyRepository policyRepository;
    private final QuotationRepository quotationRepository;
    private final TaskRepository taskRepository;

    public AnalyticsServiceImpl(
            ClientRepository clientRepository,
            LeadRepository leadRepository,
            PolicyRepository policyRepository,
            QuotationRepository quotationRepository,
            TaskRepository taskRepository) {
        this.clientRepository = clientRepository;
        this.leadRepository = leadRepository;
        this.policyRepository = policyRepository;
        this.quotationRepository = quotationRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary() {
        long totalLeads = leadRepository.count();
        long eligibleLeads = totalLeads - leadRepository.countByStage(LeadStage.UNQUALIFIED);
        long wonDeals = leadRepository.countByStage(LeadStage.WON);
        long openLeads = leadRepository.countByStageIn(OPEN_STAGES);
        long completedTasks = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long totalQuotations = quotationRepository.count();
        long acceptedQuotations = quotationRepository.countByStatus(QuotationStatus.ACCEPTED);

        EnumMap<LeadStage, Long> stageCounts = new EnumMap<>(LeadStage.class);
        for (LeadStage stage : LeadStage.values()) {
            stageCounts.put(stage, leadRepository.countByStage(stage));
        }
        Map<String, Long> stages = stageCounts.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().name(), Map.Entry::getValue));

        long activeTasks = taskRepository.countByStatusIn(OPEN_TASK_STATUSES);
        LocalDate today = LocalDate.now();
        return new AnalyticsSummaryResponse(
                clientRepository.countByStatusNot(ClientStatus.ARCHIVED),
                totalLeads,
                openLeads,
                wonDeals,
                percentage(wonDeals, eligibleLeads),
                leadRepository.sumExpectedValueByStages(OPEN_STAGES),
                leadRepository.sumExpectedValueByStage(LeadStage.WON),
                activeTasks,
                completedTasks,
                percentage(completedTasks, completedTasks + activeTasks),
                policyRepository.countByStatus(PolicyStatus.ACTIVE),
                policyRepository.countRenewingBetween(PolicyStatus.ACTIVE, today, today.plusDays(30)),
                acceptedQuotations,
                totalQuotations,
                percentage(acceptedQuotations, totalQuotations),
                quotationRepository.sumGrandTotalByStatus(QuotationStatus.ACCEPTED),
                stages);
    }

    private static double percentage(long numerator, long denominator) {
        return denominator == 0 ? 0 : BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}
