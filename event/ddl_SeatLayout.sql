CREATE TABLE seat
(
    layout_id BIGINT       NOT NULL,
    seat_id   VARCHAR(255) NULL,
    signature VARCHAR(255) NULL,
    grade     VARCHAR(255) NULL,
    amount    INT          NULL
);

CREATE TABLE seat_layout
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    layout        VARCHAR(255)          NULL,
    seats_seat_id VARCHAR(255)          NULL,
    CONSTRAINT pk_seat_layout PRIMARY KEY (id)
);

ALTER TABLE seat
    ADD CONSTRAINT fk_seat_on_seat_layout FOREIGN KEY (layout_id) REFERENCES seat_layout (id);