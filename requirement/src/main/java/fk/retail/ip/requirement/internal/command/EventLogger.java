package fk.retail.ip.requirement.internal.command;

import com.google.inject.Inject;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.internal.entities.RequirementEventLog;
import fk.retail.ip.requirement.internal.enums.EventType;
import fk.retail.ip.requirement.internal.repository.RequirementEventLogRepository;
import fk.retail.ip.requirement.model.RequirementChangeMap;
import fk.retail.ip.requirement.model.RequirementChangeRequest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by agarwal.vaibhav on 19/04/17.
 */
public class EventLogger {
    private final RequirementEventLogRepository requirementEventLogRepository;

    public EventLogger(RequirementEventLogRepository requirementEventLogRepository) {
        this.requirementEventLogRepository = requirementEventLogRepository;
    }

    public void insertEvent(List<RequirementChangeRequest> requirementChangeRequestList, EventType eventType) {
        List<RequirementEventLog> requirementEventLogs = new ArrayList<>();
        requirementChangeRequestList.forEach(item -> {
            List<RequirementChangeMap> requirementChangeMaps = item.getRequirementChangeMaps();
            Requirement requirement = item.getRequirement();
            requirementChangeMaps.forEach(changeMap -> {
                RequirementEventLog requirementEventLog = new RequirementEventLog();
                requirementEventLog.setUserId(changeMap.getUser());
                requirementEventLog.setAttribute(changeMap.getAttribute());
                requirementEventLog.setNewValue(changeMap.getNewValue());
                requirementEventLog.setOldValue(changeMap.getOldValue());
                requirementEventLog.setReason(changeMap.getReason());
                requirementEventLog.setEntityId(requirement.getId());
                requirementEventLogs.add(requirementEventLog);
                requirementEventLog.setEventType(eventType.toString());
            });
        });
        requirementEventLogRepository.persist(requirementEventLogs);
    }

    private String getCurrentTimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(new Date());
    }
}
