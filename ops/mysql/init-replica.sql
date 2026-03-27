CREATE USER IF NOT EXISTS 'batch_analyze'@'%' IDENTIFIED BY 'analyze_password';
CREATE USER IF NOT EXISTS 'batch_analyze'@'localhost' IDENTIFIED BY 'analyze_password';

GRANT SELECT, PROCESS, REFERENCES, INDEX ON *.* TO 'batch_analyze'@'%';
GRANT SELECT, PROCESS, REFERENCES, INDEX ON *.* TO 'batch_analyze'@'localhost';

CREATE DATABASE IF NOT EXISTS ticketon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'ticketon'@'%' IDENTIFIED BY 'ticketon';
GRANT SELECT ON ticketon.* TO 'batch_analyze'@'%';
GRANT SELECT ON ticketon.* TO 'ticketon'@'%';

FLUSH PRIVILEGES;
