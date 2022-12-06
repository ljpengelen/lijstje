alter table gift add column external_id text not null;
--;;
create unique index gift_external_id on gift(external_id);
