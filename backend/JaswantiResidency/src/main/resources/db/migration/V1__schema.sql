
    create table admin_activity_logs (
        admin_id bigint not null,
        id bigint not null auto_increment,
        timestamp datetime(6) not null,
        description varchar(2000),
        action varchar(255) not null,
        ip_address varchar(255),
        module varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table announcements (
        created_by_id bigint not null,
        created_date datetime(6),
        expiry_date datetime(6),
        id bigint not null auto_increment,
        description varchar(2000) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table complaints (
        created_date datetime(6),
        id bigint not null auto_increment,
        resident_id bigint not null,
        description varchar(1000) not null,
        category varchar(255) not null,
        image_url varchar(255),
        status enum ('IN_PROGRESS','OPEN','RESOLVED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table maintenance_bills (
        amount float(53) not null,
        due_date date not null,
        late_fee float(53),
        id bigint not null auto_increment,
        room_id bigint not null,
        billing_month varchar(255) not null,
        status enum ('PAID','PAID_CASH','PENDING') not null,
        primary key (id)
    ) engine=InnoDB;

    create table rooms (
        floor integer not null,
        id bigint not null auto_increment,
        resident_id bigint,
        room_number varchar(255) not null,
        wing varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table transactions (
        amount float(53) not null,
        bill_id bigint not null,
        id bigint not null auto_increment,
        timestamp datetime(6) not null,
        gateway_payment_id varchar(255),
        payment_mode varchar(255),
        status varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table users (
        email_verified bit,
        is_active bit not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        verified_at datetime(6),
        email varchar(255),
        name varchar(255) not null,
        password varchar(255) not null,
        phone varchar(255),
        role enum ('ADMIN','RESIDENT') not null,
        status enum ('ACTIVE','BLOCKED','INACTIVE') not null,
        primary key (id)
    ) engine=InnoDB;

    alter table users 
       add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);

    alter table admin_activity_logs 
       add constraint FKitbisfifqj9fqo71et0bl1xob 
       foreign key (admin_id) 
       references users (id);

    alter table announcements 
       add constraint FK2xq7ufsyexvej4l6ld869ddxy 
       foreign key (created_by_id) 
       references users (id);

    alter table complaints 
       add constraint FKcpsq2ncd9smckstcuybinq8da 
       foreign key (resident_id) 
       references users (id);

    alter table maintenance_bills 
       add constraint FK3yfpfws5a5k9ontv2iyod7h9q 
       foreign key (room_id) 
       references rooms (id);

    alter table rooms 
       add constraint FKl1r4fbkdpbprrnxddkdiqwahr 
       foreign key (resident_id) 
       references users (id);

    alter table transactions 
       add constraint FKsqx0e1p2m2o38emnm8n1q7sqo 
       foreign key (bill_id) 
       references maintenance_bills (id);
