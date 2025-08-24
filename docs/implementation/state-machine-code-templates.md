# State Machine Implementation Code Templates

## 1. BasketState Enum

```java
package com.custom.indexbasket.core.model;

public enum BasketState {
    // Creation States
    DRAFT("Draft", "User is creating/editing basket", true),
    
    // Backtesting States  
    BACKTESTING("Backtesting", "Running historical analysis", false),
    BACKTESTED("Backtested", "Backtest completed successfully", true),
    BACKTEST_FAILED("Backtest Failed", "Backtest encountered errors", true),
    
    // Approval States
    PENDING_APPROVAL("Pending Approval", "Submitted for approval", false),
    APPROVED("Approved", "Approved by designated approver", false),
    REJECTED("Rejected", "Rejected by approver", true),
    
    // Publishing States
    LISTING("Listing", "Publishing to vendor platforms", false),
    LISTED("Listed", "Successfully listed on vendor platforms", false),
    LISTING_FAILED("Listing Failed", "Failed to list on vendor platforms", true),
    
    // Operational States
    ACTIVE("Active", "Live basket with real-time pricing", false),
    SUSPENDED("Suspended", "Temporarily suspended from trading", true),
    DELISTED("Delisted", "Removed from vendor platforms", false),
    
    // Terminal States
    DELETED("Deleted", "Basket permanently deleted", false);
    
    private final String displayName;
    private final String description;
    private final boolean userEditable;
    
    BasketState(String displayName, String description, boolean userEditable) {
        this.displayName = displayName;
        this.description = description;
        this.userEditable = userEditable;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public boolean isUserEditable() { return userEditable; }
    
    public boolean isTerminal() {
        return this == DELETED || this == DELISTED;
    }
    
    public boolean isOperational() {
        return this == ACTIVE || this == SUSPENDED;
    }
    
    public boolean canTransitionTo(BasketState target) {
        // Define valid transitions (implement based on state diagram)
        switch (this) {
            case DRAFT:
                return target == BACKTESTING || target == DELETED;
            case BACKTESTING:
                return target == BACKTESTED || target == BACKTEST_FAILED;
            case BACKTESTED:
                return target == DRAFT || target == PENDING_APPROVAL || target == DELETED;
            case PENDING_APPROVAL:
                return target == APPROVED || target == REJECTED || target == DRAFT;
            case APPROVED:
                return target == LISTING;
            case LISTING:
                return target == LISTED || target == LISTING_FAILED;
            case LISTED:
                return target == ACTIVE;
            case ACTIVE:
                return target == SUSPENDED || target == DELISTED;
            case SUSPENDED:
                return target == ACTIVE || target == DELISTED;
            default:
                return false;
        }
    }
}
```

## 2. BasketEvent Enum

```java
package com.custom.indexbasket.core.model;

public enum BasketEvent {
    // User Actions
    CREATE_BASKET("Create Basket", "User creates new basket"),
    MODIFY_BASKET("Modify Basket", "User modifies basket contents"),
    TRIGGER_BACKTEST("Trigger Backtest", "User initiates backtesting"),
    SUBMIT_FOR_APPROVAL("Submit for Approval", "User submits for approval workflow"),
    WITHDRAW_SUBMISSION("Withdraw Submission", "User withdraws from approval"),
    DELETE_BASKET("Delete Basket", "User deletes basket"),
    
    // System Events
    BACKTEST_COMPLETED("Backtest Completed", "Backtest process finished successfully"),
    BACKTEST_FAILED("Backtest Failed", "Backtest process encountered errors"),
    
    // Approval Events
    APPROVE_BASKET("Approve Basket", "Approver approves the basket"),
    REJECT_BASKET("Reject Basket", "Approver rejects the basket"),
    
    // Publishing Events
    START_LISTING("Start Listing", "Begin vendor listing process"),
    LISTING_COMPLETED("Listing Completed", "Vendor listing successful"),
    LISTING_FAILED("Listing Failed", "Vendor listing failed"),
    RETRY_LISTING("Retry Listing", "Retry failed listing"),
    
    // Operational Events
    ACTIVATE_TRADING("Activate Trading", "Start real-time pricing"),
    SUSPEND_TRADING("Suspend Trading", "Pause trading operations"),
    RESUME_TRADING("Resume Trading", "Resume trading operations"),
    DELIST_BASKET("Delist Basket", "Remove from vendor platforms"),
    
    // Admin Events
    ADMIN_SUSPEND("Admin Suspend", "Administrator suspends basket"),
    ADMIN_RESUME("Admin Resume", "Administrator resumes basket"),
    ADMIN_DELIST("Admin Delist", "Administrator delists basket");
    
    private final String displayName;
    private final String description;
    
    BasketEvent(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    public boolean isUserAction() {
        return name().startsWith("CREATE_") || name().startsWith("MODIFY_") || 
               name().startsWith("TRIGGER_") || name().startsWith("SUBMIT_") ||
               name().startsWith("WITHDRAW_") || name().startsWith("DELETE_");
    }
    
    public boolean isAdminAction() {
        return name().startsWith("ADMIN_");
    }
    
    public boolean isSystemEvent() {
        return name().contains("_COMPLETED") || name().contains("_FAILED");
    }
}
```

