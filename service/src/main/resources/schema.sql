CREATE SEQUENCE IF NOT EXISTS `acquisition_service_id_seq`
    MAXVALUE 999999999
    INCREMENT BY 1
    START WITH 1
    NOCACHE
    NOCYCLE;

CREATE TABLE IF NOT EXISTS `acquisition_service`
(
    `id` BIGINT NOT NULL DEFAULT nextval('acquisition_service_id_seq') PRIMARY KEY,
    `name` VARCHAR NOT NULL,
    `gav` CHARACTER VARYING(4096) NOT NULL,
    `description` CHARACTER VARYING(4096) NOT NULL,
    `state` TINYINT,
    `path` CHARACTER VARYING(4096)
    );