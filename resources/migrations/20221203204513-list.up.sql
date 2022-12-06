create table list (
    list_id integer primary key,
    name text not null,
    public_external_id text unique not null,
    private_external_id text unique not null
)
