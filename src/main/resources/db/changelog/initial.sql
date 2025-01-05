create table site (
  id serial not null,
  status varchar(255) not null,
  status_time timestamp not null,
  last_error text,
  url varchar(255) not null,
  name varchar(255) not null,
  primary key (id)
);

create table page (
    id serial not null,
    site_id integer not null references site (id),
    path varchar(512) not null,
    code integer not null,
    content text not null,
    primary key (id)
);

create table lemma (
    id serial not null,
    site_id integer not null references site (id),
    lemma varchar(255) not null,
    frequency integer not null,
    primary key (id)
);

create table indexes (
    id serial not null,
    page_id integer not null references page (id),
    lemma_id integer not null references lemma (id),
    ranks real not null,
    primary key (id)
);