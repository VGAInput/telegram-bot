
CREATE TABLE notification_tasks
(
    id                     BIGSERIAL bigint GENERATES BY DEFAULT AS IDENTITY PRIMARY KEY,
    message                TEXT      NOT NULL,
    notification_date_time TIMESTAMP NOT NULL
);