## 3. State Machine Configuration

```java
package com.custom.indexbasket.core.config;

import com.custom.indexbasket.core.model.BasketState;
import com.custom.indexbasket.core.model.BasketEvent;
import com.custom.indexbasket.core.statemachine.BasketStateMachineActions;
import com.custom.indexbasket.core.statemachine.BasketStateMachineGuards;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachine
public class BasketStateMachineConfig extends StateMachineConfigurerAdapter<BasketState, BasketEvent> {
    
    @Autowired
    private BasketStateMachineActions actions;
    
    @Autowired
    private BasketStateMachineGuards guards;
    
    @Override
    public void configure(StateMachineStateConfigurer<BasketState, BasketEvent> states) throws Exception {
        states
            .withStates()
                .initial(BasketState.DRAFT)
                .states(EnumSet.allOf(BasketState.class))
                .end(BasketState.DELETED)
                .end(BasketState.DELISTED);
    }
    
    @Override
    public void configure(StateMachineTransitionConfigurer<BasketState, BasketEvent> transitions) throws Exception {
        transitions
            // Creation & Editing
            .withExternal()
                .source(BasketState.DRAFT).target(BasketState.DRAFT)
                .event(BasketEvent.MODIFY_BASKET)
                .action(actions.modifyBasketAction())
                
            .and().withExternal()
                .source(BasketState.DRAFT).target(BasketState.BACKTESTING)
                .event(BasketEvent.TRIGGER_BACKTEST)
                .guard(guards.basketValidGuard())
                .action(actions.startBacktestAction())
                
            .and().withExternal()
                .source(BasketState.DRAFT).target(BasketState.DELETED)
                .event(BasketEvent.DELETE_BASKET)
                .action(actions.deleteBasketAction())
                
            // Backtesting
            .and().withExternal()
                .source(BasketState.BACKTESTING).target(BasketState.BACKTESTED)
                .event(BasketEvent.BACKTEST_COMPLETED)
                .action(actions.backtestCompletedAction())
                
            .and().withExternal()
                .source(BasketState.BACKTESTING).target(BasketState.BACKTEST_FAILED)
                .event(BasketEvent.BACKTEST_FAILED)
                .action(actions.backtestFailedAction())
                
            .and().withExternal()
                .source(BasketState.BACKTEST_FAILED).target(BasketState.DRAFT)
                .event(BasketEvent.MODIFY_BASKET)
                .action(actions.resetBacktestAction())
                
            .and().withExternal()
                .source(BasketState.BACKTESTED).target(BasketState.DRAFT)
                .event(BasketEvent.MODIFY_BASKET)
                .action(actions.resetBacktestAction())
                
            // Approval Process
            .and().withExternal()
                .source(BasketState.BACKTESTED).target(BasketState.PENDING_APPROVAL)
                .event(BasketEvent.SUBMIT_FOR_APPROVAL)
                .guard(guards.backtestValidGuard())
                .action(actions.submitForApprovalAction())
                
            .and().withExternal()
                .source(BasketState.PENDING_APPROVAL).target(BasketState.APPROVED)
                .event(BasketEvent.APPROVE_BASKET)
                .guard(guards.approverAuthGuard())
                .action(actions.approveBasketAction())
                
            .and().withExternal()
                .source(BasketState.PENDING_APPROVAL).target(BasketState.REJECTED)
                .event(BasketEvent.REJECT_BASKET)
                .guard(guards.approverAuthGuard())
                .action(actions.rejectBasketAction())
                
            .and().withExternal()
                .source(BasketState.PENDING_APPROVAL).target(BasketState.DRAFT)
                .event(BasketEvent.WITHDRAW_SUBMISSION)
                .guard(guards.ownerAuthGuard())
                .action(actions.withdrawSubmissionAction())
                
            .and().withExternal()
                .source(BasketState.REJECTED).target(BasketState.DRAFT)
                .event(BasketEvent.MODIFY_BASKET)
                .action(actions.reviseRejectedBasketAction())
                
            // Publishing Process
            .and().withExternal()
                .source(BasketState.APPROVED).target(BasketState.LISTING)
                .event(BasketEvent.START_LISTING)
                .action(actions.startListingAction())
                
            .and().withExternal()
                .source(BasketState.LISTING).target(BasketState.LISTED)
                .event(BasketEvent.LISTING_COMPLETED)
                .action(actions.listingCompletedAction())
                
            .and().withExternal()
                .source(BasketState.LISTING).target(BasketState.LISTING_FAILED)
                .event(BasketEvent.LISTING_FAILED)
                .action(actions.listingFailedAction())
                
            .and().withExternal()
                .source(BasketState.LISTING_FAILED).target(BasketState.LISTING)
                .event(BasketEvent.RETRY_LISTING)
                .guard(guards.retryLimitGuard())
                .action(actions.retryListingAction())
                
            // Operational States
            .and().withExternal()
                .source(BasketState.LISTED).target(BasketState.ACTIVE)
                .event(BasketEvent.ACTIVATE_TRADING)
                .action(actions.activateTradingAction())
                
            .and().withExternal()
                .source(BasketState.ACTIVE).target(BasketState.SUSPENDED)
                .event(BasketEvent.ADMIN_SUSPEND)
                .guard(guards.adminAuthGuard())
                .action(actions.suspendTradingAction())
                
            .and().withExternal()
                .source(BasketState.SUSPENDED).target(BasketState.ACTIVE)
                .event(BasketEvent.ADMIN_RESUME)
                .guard(guards.adminAuthGuard())
                .action(actions.resumeTradingAction())
                
            .and().withExternal()
                .source(BasketState.ACTIVE).target(BasketState.DELISTED)
                .event(BasketEvent.ADMIN_DELIST)
                .guard(guards.adminAuthGuard())
                .action(actions.delistBasketAction())
                
            .and().withExternal()
                .source(BasketState.SUSPENDED).target(BasketState.DELISTED)
                .event(BasketEvent.ADMIN_DELIST)
                .guard(guards.adminAuthGuard())
                .action(actions.delistBasketAction());
    }
}
```

