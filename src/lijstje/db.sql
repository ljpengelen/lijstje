-- :name create-list! :<! :1
-- :doc Create a list and return it
insert into list (name, public_external_id, private_external_id)
values (:name, :public-external-id, :private-external-id)
returning list_id "list-id",
    name,
    public_external_id "public-external-id",
    private_external_id "private-external-id"

-- :name get-all-lists :? :*
-- :doc Get all lists
select list_id "list-id",
    name,
    public_external_id "public-external-id",
    private_external_id "private-external-id"
from list

-- :name get-list-by-public-id :? :1
-- :doc Get a list by its public external ID
select list_id "list-id",
    name,
    public_external_id "public-external-id",
    private_external_id "private-external-id"
from list where public_external_id = :id

-- :name get-list-by-private-id :? :1
-- :doc Get a list by its private external ID
select list_id "list-id",
    name,
    public_external_id "public-external-id",
    private_external_id "private-external-id"
from list where private_external_id = :id

-- :name create-gift! :<! :1
-- :doc Create a gift and return it
insert into gift (external_id, list_id, name, price, description)
values (:external-id, :list-id, :name, :price, :description)
returning gift_id "gift-id",
    external_id "external-id",
    list_id "list-id",
    name,
    price,
    description

-- :name get-gifts-by-list-id :? :*
-- :doc Get all gifts for a given list
select gift_id "gift-id",
    external_id "external-id",
    list_id "list-id",
    name,
    price,
    description,
    reserved_by "reserved-by",
    reserved_at "reserved-at"
from gift where list_id = :id

-- :name get-gift-by-external-id :? :1
-- :doc Get a gift by its external ID
select gift_id "gift-id",
    external_id "external-id",
    list_id "list-id",
    name,
    price,
    description,
    reserved_by "reserved-by",
    reserved_at "reserved-at"
from gift where external_id = :id

-- :name reserve-gift! :! :n
-- :doc Reserve the gift with the given external ID
update gift
set reserved_by = :reserved-by,
    reserved_at = :reserved-at
where external_id = :external-id and
    reserved_by is null and
    reserved_at is null

-- :name cancel-gift-reservation! :! :n
-- :doc Cancel the reservation of the gift with the given external ID
update gift
set reserved_by = null,
    reserved_at = null
where external_id = :external-id

-- :name delete-list-by-private-id! :! :n
-- :doc Delete the list with the given private external ID
delete from list
where private_external_id = :id

-- :name delete-gifts-by-private-list-id! :! :n
-- :doc Delete the gifts belonging to the list with the given private external ID
delete from gift
where list_id = (
    select list_id from list
    where private_external_id = :id)

-- :name update-gift! :! :n
-- :doc Update the gift with the given external ID
update gift
set name = :name,
    price = :price,
    description = :description
where external_id = :id

-- :name delete-gift! :! :n
-- :doc Delete the gift with the given external ID
delete from gift
where external_id = :id
