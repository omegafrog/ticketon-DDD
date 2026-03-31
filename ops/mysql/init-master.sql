CREATE USER IF NOT EXISTS 'repl_user'@'%' IDENTIFIED BY 'repl_password';
ALTER USER 'repl_user'@'%' IDENTIFIED BY 'repl_password';
GRANT REPLICATION SLAVE ON *.* TO 'repl_user'@'%';

CREATE USER IF NOT EXISTS 'batch_user'@'%' IDENTIFIED BY 'batch_password';
GRANT SELECT, PROCESS, REPLICATION CLIENT ON *.* TO 'batch_user'@'%';

CREATE DATABASE IF NOT EXISTS ticketon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'ticketon'@'%' IDENTIFIED BY 'ticketon';
GRANT ALL PRIVILEGES ON ticketon.* TO 'ticketon'@'%';

FLUSH PRIVILEGES;
