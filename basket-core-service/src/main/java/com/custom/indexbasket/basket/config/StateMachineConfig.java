package com.custom.indexbasket.basket.config;

import com.custom.indexbasket.common.model.BasketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

@Configuration
public class StateMachineConfig {
    
    private static final Logger log = LoggerFactory.getLogger(StateMachineConfig.class);
    
    public StateMachineConfig() {
        log.info("🔧 StateMachineConfig constructor called - State machine workflow being configured!");
    }

    /**
     * Simple status transition logic (temporary until state machine is fully integrated)
     */
    public BasketStatus determineNewStatus(BasketStatus currentStatus, String event) {
        log.info("🔧 Determining new status for current: {} with event: {}", currentStatus, event);
        
        return switch (currentStatus) {
            case DRAFT -> switch (event) {
                case "START_BACKTEST" -> BasketStatus.BACKTESTING;
                case "DELETE" -> BasketStatus.DELETED;
                default -> null;
            };
            case BACKTESTING -> switch (event) {
                case "BACKTEST_COMPLETE" -> BasketStatus.BACKTESTED;
                case "BACKTEST_FAIL" -> BasketStatus.BACKTEST_FAILED;
                default -> null;
            };
            case BACKTESTED -> switch (event) {
                case "SUBMIT_FOR_APPROVAL" -> BasketStatus.PENDING_APPROVAL;
                case "DELETE" -> BasketStatus.DELETED;
                default -> null;
            };
            case PENDING_APPROVAL -> switch (event) {
                case "APPROVE" -> BasketStatus.APPROVED;
                case "REJECT" -> BasketStatus.REJECTED;
                default -> null;
            };
            case APPROVED -> switch (event) {
                case "START_LISTING" -> BasketStatus.LISTING;
                default -> null;
            };
            case LISTING -> switch (event) {
                case "LISTING_COMPLETE" -> BasketStatus.LISTED;
                case "LISTING_FAIL" -> BasketStatus.LISTING_FAILED;
                default -> null;
            };
            case LISTED -> switch (event) {
                case "ACTIVATE" -> BasketStatus.ACTIVE;
                default -> null;
            };
            case ACTIVE -> switch (event) {
                case "SUSPEND" -> BasketStatus.SUSPENDED;
                case "DELIST" -> BasketStatus.DELISTED;
                default -> null;
            };
            case SUSPENDED -> switch (event) {
                case "RESUME" -> BasketStatus.ACTIVE;
                case "DELIST" -> BasketStatus.DELISTED;
                default -> null;
            };
            case REJECTED -> switch (event) {
                case "REVISE" -> BasketStatus.DRAFT;
                case "DELETE" -> BasketStatus.DELETED;
                default -> null;
            };
            default -> null;
        };
    }
}