## 4. State Machine Service

```java
package com.custom.indexbasket.core.service;

import com.custom.indexbasket.core.model.BasketState;
import com.custom.indexbasket.core.model.BasketEvent;
import com.custom.indexbasket.core.repository.BasketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineException;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketStateMachineService {
    
    private static final Logger log = LoggerFactory.getLogger(BasketStateMachineService.class);
    
    @Autowired
    private StateMachineService<BasketState, BasketEvent> stateMachineService;
    
    @Autowired
    private BasketRepository basketRepository;
    
    public Mono<Boolean> sendEvent(String basketId, BasketEvent event) {
        return Mono.fromCallable(() -> {
            try {
                StateMachine<BasketState, BasketEvent> stateMachine = 
                    stateMachineService.acquireStateMachine(basketId);
                    
                Message<BasketEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader("basketId", basketId)
                    .setHeader("timestamp", LocalDateTime.now())
                    .build();
                    
                boolean accepted = stateMachine.sendEvent(message);
                
                if (accepted) {
                    log.info("✅ State transition successful: {} -> {} for basket {}", 
                        event, stateMachine.getState().getId(), basketId);
                        
                    // Persist state change
                    updateBasketState(basketId, stateMachine.getState().getId());
                } else {
                    log.warn("⚠️ State transition rejected: {} for basket {}", event, basketId);
                }
                
                stateMachineService.releaseStateMachine(basketId);
                return accepted;
                
            } catch (Exception e) {
                log.error("❌ State machine error for basket {}: {}", basketId, e.getMessage());
                throw new StateMachineException("Failed to process event: " + event, e);
            }
        });
    }
    
    public Mono<BasketState> getCurrentState(String basketId) {
        return Mono.fromCallable(() -> {
            StateMachine<BasketState, BasketEvent> stateMachine = 
                stateMachineService.acquireStateMachine(basketId);
            BasketState currentState = stateMachine.getState().getId();
            stateMachineService.releaseStateMachine(basketId);
            return currentState;
        });
    }
    
    public Mono<List<BasketEvent>> getAvailableTransitions(String basketId) {
        return Mono.fromCallable(() -> {
            StateMachine<BasketState, BasketEvent> stateMachine = 
                stateMachineService.acquireStateMachine(basketId);
                
            List<BasketEvent> transitions = stateMachine.getTransitions().stream()
                .filter(transition -> transition.getSource().getId() == getCurrentState(basketId).block())
                .map(transition -> transition.getTrigger().getEvent())
                .collect(Collectors.toList());
                
            stateMachineService.releaseStateMachine(basketId);
            return transitions;
        });
    }
    
    public Mono<Boolean> canTransition(String basketId, BasketEvent event) {
        return getCurrentState(basketId)
            .flatMap(currentState -> {
                return getAvailableTransitions(basketId)
                    .map(transitions -> transitions.contains(event));
            });
    }
    
    private void updateBasketState(String basketId, BasketState newState) {
        basketRepository.updateState(basketId, newState, LocalDateTime.now())
            .doOnSuccess(updated -> log.debug("State persisted for basket {}: {}", basketId, newState))
            .doOnError(error -> log.error("Failed to persist state for basket {}: {}", basketId, error.getMessage()))
            .subscribe();
    }
}
```

