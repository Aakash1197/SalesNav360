package com.salesnav360.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunicationRequest(
        @Email @NotBlank @Size(max = 255) String recipientEmail,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 10000) String body,
        Long templateId,
        Long clientId,
        Long leadId) {

    @AssertTrue(message = "A communication must be associated with a client or a lead.")
    public boolean hasCrmAssociation() {
        return clientId != null || leadId != null;
    }
}
