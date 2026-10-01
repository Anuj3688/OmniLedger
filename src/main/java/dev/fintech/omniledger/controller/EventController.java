package dev.fintech.omniledger.controller;

import dev.fintech.omniledger.dto.request.EventFilterRequest;
import dev.fintech.omniledger.dto.response.SystemEventResponse;
import dev.fintech.omniledger.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for retrieving and filtering immutable system audit events.
 */
@Tag(name = "System Events", description = "Endpoints for retrieving system-wide audit and operational event logs")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "Get all events", description = "Fetches the complete historical audit log ordered chronologically")
    @GetMapping
    public ResponseEntity<List<SystemEventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @Operation(summary = "Filter events by time range", description = "Retrieves audit events that occurred between startTime and endTime")
    @PostMapping("/filter")
    public ResponseEntity<List<SystemEventResponse>> filterEvents(@RequestBody EventFilterRequest filterRequest) {
        return ResponseEntity.ok(eventService.getEventsByTimeRange(filterRequest.startTime(), filterRequest.endTime()));
    }
}
