package lead_scoring_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BatchLeadRequest {

    @NotEmpty(message = "Leads list cannot be empty")
    @Size(max = 50, message = "Maximum 50 leads are allowed")
    @Valid
    private List<LeadRequest> leads;

    public BatchLeadRequest() {
    }

    public List<LeadRequest> getLeads() {
        return leads;
    }

    public void setLeads(List<LeadRequest> leads) {
        this.leads = leads;
    }
}