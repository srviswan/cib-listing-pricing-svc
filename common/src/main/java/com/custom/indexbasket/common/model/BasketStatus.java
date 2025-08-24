package com.custom.indexbasket.common.model;

import lombok.Getter;

import java.util.Set;

/**
 * Basket lifecycle status enum with state transition logic
 * Based on the comprehensive state machine design
 */
@Getter
public enum BasketStatus {
    
    // Creation States
    DRAFT("Draft", "User is creating/editing basket", true, false),
    
    // Backtesting States  
    BACKTESTING("Backtesting", "Running historical analysis", false, false),
    BACKTESTED("Backtested", "Backtest completed successfully", true, false),
    BACKTEST_FAILED("Backtest Failed", "Backtest encountered errors", true, false),
    
    // Approval States
    PENDING_APPROVAL("Pending Approval", "Submitted for approval", false, false),
    APPROVED("Approved", "Approved by designated approver", false, false),
    REJECTED("Rejected", "Rejected by approver", true, false),
    
    // Publishing States
    LISTING("Listing", "Publishing to vendor platforms", false, false),
    LISTED("Listed", "Successfully listed on vendor platforms", false, false),
    LISTING_FAILED("Listing Failed", "Failed to list on vendor platforms", true, false),
    
    // Operational States
    ACTIVE("Active", "Live basket with real-time pricing", false, true),
    SUSPENDED("Suspended", "Temporarily suspended from trading", true, true),
    DELISTED("Delisted", "Removed from vendor platforms", false, false),
    
    // Terminal States
    DELETED("Deleted", "Basket permanently deleted", false, false);
    
    private final String displayName;
    private final String description;
    private final boolean userEditable;
    private final boolean operational;
    
    BasketStatus(String displayName, String description, boolean userEditable, boolean operational) {
        this.displayName = displayName;
        this.description = description;
        this.userEditable = userEditable;
        this.operational = operational;
    }
    
    public boolean isTerminal() {
        return this == DELETED || this == DELISTED;
    }
    
    public boolean canTransitionTo(BasketStatus targetStatus) {
        if (this == targetStatus) {
            return false; // No self-transitions
        }
        
        return switch (this) {
            case DRAFT -> Set.of(BACKTESTING, DELETED).contains(targetStatus);
            case BACKTESTING -> Set.of(BACKTESTED, BACKTEST_FAILED).contains(targetStatus);
            case BACKTEST_FAILED -> Set.of(DRAFT, DELETED).contains(targetStatus);
            case BACKTESTED -> Set.of(DRAFT, PENDING_APPROVAL, DELETED).contains(targetStatus);
            case PENDING_APPROVAL -> Set.of(APPROVED, REJECTED, DRAFT).contains(targetStatus);
            case REJECTED -> Set.of(DRAFT, DELETED).contains(targetStatus);
            case APPROVED -> Set.of(LISTING).contains(targetStatus);
            case LISTING -> Set.of(LISTED, LISTING_FAILED).contains(targetStatus);
            case LISTING_FAILED -> Set.of(LISTING, SUSPENDED).contains(targetStatus);
            case LISTED -> Set.of(ACTIVE).contains(targetStatus);
            case ACTIVE -> Set.of(SUSPENDED, DELISTED).contains(targetStatus);
            case SUSPENDED -> Set.of(ACTIVE, DELISTED).contains(targetStatus);
            case DELISTED -> false; // Terminal state
            case DELETED -> false; // Terminal state
        };
    }
    
    public boolean canTriggerBacktest() {
        return this == DRAFT;
    }
    
    public boolean canSubmitForApproval() {
        return this == BACKTESTED;
    }
    
    public boolean canApprove() {
        return this == PENDING_APPROVAL;
    }
    
    public boolean canReject() {
        return this == PENDING_APPROVAL;
    }
    
    public boolean canStartListing() {
        return this == APPROVED;
    }
    
    public boolean canActivateTrading() {
        return this == LISTED;
    }
    
    public boolean canSuspend() {
        return this == ACTIVE;
    }
    
    public boolean canResume() {
        return this == SUSPENDED;
    }
    
    public boolean canDelist() {
        return this == ACTIVE || this == SUSPENDED;
    }
    
    public boolean canDelete() {
        return this == DRAFT || this == BACKTEST_FAILED || this == REJECTED;
    }
    
    public boolean requiresApproval() {
        return this == PENDING_APPROVAL;
    }
    
    public boolean isInWorkflow() {
        return this == BACKTESTING || this == PENDING_APPROVAL || this == LISTING;
    }
    
    public boolean isPublished() {
        return this == LISTED || this == ACTIVE || this == SUSPENDED;
    }
    
    public boolean isLive() {
        return this == ACTIVE;
    }
    
    public boolean isSuspended() {
        return this == SUSPENDED;
    }
    
