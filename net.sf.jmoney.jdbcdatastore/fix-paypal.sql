select * from NET_SF_JMONEY_ACCOUNT where "name" = 'Halifax Credit Card 9076' or "name" LIKE 'Paypal%'
select * from NET_SF_JMONEY_ACCOUNT where "name" LIKE 'Robert%'
halifax = 742
pp = 747

select * from NET_SF_JMONEY_ENTRY A where A."account" = 747 and exists (select * from NET_SF_JMONEY_ENTRY B where A."net_sf_jmoney_transaction_entry" = B."net_sf_jmoney_transaction_entry" and B."account" = 742)

select * from NET_SF_JMONEY_ENTRY A where A."account" = 742

select * from NET_SF_JMONEY_ENTRY A where A."account" = 41

update NET_SF_JMONEY_ENTRY A set "account" = 785 where A."account" = 747 and exists (select * from NET_SF_JMONEY_ENTRY B where A."net_sf_jmoney_transaction_entry" = B."net_sf_jmoney_transaction_entry" and B."account" = 742)


785


select * from NET_SF_JMONEY_ACCOUNT where "name" = 'Access'
select distinct "net_sf_jmoney_reconciliation_entryProperties_statement" from NET_SF_JMONEY_ENTRY A where A."account" = 117

update NET_SF_JMONEY_ENTRY A set "net_sf_jmoney_reconciliation_entryProperties_statement" = '1989.12.28' where "net_sf_jmoney_reconciliation_entryProperties_statement" = '8912' and A."account" = 117