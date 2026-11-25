CREATE TABLE security_audit_events (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       actor_user_id BIGINT NULL,
                                       target_user_id BIGINT NULL,
                                       action VARCHAR(60) NOT NULL,
                                       outcome VARCHAR(20) NOT NULL,
                                       ip_address VARCHAR(45) NULL,
                                       trace_id VARCHAR(64) NULL,
                                       details VARCHAR(500) NULL,
                                       created_at DATETIME(6) NOT NULL,

                                       PRIMARY KEY (id),

                                       INDEX idx_audit_actor_id (actor_user_id),
                                       INDEX idx_audit_action_created (action, created_at),
                                       INDEX idx_audit_target_id (target_user_id)
) ENGINE = InnoDB;