## 5. REST Controller

```java
package com.custom.indexbasket.core.controller;

import com.custom.indexbasket.core.model.BasketState;
import com.custom.indexbasket.core.model.BasketEvent;
import com.custom.indexbasket.core.service.BasketStateMachineService;
import com.custom.indexbasket.core.dto.BasketStateResponse;
import com.custom.indexbasket.core.dto.StateTransitionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/baskets/{basketId}/state")
@CrossOrigin(origins = "*")
public class BasketStateController {
    
    @Autowired
    private BasketStateMachineService stateMachineService;
    
    @GetMapping
    public Mono<ResponseEntity<BasketStateResponse>> getCurrentState(@PathVariable String basketId) {
        return stateMachineService.getCurrentState(basketId)
            .flatMap(currentState -> 
                stateMachineService.getAvailableTransitions(basketId)
                    .map(transitions -> BasketStateResponse.builder()
                        .basketId(basketId)
                        .currentState(currentState)
                        .availableTransitions(transitions)
                        .isTerminal(currentState.isTerminal())
                        .isOperational(currentState.isOperational())
                        .isUserEditable(currentState.isUserEditable())
                        .build())
            )
            .map(ResponseEntity::ok);
    }
    
    @PostMapping("/transition")
    public Mono<ResponseEntity<String>> triggerTransition(
            @PathVariable String basketId,
            @RequestBody StateTransitionRequest request) {
        
        return stateMachineService.canTransition(basketId, request.getEvent())
            .flatMap(canTransition -> {
                if (!canTransition) {
                    return Mono.just(ResponseEntity.badRequest()
                        .body("Invalid transition: " + request.getEvent()));
                }
                
                return stateMachineService.sendEvent(basketId, request.getEvent())
                    .map(success -> {
                        if (success) {
                            return ResponseEntity.ok("State transition successful");
                        } else {
                            return ResponseEntity.badRequest()
                                .body("Transition failed: " + request.getEvent());
                        }
                    });
            })
            .onErrorReturn(ResponseEntity.internalServerError()
                .body("Transition error occurred"));
    }
    
    // Convenience endpoints for common actions
    @PostMapping("/backtest")
    public Mono<ResponseEntity<String>> triggerBacktest(@PathVariable String basketId) {
        return stateMachineService.sendEvent(basketId, BasketEvent.TRIGGER_BACKTEST)
            .map(success -> success ? 
                ResponseEntity.accepted().body("Backtest initiated") :
                ResponseEntity.badRequest().body("Cannot trigger backtest"));
    }
    
    @PostMapping("/submit-approval")
    public Mono<ResponseEntity<String>> submitForApproval(@PathVariable String basketId) {
        return stateMachineService.sendEvent(basketId, BasketEvent.SUBMIT_FOR_APPROVAL)
            .map(success -> success ? 
                ResponseEntity.ok("Submitted for approval") :
                ResponseEntity.badRequest().body("Cannot submit for approval"));
    }
    
    @PostMapping("/approve")
    public Mono<ResponseEntity<String>> approveBasket(@PathVariable String basketId) {
        return stateMachineService.sendEvent(basketId, BasketEvent.APPROVE_BASKET)
            .map(success -> success ? 
                ResponseEntity.ok("Basket approved") :
                ResponseEntity.badRequest().body("Cannot approve basket"));
    }
    
    @PostMapping("/reject")
    public Mono<ResponseEntity<String>> rejectBasket(@PathVariable String basketId) {
        return stateMachineService.sendEvent(basketId, BasketEvent.REJECT_BASKET)
            .map(success -> success ? 
                ResponseEntity.ok("Basket rejected") :
                ResponseEntity.badRequest().body("Cannot reject basket"));
    }
    
    @PostMapping("/suspend")
    public Mono<ResponseEntity<String>> suspendBasket(@PathVariable String basketId) {
        return stateMachineService.sendEvent(basketId, BasketEvent.ADMIN_SUSPEND)
            .map(success -> success ? 
                ResponseEntity.ok("Basket suspended") :
                ResponseEntity.badRequest().body("Cannot suspend basket"));
    }
    
    @PostMapping("/resume")
    public Mono<ResponseEntity<String>> resumeBasket(@PathVariable String basketId) {
        return stateMachineService.sendEvent(basketId, BasketEvent.ADMIN_RESUME)
            .map(success -> success ? 
                ResponseEntity.ok("Basket resumed") :
                ResponseEntity.badRequest().body("Cannot resume basket"));
    }
}
```

