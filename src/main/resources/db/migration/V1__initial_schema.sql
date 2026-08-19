create table products (
    id uuid primary key,
    sku varchar(50) not null,
    name varchar(160) not null,
    active boolean not null,
    constraint uk_products_sku unique (sku)
);

create table warehouses (
    id uuid primary key,
    code varchar(30) not null,
    name varchar(120) not null,
    active boolean not null,
    constraint uk_warehouses_code unique (code)
);

create table stock_balances (
    id uuid primary key,
    warehouse_id uuid not null references warehouses(id),
    product_id uuid not null references products(id),
    on_hand bigint not null,
    reserved bigint not null,
    version bigint not null,
    constraint uk_stock_balance_warehouse_product unique (warehouse_id, product_id),
    constraint ck_stock_balance_on_hand check (on_hand >= 0),
    constraint ck_stock_balance_reserved check (reserved >= 0 and reserved <= on_hand)
);

create table stock_reservations (
    id uuid primary key,
    warehouse_id uuid not null references warehouses(id),
    product_id uuid not null references products(id),
    quantity bigint not null,
    external_reference varchar(100) not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null,
    constraint uk_stock_reservation_reference unique (external_reference),
    constraint ck_stock_reservation_quantity check (quantity > 0),
    constraint ck_stock_reservation_status check (status in ('ACTIVE', 'RELEASED', 'CONSUMED'))
);

create table stock_movements (
    id uuid primary key,
    warehouse_id uuid not null references warehouses(id),
    product_id uuid not null references products(id),
    reservation_id uuid references stock_reservations(id),
    type varchar(30) not null,
    quantity bigint not null,
    external_reference varchar(120) not null,
    occurred_at timestamptz not null,
    constraint uk_stock_movement_reference unique (external_reference),
    constraint ck_stock_movement_quantity check (quantity > 0),
    constraint ck_stock_movement_type check (type in (
        'RECEIPT', 'MANUAL_ISSUE', 'RESERVATION', 'RELEASE', 'ORDER_ISSUE', 'TRANSFER_OUT', 'TRANSFER_IN'))
);

create index ix_stock_movements_warehouse_time on stock_movements (warehouse_id, occurred_at desc);
create index ix_stock_movements_product_time on stock_movements (product_id, occurred_at desc);
create index ix_stock_reservations_status on stock_reservations (status);

create table orders (
    id uuid primary key,
    warehouse_id uuid not null references warehouses(id),
    external_reference varchar(100) not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null,
    constraint uk_orders_reference unique (external_reference),
    constraint ck_order_status check (status in ('CREATED', 'RESERVED', 'CONFIRMED', 'CANCELLED'))
);

create table order_items (
    id uuid primary key,
    order_id uuid not null references orders(id) on delete cascade,
    product_id uuid not null references products(id),
    reservation_id uuid not null references stock_reservations(id),
    quantity bigint not null,
    constraint uk_order_item_product unique (order_id, product_id),
    constraint ck_order_item_quantity check (quantity > 0)
);

create index ix_order_items_order on order_items (order_id);
create index ix_orders_warehouse_status on orders (warehouse_id, status);

create table app_users (
    id uuid primary key,
    username varchar(80) not null,
    password_hash varchar(100) not null,
    enabled boolean not null,
    constraint uk_app_users_username unique (username)
);

create table user_roles (
    user_id uuid not null references app_users(id) on delete cascade,
    role varchar(30) not null,
    primary key (user_id, role),
    constraint ck_user_role check (role in ('ADMIN', 'STOCK_OPERATOR', 'ORDER_OPERATOR', 'VIEWER'))
);
