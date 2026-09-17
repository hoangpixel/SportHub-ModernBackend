select
        c1_0.id,
        c1_0.close_time,
        c1_0.created_at,
        c1_0.description,
        c1_0.name,
        c1_0.open_time,
        c1_0.price_per_hour,
        c1_0.sport_id,
        s1_0.id,
        s1_0.created_at,
        s1_0.description,
        s1_0.icon_url,
        s1_0.name,
        s1_0.status,
        s1_0.updated_at,
        c1_0.status,
        c1_0.updated_at,
        c1_0.venue_id,
        v1_0.id,
        v1_0.address,
        v1_0.city,
        v1_0.created_at,
        v1_0.description,
        v1_0.district,
        v1_0.image_url,
        v1_0.name,
        v1_0.owner_id,
        v1_0.phone,
        v1_0.status,
        v1_0.updated_at
    from
        courts c1_0
    join
        venues v1_0
            on v1_0.id=c1_0.venue_id
    join
        sports s1_0
            on s1_0.id=c1_0.sport_id
    where
        (
            ? is null
            or ?=''
            or lower(c1_0.name) like lower(concat('%', ?, '%')) escape ''
            or lower(v1_0.name) like lower(concat('%', ?, '%')) escape ''
        )
        and (
            ? is null
            or s1_0.id=?
        )
        and (
            ? is null
            or v1_0.id=?
        )
        and (
            ? is null
            or c1_0.price_per_hour>=?
        )
        and (
            ? is null
            or c1_0.price_per_hour<=?
        )
    order by
        c1_0.price_per_hour desc,
        c1_0.id
    limit
        ?, ?