## 6. Database Schema

```sql
-- Basket state tracking
CREATE TABLE basket_states (
    basket_id VARCHAR(50) PRIMARY KEY,
    current_state VARCHAR(30) NOT NULL,
    previous_state VARCHAR(30),
    last_transition_at TIMESTAMP NOT NULL,
    transition_count INTEGER DEFAULT 0,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- State transition audit trail
CREATE TABLE basket_state_transitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    basket_id VARCHAR(50) NOT NULL,
    from_state VARCHAR(30),
    to_state VARCHAR(30) NOT NULL,
    event VARCHAR(50) NOT NULL,
    triggered_by VARCHAR(100) NOT NULL,
    transition_at TIMESTAMP NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    context_data JSONB,
    FOREIGN KEY (basket_id) REFERENCES baskets(basket_code)
);

-- Indexes for performance
CREATE INDEX idx_basket_states_current_state ON basket_states(current_state);
CREATE INDEX idx_basket_state_transitions_basket_id ON basket_state_transitions(basket_id);
CREATE INDEX idx_basket_state_transitions_event ON basket_state_transitions(event);
CREATE INDEX idx_basket_state_transitions_timestamp ON basket_state_transitions(transition_at);
```

## 7. Event Publishing

```java
package com.custom.indexbasket.core.events;

import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

public abstract class BasketStateEvent extends ApplicationEvent {
    private final String basketId;
    private final LocalDateTime timestamp;
    
    public BasketStateEvent(Object source, String basketId) {
        super(source);
        this.basketId = basketId;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getBasketId() { return basketId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}

// Specific event classes
public class BasketCreatedEvent extends BasketStateEvent {
    public BasketCreatedEvent(Object source, String basketId) {
        super(source, basketId);
    }
}

public class BasketModifiedEvent extends BasketStateEvent {
    public BasketModifiedEvent(Object source, String basketId) {
        super(source, basketId);
    }
}

public class BacktestCompletedEvent extends BasketStateEvent {
    private final BacktestResult result;
    
    public BacktestCompletedEvent(Object source, String basketId, BacktestResult result) {
        super(source, basketId);
        this.result = result;
    }
    
    public BacktestResult getResult() { return result; }
}

public class BasketApprovedEvent extends BasketStateEvent {
    private final String approver;
    
    public BasketApprovedEvent(Object source, String basketId, String approver) {
        super(source, basketId);
        this.approver = approver;
    }
    
    public String getApprover() { return approver; }
}

public class BasketActivatedEvent extends BasketStateEvent {
    public BasketActivatedEvent(Object source, String basketId) {
        super(source, basketId);
    }
}
```

