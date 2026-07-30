package com.fanhua.jobtrack.module.application.domain;

import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 投递状态机的唯一规则入口。 */
@Component
public class ApplicationStateMachine {

    private static final Set<ApplicationStatus> REASON_REQUIRED = EnumSet.of(
            ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED,
            ApplicationStatus.WITHDRAWN, ApplicationStatus.CLOSED);

    private final Map<ApplicationStatus, Set<ApplicationStatus>> transitions = new EnumMap<>(ApplicationStatus.class);

    public ApplicationStateMachine() {
        transitions.put(ApplicationStatus.SAVED, EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.CLOSED));
        transitions.put(ApplicationStatus.APPLIED, EnumSet.of(
                ApplicationStatus.ASSESSMENT, ApplicationStatus.INTERVIEWING,
                ApplicationStatus.OFFERED, ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN, ApplicationStatus.CLOSED));
        transitions.put(ApplicationStatus.ASSESSMENT, EnumSet.of(
                ApplicationStatus.INTERVIEWING, ApplicationStatus.OFFERED,
                ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN, ApplicationStatus.CLOSED));
        transitions.put(ApplicationStatus.INTERVIEWING, EnumSet.of(
                ApplicationStatus.OFFERED, ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN, ApplicationStatus.CLOSED));
        transitions.put(ApplicationStatus.OFFERED, EnumSet.of(
                ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN));
        transitions.put(ApplicationStatus.ACCEPTED, EnumSet.noneOf(ApplicationStatus.class));
        transitions.put(ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class));
        transitions.put(ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class));
        transitions.put(ApplicationStatus.CLOSED, EnumSet.noneOf(ApplicationStatus.class));
    }

    public ApplicationTransitionRule requireAllowed(ApplicationStatus from, ApplicationStatus to, String reason) {
        ApplicationTransitionRule rule = rule(from, to);
        if (rule == null) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_STATUS_TRANSITION.getCode(),
                    "不允许从 " + from + " 流转到 " + to);
        }
        if (rule.requiresReason() && (reason == null || reason.isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "该状态流转必须填写原因");
        }
        return rule;
    }

    public List<ApplicationStatus> allowedTargets(ApplicationStatus from) {
        return transitions.getOrDefault(from, Set.of()).stream().sorted().toList();
    }

    public ApplicationTransitionRule rule(ApplicationStatus from, ApplicationStatus to) {
        if (from == null || to == null || !transitions.getOrDefault(from, Set.of()).contains(to)) {
            return null;
        }
        return new ApplicationTransitionRule(
                from, to, REASON_REQUIRED.contains(to),
                false,
                to == ApplicationStatus.INTERVIEWING || to == ApplicationStatus.OFFERED
                        || to == ApplicationStatus.ACCEPTED,
                to.isTerminal(), true);
    }

    public Map<ApplicationStatus, Set<ApplicationStatus>> transitionTable() {
        return Map.copyOf(transitions);
    }
}