    public boolean isFailed() {
        return this == BACKTEST_FAILED || this == LISTING_FAILED;
    }
    
    public boolean isPending() {
        return this == PENDING_APPROVAL;
    }
    
    public boolean isApproved() {
        return this == APPROVED;
    }
    
    public boolean isRejected() {
        return this == REJECTED;
    }
    
    public boolean isDraft() {
        return this == DRAFT;
    }
    
    public boolean isBacktesting() {
        return this == BACKTESTING;
    }
    
    public boolean isBacktested() {
        return this == BACKTESTED;
    }
    
    public boolean isListing() {
        return this == LISTING;
    }
    
    public boolean isListed() {
        return this == LISTED;
    }
    
    public boolean isDelisted() {
        return this == DELISTED;
    }
    
    public boolean isDeleted() {
        return this == DELETED;
    }
    
    /**
     * Get the next logical states for this status
     */
    public Set<BasketStatus> getNextPossibleStates() {
        return switch (this) {
            case DRAFT -> Set.of(BACKTESTING, DELETED);
            case BACKTESTING -> Set.of(BACKTESTED, BACKTEST_FAILED);
            case BACKTEST_FAILED -> Set.of(DRAFT, DELETED);
            case BACKTESTED -> Set.of(DRAFT, PENDING_APPROVAL, DELETED);
            case PENDING_APPROVAL -> Set.of(APPROVED, REJECTED, DRAFT);
            case REJECTED -> Set.of(DRAFT, DELETED);
            case APPROVED -> Set.of(LISTING);
            case LISTING -> Set.of(LISTED, LISTING_FAILED);
            case LISTING_FAILED -> Set.of(LISTING, SUSPENDED);
            case LISTED -> Set.of(ACTIVE);
            case ACTIVE -> Set.of(SUSPENDED, DELISTED);
            case SUSPENDED -> Set.of(ACTIVE, DELISTED);
            case DELISTED -> Set.of(); // Terminal state
            case DELETED -> Set.of(); // Terminal state
        };
    }
    
    /**
     * Get the previous possible states for this status
     */
    public Set<BasketStatus> getPreviousPossibleStates() {
        return switch (this) {
            case DRAFT -> Set.of(BACKTEST_FAILED, REJECTED, BACKTESTED);
            case BACKTESTING -> Set.of(DRAFT);
            case BACKTESTED -> Set.of(BACKTESTING);
            case BACKTEST_FAILED -> Set.of(BACKTESTING);
            case PENDING_APPROVAL -> Set.of(BACKTESTED);
            case APPROVED -> Set.of(PENDING_APPROVAL);
            case REJECTED -> Set.of(PENDING_APPROVAL);
            case LISTING -> Set.of(APPROVED, LISTING_FAILED);
            case LISTED -> Set.of(LISTING);
            case LISTING_FAILED -> Set.of(LISTING);
            case ACTIVE -> Set.of(LISTED, SUSPENDED);
            case SUSPENDED -> Set.of(ACTIVE, LISTING_FAILED);
            case DELISTED -> Set.of(ACTIVE, SUSPENDED);
            case DELETED -> Set.of(DRAFT, BACKTEST_FAILED, REJECTED);
        };
    }
    
    /**
     * Check if a transition is valid
     */
    public boolean isValidTransition(BasketStatus targetStatus) {
        return canTransitionTo(targetStatus);
    }
    
    /**
     * Get the workflow stage this status represents
     */
    public WorkflowStage getWorkflowStage() {
        return switch (this) {
            case DRAFT -> WorkflowStage.CREATION;
            case BACKTESTING -> WorkflowStage.BACKTESTING;
            case BACKTESTED -> WorkflowStage.BACKTESTING;
            case BACKTEST_FAILED -> WorkflowStage.BACKTESTING;
            case PENDING_APPROVAL -> WorkflowStage.APPROVAL;
            case APPROVED -> WorkflowStage.APPROVAL;
            case REJECTED -> WorkflowStage.APPROVAL;
            case LISTING -> WorkflowStage.PUBLISHING;
            case LISTED -> WorkflowStage.PUBLISHING;
            case LISTING_FAILED -> WorkflowStage.PUBLISHING;
            case ACTIVE -> WorkflowStage.OPERATIONAL;
            case SUSPENDED -> WorkflowStage.OPERATIONAL;
            case DELISTED -> WorkflowStage.TERMINAL;
            case DELETED -> WorkflowStage.TERMINAL;
        };
    }
    
    /**
     * Workflow stages for the basket lifecycle
     */
    public enum WorkflowStage {
        CREATION("Creation"),
        BACKTESTING("Backtesting"),
        APPROVAL("Approval"),
        PUBLISHING("Publishing"),
        OPERATIONAL("Operational"),
        TERMINAL("Terminal");
        
        private final String displayName;
        
        WorkflowStage(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    @Override
    public String toString() {
        return name();
    }
}