## 8. Testing Template

```java
package com.custom.indexbasket.core.statemachine;

import com.custom.indexbasket.core.model.BasketState;
import com.custom.indexbasket.core.model.BasketEvent;
import com.custom.indexbasket.core.service.BasketStateMachineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
public class BasketStateMachineTest {
    
    @Autowired
    private BasketStateMachineService stateMachineService;
    
    @Test
    public void testBasicWorkflow() {
        String basketId = "TEST_BASKET_001";
        
        // Test initial state
        StepVerifier.create(stateMachineService.getCurrentState(basketId))
            .expectNext(BasketState.DRAFT)
            .verifyComplete();
        
        // Test backtest trigger
        StepVerifier.create(stateMachineService.sendEvent(basketId, BasketEvent.TRIGGER_BACKTEST))
            .expectNext(true)
            .verifyComplete();
        
        StepVerifier.create(stateMachineService.getCurrentState(basketId))
            .expectNext(BasketState.BACKTESTING)
            .verifyComplete();
        
        // Test backtest completion
        StepVerifier.create(stateMachineService.sendEvent(basketId, BasketEvent.BACKTEST_COMPLETED))
            .expectNext(true)
            .verifyComplete();
        
        StepVerifier.create(stateMachineService.getCurrentState(basketId))
            .expectNext(BasketState.BACKTESTED)
            .verifyComplete();
        
        // Test approval submission
        StepVerifier.create(stateMachineService.sendEvent(basketId, BasketEvent.SUBMIT_FOR_APPROVAL))
            .expectNext(true)
            .verifyComplete();
        
        StepVerifier.create(stateMachineService.getCurrentState(basketId))
            .expectNext(BasketState.PENDING_APPROVAL)
            .verifyComplete();
    }
    
    @Test
    public void testInvalidTransitions() {
        String basketId = "TEST_BASKET_002";
        
        // Try to approve without being in pending approval state
        StepVerifier.create(stateMachineService.sendEvent(basketId, BasketEvent.APPROVE_BASKET))
            .expectNext(false)
            .verifyComplete();
        
        // State should remain DRAFT
        StepVerifier.create(stateMachineService.getCurrentState(basketId))
            .expectNext(BasketState.DRAFT)
            .verifyComplete();
    }
    
    @Test
    public void testAvailableTransitions() {
        String basketId = "TEST_BASKET_003";
        
        StepVerifier.create(stateMachineService.getAvailableTransitions(basketId))
            .expectNextMatches(transitions -> 
                transitions.contains(BasketEvent.TRIGGER_BACKTEST) &&
                transitions.contains(BasketEvent.MODIFY_BASKET) &&
                transitions.contains(BasketEvent.DELETE_BASKET))
            .verifyComplete();
    }
}
```
