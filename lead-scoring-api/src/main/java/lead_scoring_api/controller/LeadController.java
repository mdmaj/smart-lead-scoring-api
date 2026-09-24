package lead_scoring_api.controller;

import jakarta.validation.Valid;
import lead_scoring_api.dto.BatchLeadRequest;
import lead_scoring_api.dto.LeadRequest;
import lead_scoring_api.entity.Lead;
import lead_scoring_api.service.BatchLeadService;
import lead_scoring_api.service.LeadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;
    private final BatchLeadService batchLeadService;

    public LeadController(
            LeadService leadService,
            BatchLeadService batchLeadService) {

        this.leadService = leadService;
        this.batchLeadService = batchLeadService;
    }

    // Create single lead
    @PostMapping
    public ResponseEntity<Lead> createLead(
            @Valid @RequestBody LeadRequest request) {

        Lead lead = leadService.createLead(request);

        return ResponseEntity.ok(lead);
    }

    // Rescore an existing lead
    @PostMapping("/{id}/rescore")
    public ResponseEntity<Lead> rescoreLead(
            @PathVariable Long id) {

        Lead lead = leadService.rescoreLead(id);

        return ResponseEntity.ok(lead);
    }

    // Create multiple leads
    @PostMapping("/batch")
    public ResponseEntity<List<Lead>> createBatch(
            @Valid @RequestBody BatchLeadRequest request) {

        List<Lead> leads =
                batchLeadService.createBatch(request);

        return ResponseEntity.ok(leads);
    }
}