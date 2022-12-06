create table gift (
    gift_id integer primary key,
    list_id integer not null,
    description text not null,
    reserved_at text,
    reserved_by text,
    foreign key(list_id) references list(list_id)
)
