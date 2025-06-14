-- Создаем временные колонки UUID
ALTER TABLE accounts ADD COLUMN id_new UUID;
ALTER TABLE accounts ADD COLUMN user_id_new UUID;
ALTER TABLE payment_tasks ADD COLUMN id_new UUID;
ALTER TABLE payment_tasks ADD COLUMN order_id_new UUID;
ALTER TABLE payment_result_outbox ADD COLUMN id_new UUID;
ALTER TABLE payment_result_outbox ADD COLUMN order_id_new UUID;

-- Преобразуем существующие ID в UUID
UPDATE accounts SET id_new = gen_random_uuid();
UPDATE accounts SET user_id_new = gen_random_uuid();
UPDATE payment_tasks SET id_new = gen_random_uuid();
UPDATE payment_tasks SET order_id_new = gen_random_uuid();
UPDATE payment_result_outbox SET id_new = gen_random_uuid();
UPDATE payment_result_outbox SET order_id_new = gen_random_uuid();

-- Удаляем старые колонки и переименовываем новые
ALTER TABLE accounts DROP CONSTRAINT IF EXISTS accounts_pkey;
ALTER TABLE payment_tasks DROP CONSTRAINT IF EXISTS payment_tasks_pkey;
ALTER TABLE payment_result_outbox DROP CONSTRAINT IF EXISTS payment_result_outbox_pkey;

ALTER TABLE accounts DROP COLUMN id;
ALTER TABLE accounts DROP COLUMN user_id;
ALTER TABLE payment_tasks DROP COLUMN id;
ALTER TABLE payment_tasks DROP COLUMN order_id;
ALTER TABLE payment_result_outbox DROP COLUMN id;
ALTER TABLE payment_result_outbox DROP COLUMN order_id;

ALTER TABLE accounts RENAME COLUMN id_new TO id;
ALTER TABLE accounts RENAME COLUMN user_id_new TO user_id;
ALTER TABLE payment_tasks RENAME COLUMN id_new TO id;
ALTER TABLE payment_tasks RENAME COLUMN order_id_new TO order_id;
ALTER TABLE payment_result_outbox RENAME COLUMN id_new TO id;
ALTER TABLE payment_result_outbox RENAME COLUMN order_id_new TO order_id;

-- Добавляем первичные ключи
ALTER TABLE accounts ADD PRIMARY KEY (id);
ALTER TABLE payment_tasks ADD PRIMARY KEY (id);
ALTER TABLE payment_result_outbox ADD PRIMARY KEY (id);

-- Добавляем внешние ключи
ALTER TABLE payment_tasks ADD CONSTRAINT fk_payment_tasks_order_id FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE payment_result_outbox ADD CONSTRAINT fk_payment_result_outbox_order_id FOREIGN KEY (order_id) REFERENCES orders(id); 