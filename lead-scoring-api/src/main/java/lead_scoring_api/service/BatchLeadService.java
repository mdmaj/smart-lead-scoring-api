package lead_scoring_api.service;

import lead_scoring_api.dto.BatchLeadRequest;
import lead_scoring_api.dto.LeadRequest;
import lead_scoring_api.entity.Lead;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BatchLeadService {

    private final LeadService leadService;

    public BatchLeadService(LeadService leadService) {
        this.leadService = leadService;
    }

    public List<Lead> createBatch(BatchLeadRequest request) {

        List<Lead> results = new ArrayList<>();

        for (LeadRequest leadRequest : request.getLeads()) {

            Lead lead = leadService.createLead(leadRequest);

            results.add(lead);
        }

        return results;
    }
}