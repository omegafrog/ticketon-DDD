# delete-all-then-re-insert

```java
2025-07-08T14:57:14.149+09:00 DEBUG 12272 --- [seat] [nio-9000-exec-1] org.hibernate.SQL                        : select e1_0.id,e1_0.age_limit,e1_0.booking_end,e1_0.booking_start,e1_0.event_category_id,e1_0.description,e1_0.event_end,e1_0.event_start,e1_0.restrictions,e1_0.seat_selectable,e1_0.status,e1_0.thumbnail_url,e1_0.title,e1_0.view_count,e1_0.manager_id,e1_0.created_at,e1_0.deleted,e1_0.modified_at,e1_0.seat_layout_id from event e1_0 where e1_0.id=?
2025-07-08T14:57:14.332+09:00 DEBUG 12272 --- [seat] [nio-9000-exec-1] org.hibernate.SQL                        : select sl1_0.id,sl1_0.layout from seat_layout sl1_0 where sl1_0.id=?
2025-07-08T14:57:14.380+09:00 DEBUG 12272 --- [seat] [nio-9000-exec-1] org.hibernate.SQL                        : delete from seat where layout_id=?
2025-07-08T14:57:14.387+09:00 DEBUG 12272 --- [seat] [nio-9000-exec-1] org.hibernate.SQL                        : insert into seat (layout_id,amount,grade,seat_id,signature) values (?,?,?,?,?)
2025-07-08T14:57:14.387+09:00 DEBUG 12272 --- [seat] [nio-9000-exec-1] org.hibernate.SQL                        : insert into seat (layout_id,amount,grade,seat_id,signature) values (?,?,?,?,?)
2025-07-08T14:57:14.387+09:00 DEBUG 12272 --- [seat] [nio-9000-exec-1] org.hibernate.SQL                        : insert into seat (layout_id,amount,grade,seat_id,signature) values (?,?,?,?,?)
2025-07-08T14:57:14.387+09:00 DEBUG 12272 --- [seat] [nio-9000-exec-1] org.hibernate.SQL                        : insert into seat (layout_id,amount,grade,seat_id,signature) values (?,?,?,?,?)
2025-07-08T14:57:14.387+09:00 DEBUG 12272 --- [seat] [nio-9000-exec-1] org.hibernate.SQL                        : insert into seat (layout_id,amount,grade,seat_id,signature) values (?,?,?,?,?)
```
event의 seat를 업데이트하는 과정에서 이렇게 insert query가 끊어져 나가고 있음.

