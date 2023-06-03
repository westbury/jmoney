select * from NET_SF_JMONEY_ENTRY where NET_SF_JMONEY_ENTRY."creation" > 1557169000000
select * from NET_SF_JMONEY_ENTRY where NET_SF_JMONEY_ENTRY."creation" <  1557169000000 and "creation" > 1557000000000 order by NET_SF_JMONEY_ENTRY."creation" DESC fetch first 1000 rows only

update NET_SF_JMONEY_ENTRY set "amount" = -"amount" where NET_SF_JMONEY_ENTRY."creation" <  1557169000000 and "creation" > 1557000000000

select * from NET_SF_JMONEY_ACCOUNT where NET_SF_JMONEY_ACCOUNT."_ID" = 129

select * from NET_SF_JMONEY_ENTRY where NET_SF_JMONEY_ENTRY."_ID" = 105456
